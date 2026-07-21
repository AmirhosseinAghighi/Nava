-- Allows authenticated chat members to resolve a shared catalog track for local playback.
create or replace function public.get_shareable_track(p_track_id uuid)
returns table (
  id uuid,
  title text,
  artist_name text,
  cover_image_url text,
  audio_url text,
  language_code text
)
language sql
security invoker
set search_path = public
as $$
  select t.id, t.title, a.name, t.cover_image_url, t.audio_url, t.language_code
  from public.tracks t
  join public.artists a on a.id = t.artist_id
  where t.id = p_track_id;
$$;

revoke all on function public.get_shareable_track(uuid) from public, anon;
grant execute on function public.get_shareable_track(uuid) to authenticated;
