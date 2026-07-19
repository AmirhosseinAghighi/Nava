-- Premium-only download authorization with an auditable decision for every request.
create table public.download_audit (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  status text not null check (status in ('allowed', 'denied')),
  requested_at timestamptz not null default now()
);

create index download_audit_user_requested_idx
  on public.download_audit (user_id, requested_at desc, id);

alter table public.download_audit enable row level security;

create policy "users read their download audit" on public.download_audit
  for select to authenticated using (user_id = auth.uid());

create or replace function public.authorize_track_download(p_track_id uuid)
returns table (audio_url text, expires_in_seconds integer)
language plpgsql
security definer
set search_path = public
as $$
declare
  requesting_user uuid := auth.uid();
  premium_enabled boolean;
  storage_uri text;
begin
  if requesting_user is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;

  select is_premium into premium_enabled from public.profiles where id = requesting_user;
  if coalesce(premium_enabled, false) is false then
    insert into public.download_audit (user_id, track_id, status)
    values (requesting_user, p_track_id, 'denied');
    raise exception 'premium subscription required' using errcode = '42501';
  end if;

  select audio_url into storage_uri from public.tracks where id = p_track_id;
  if storage_uri is null then
    raise exception 'track not found' using errcode = 'P0002';
  end if;

  insert into public.download_audit (user_id, track_id, status)
  values (requesting_user, p_track_id, 'allowed');

  return query select storage_uri, 600;
end;
$$;

revoke all on function public.authorize_track_download(uuid) from public;
grant execute on function public.authorize_track_download(uuid) to authenticated;
