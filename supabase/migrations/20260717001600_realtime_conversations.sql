-- Feature 9: member-scoped direct conversations, messages, receipts, and Realtime typing.
create table public.conversations (
  id uuid primary key default gen_random_uuid(),
  created_by uuid not null references auth.users(id) on delete cascade,
  direct_key text unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.conversation_members (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  last_read_at timestamptz,
  primary key (conversation_id, user_id)
);

create table public.conversation_messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  body text,
  track_id uuid references public.tracks(id) on delete set null,
  created_at timestamptz not null default now(),
  check (char_length(coalesce(body, '')) <= 2000),
  check (nullif(trim(coalesce(body, '')), '') is not null or track_id is not null)
);

create table public.message_receipts (
  message_id uuid not null references public.conversation_messages(id) on delete cascade,
  recipient_id uuid not null references auth.users(id) on delete cascade,
  delivered_at timestamptz,
  read_at timestamptz,
  primary key (message_id, recipient_id),
  check (read_at is null or delivered_at is not null)
);

create index conversation_members_user_joined_idx
  on public.conversation_members (user_id, joined_at desc, conversation_id);
create index conversation_messages_conversation_created_idx
  on public.conversation_messages (conversation_id, created_at desc, id desc);
create index message_receipts_recipient_message_idx
  on public.message_receipts (recipient_id, message_id);

create trigger conversations_updated_at before update on public.conversations
for each row execute procedure public.touch_updated_at();

create or replace function public.touch_conversation_on_message()
returns trigger
language plpgsql
security invoker
set search_path = public
as $$
begin
  update public.conversations set updated_at = new.created_at where id = new.conversation_id;
  return new;
end;
$$;

create trigger conversation_message_updates_conversation
after insert on public.conversation_messages
for each row execute procedure public.touch_conversation_on_message();

alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.conversation_messages enable row level security;
alter table public.message_receipts enable row level security;

create policy "members read their conversations" on public.conversations
  for select to authenticated using (
    exists (
      select 1 from public.conversation_members cm
      where cm.conversation_id = id and cm.user_id = auth.uid()
    )
  );

create policy "members read conversation membership" on public.conversation_members
  for select to authenticated using (
    exists (
      select 1 from public.conversation_members own
      where own.conversation_id = conversation_id and own.user_id = auth.uid()
    )
  );

create policy "members read conversation messages" on public.conversation_messages
  for select to authenticated using (
    exists (
      select 1 from public.conversation_members cm
      where cm.conversation_id = conversation_id and cm.user_id = auth.uid()
    )
  );

create policy "recipients and senders read message receipts" on public.message_receipts
  for select to authenticated using (
    recipient_id = auth.uid()
    or exists (
      select 1 from public.conversation_messages m
      where m.id = message_id and m.sender_id = auth.uid()
    )
  );

create policy "recipients update their own receipts" on public.message_receipts
  for update to authenticated using (recipient_id = auth.uid())
  with check (recipient_id = auth.uid());

create or replace function public.get_or_create_direct_conversation(p_other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  requesting_user uuid := auth.uid();
  conversation_id uuid;
  key text;
begin
  if requesting_user is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;
  if p_other_user_id is null or p_other_user_id = requesting_user then
    raise exception 'choose another user' using errcode = '22023';
  end if;
  if not exists (select 1 from public.profiles where id = p_other_user_id) then
    raise exception 'user not found' using errcode = '22023';
  end if;

  key := least(requesting_user::text, p_other_user_id::text) || ':' || greatest(requesting_user::text, p_other_user_id::text);
  insert into public.conversations (created_by, direct_key)
  values (requesting_user, key)
  on conflict (direct_key) do update set updated_at = public.conversations.updated_at
  returning id into conversation_id;

  insert into public.conversation_members (conversation_id, user_id)
  values (conversation_id, requesting_user), (conversation_id, p_other_user_id)
  on conflict do nothing;
  return conversation_id;
end;
$$;

create or replace function public.list_conversations(p_limit integer default 30, p_before timestamptz default null)
returns table (
  id uuid,
  peer_id uuid,
  peer_name text,
  peer_avatar_path text,
  last_message_id uuid,
  last_message_body text,
  last_message_track_id uuid,
  last_message_at timestamptz,
  unread_count bigint
)
language sql
security invoker
set search_path = public
as $$
  select c.id,
         peer.user_id as peer_id,
         p.display_name as peer_name,
         p.avatar_path as peer_avatar_path,
         latest.id as last_message_id,
         latest.body as last_message_body,
         latest.track_id as last_message_track_id,
         latest.created_at as last_message_at,
         coalesce(unread.count, 0) as unread_count
  from public.conversation_members mine
  join public.conversations c on c.id = mine.conversation_id
  join lateral (
    select other.user_id
    from public.conversation_members other
    where other.conversation_id = c.id and other.user_id <> auth.uid()
    order by other.joined_at asc
    limit 1
  ) peer on true
  join public.profiles p on p.id = peer.user_id
  left join lateral (
    select m.id, m.body, m.track_id, m.created_at
    from public.conversation_messages m
    where m.conversation_id = c.id
    order by m.created_at desc, m.id desc
    limit 1
  ) latest on true
  left join lateral (
    select count(*) as count
    from public.message_receipts r
    join public.conversation_messages m on m.id = r.message_id
    where m.conversation_id = c.id
      and r.recipient_id = auth.uid()
      and r.read_at is null
  ) unread on true
  where mine.user_id = auth.uid()
    and (p_before is null or coalesce(latest.created_at, c.updated_at) < p_before)
  order by coalesce(latest.created_at, c.updated_at) desc, c.id desc
  limit greatest(1, least(p_limit, 50));
$$;

create or replace function public.get_conversation_messages(
  p_conversation_id uuid,
  p_limit integer default 50,
  p_before timestamptz default null
)
returns table (
  id uuid,
  conversation_id uuid,
  sender_id uuid,
  sender_name text,
  body text,
  track_id uuid,
  track_title text,
  track_artist_name text,
  created_at timestamptz,
  delivered_at timestamptz,
  read_at timestamptz
)
language sql
security invoker
set search_path = public
as $$
  select m.id, m.conversation_id, m.sender_id, p.display_name, m.body,
         m.track_id, t.title, a.name, m.created_at,
         receipts.delivered_at, receipts.read_at
  from public.conversation_messages m
  join public.profiles p on p.id = m.sender_id
  left join public.tracks t on t.id = m.track_id
  left join public.artists a on a.id = t.artist_id
  left join lateral (
    select max(r.delivered_at) as delivered_at, max(r.read_at) as read_at
    from public.message_receipts r
    where r.message_id = m.id
  ) receipts on true
  where m.conversation_id = p_conversation_id
    and (p_before is null or m.created_at < p_before)
  order by m.created_at desc, m.id desc
  limit greatest(1, least(p_limit, 100));
$$;

create or replace function public.send_conversation_message(
  p_conversation_id uuid,
  p_body text default null,
  p_track_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  requesting_user uuid := auth.uid();
  message_id uuid;
  sanitized_body text := nullif(trim(coalesce(p_body, '')), '');
begin
  if requesting_user is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;
  if not exists (
    select 1 from public.conversation_members
    where conversation_id = p_conversation_id and user_id = requesting_user
  ) then
    raise exception 'not a conversation member' using errcode = '42501';
  end if;
  if sanitized_body is null and p_track_id is null then
    raise exception 'message cannot be empty' using errcode = '22023';
  end if;
  if char_length(coalesce(sanitized_body, '')) > 2000 then
    raise exception 'message is too long' using errcode = '22001';
  end if;
  if p_track_id is not null and not exists (select 1 from public.tracks where id = p_track_id) then
    raise exception 'track not found' using errcode = '22023';
  end if;

  insert into public.conversation_messages (conversation_id, sender_id, body, track_id)
  values (p_conversation_id, requesting_user, sanitized_body, p_track_id)
  returning id into message_id;

  insert into public.message_receipts (message_id, recipient_id)
  select message_id, user_id
  from public.conversation_members
  where conversation_id = p_conversation_id and user_id <> requesting_user;
  return message_id;
end;
$$;

create or replace function public.mark_conversation_delivered(p_conversation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;
  update public.message_receipts r
  set delivered_at = coalesce(r.delivered_at, now())
  from public.conversation_messages m
  where m.id = r.message_id
    and m.conversation_id = p_conversation_id
    and r.recipient_id = auth.uid();
end;
$$;

create or replace function public.mark_conversation_read(p_conversation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;
  update public.message_receipts r
  set delivered_at = coalesce(r.delivered_at, now()),
      read_at = coalesce(r.read_at, now())
  from public.conversation_messages m
  where m.id = r.message_id
    and m.conversation_id = p_conversation_id
    and r.recipient_id = auth.uid();
  update public.conversation_members
  set last_read_at = now()
  where conversation_id = p_conversation_id and user_id = auth.uid();
end;
$$;

revoke all on function public.get_or_create_direct_conversation(uuid) from public, anon;
revoke all on function public.list_conversations(integer, timestamptz) from public, anon;
revoke all on function public.get_conversation_messages(uuid, integer, timestamptz) from public, anon;
revoke all on function public.send_conversation_message(uuid, text, uuid) from public, anon;
revoke all on function public.mark_conversation_delivered(uuid) from public, anon;
revoke all on function public.mark_conversation_read(uuid) from public, anon;
grant execute on function public.get_or_create_direct_conversation(uuid) to authenticated;
grant execute on function public.list_conversations(integer, timestamptz) to authenticated;
grant execute on function public.get_conversation_messages(uuid, integer, timestamptz) to authenticated;
grant execute on function public.send_conversation_message(uuid, text, uuid) to authenticated;
grant execute on function public.mark_conversation_delivered(uuid) to authenticated;
grant execute on function public.mark_conversation_read(uuid) to authenticated;

alter publication supabase_realtime add table public.conversation_messages;

-- Broadcast/Presence channels use `conversation:<uuid>` so typing status is visible only to members.
create policy "members receive conversation realtime presence" on realtime.messages
  for select to authenticated using (
    realtime.topic() like 'conversation:%'
    and exists (
      select 1 from public.conversation_members cm
      where cm.user_id = auth.uid()
        and cm.conversation_id::text = substring(realtime.topic() from 'conversation:(.*)')
    )
  );
create policy "members send conversation realtime presence" on realtime.messages
  for insert to authenticated with check (
    realtime.topic() like 'conversation:%'
    and exists (
      select 1 from public.conversation_members cm
      where cm.user_id = auth.uid()
        and cm.conversation_id::text = substring(realtime.topic() from 'conversation:(.*)')
    )
  );
