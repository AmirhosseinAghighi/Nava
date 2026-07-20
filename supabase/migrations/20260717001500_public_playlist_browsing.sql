create or replace function public.get_public_playlists(p_owner_id uuid, p_limit integer default 20)
returns table (id uuid, title text, description text, owner_name text, track_count bigint)
language sql
security definer
set search_path = public
as $$
  select pl.id, pl.title, pl.description, p.display_name,
         count(pt.track_id) as track_count
  from public.playlists pl
  join public.profiles p on p.id = pl.owner_id
  left join public.playlist_tracks pt on pt.playlist_id = pl.id
  where pl.owner_id = p_owner_id
    and (pl.is_public or pl.owner_id = auth.uid())
  group by pl.id, p.display_name
  order by pl.updated_at desc, pl.id asc
  limit greatest(1, least(p_limit, 50));
$$;

revoke all on function public.get_public_playlists(uuid, integer) from public, anon;
grant execute on function public.get_public_playlists(uuid, integer) to authenticated;
