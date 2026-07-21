-- Social discovery, follows, and public-playlist access.
create table public.user_follows (
  follower_id uuid not null references auth.users(id) on delete cascade,
  following_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  check (follower_id <> following_id)
);

create index user_follows_following_created_idx
  on public.user_follows (following_id, created_at desc, follower_id);

alter table public.user_follows enable row level security;

create policy "signed-in users read public follow graph" on public.user_follows
  for select to authenticated using (true);
create policy "users create their own follows" on public.user_follows
  for insert to authenticated with check (follower_id = auth.uid());
create policy "users delete their own follows" on public.user_follows
  for delete to authenticated using (follower_id = auth.uid());

create policy "signed-in users read public playlists" on public.playlists
  for select to authenticated using (is_public or owner_id = auth.uid());
create policy "signed-in users read tracks from readable playlists" on public.playlist_tracks
  for select to authenticated using (
    exists (
      select 1 from public.playlists p
      where p.id = playlist_id and (p.is_public or p.owner_id = auth.uid())
    )
  );

create or replace function public.search_people(p_query text, p_limit integer default 20, p_after_name text default null)
returns table (id uuid, display_name text, avatar_path text, is_following boolean)
language sql
security definer
set search_path = public
as $$
  select p.id, p.display_name, p.avatar_path,
         exists (select 1 from public.user_follows f where f.follower_id = auth.uid() and f.following_id = p.id)
  from public.profiles p
  where p.id <> auth.uid()
    and p.display_name ilike ('%' || trim(coalesce(p_query, '')) || '%')
    and (p_after_name is null or p.display_name > p_after_name)
  order by p.display_name asc, p.id asc
  limit greatest(1, least(p_limit, 50));
$$;

create or replace function public.get_social_connections(p_user_id uuid, p_kind text, p_limit integer default 20)
returns table (id uuid, display_name text, avatar_path text, connected_at timestamptz, is_following boolean)
language sql
security definer
set search_path = public
as $$
  select p.id, p.display_name, p.avatar_path, f.created_at,
         exists (select 1 from public.user_follows own where own.follower_id = auth.uid() and own.following_id = p.id)
  from public.user_follows f
  join public.profiles p on p.id = case when p_kind = 'followers' then f.follower_id else f.following_id end
  where (p_kind = 'followers' and f.following_id = p_user_id)
     or (p_kind = 'following' and f.follower_id = p_user_id)
  order by f.created_at desc, p.id asc
  limit greatest(1, least(p_limit, 50));
$$;

revoke all on function public.search_people(text, integer, text) from public, anon;
revoke all on function public.get_social_connections(uuid, text, integer) from public, anon;
grant execute on function public.search_people(text, integer, text) to authenticated;
grant execute on function public.get_social_connections(uuid, text, integer) to authenticated;
