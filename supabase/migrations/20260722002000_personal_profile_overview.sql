-- Return the signed-in listener's complete profile header and social counters.
create or replace function public.get_my_profile_overview()
returns table (
  display_name text,
  avatar_path text,
  is_premium boolean,
  followers_count bigint,
  following_count bigint,
  public_playlists_count bigint
)
language sql
security definer
set search_path = public
as $$
  select
    p.display_name,
    p.avatar_path,
    p.is_premium,
    (select count(*) from public.user_follows f where f.following_id = p.id),
    (select count(*) from public.user_follows f where f.follower_id = p.id),
    (select count(*) from public.playlists pl where pl.owner_id = p.id and pl.is_public)
  from public.profiles p
  where p.id = auth.uid();
$$;

revoke all on function public.get_my_profile_overview() from public, anon;
grant execute on function public.get_my_profile_overview() to authenticated;
