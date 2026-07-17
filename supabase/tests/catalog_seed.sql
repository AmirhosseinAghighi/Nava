-- Run after `supabase db reset`; zero rows or duplicate storage locations fail the check.
do $$
declare
  track_count integer;
  duplicate_media_count integer;
begin
  select count(*) into track_count from public.tracks;
  if track_count < 50 then
    raise exception 'Expected at least 50 tracks, got %', track_count;
  end if;
  select count(*) - count(distinct audio_url) into duplicate_media_count from public.tracks;
  if duplicate_media_count <> 0 then
    raise exception 'Catalog has duplicate audio storage URIs';
  end if;
end;
$$;
