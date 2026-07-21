-- Release readiness checks. Run after migrations and seed/import.
do $$
declare
  track_count integer;
  missing_media_count integer;
  duplicate_audio_count integer;
  playback_insert_policy_count integer;
begin
  select count(*) into track_count from public.tracks;
  if track_count < 50 then
    raise exception 'Expected at least 50 tracks, got %', track_count;
  end if;

  select count(*) into missing_media_count
  from public.tracks
  where nullif(audio_url, '') is null
     or nullif(cover_image_url, '') is null;
  if missing_media_count <> 0 then
    raise exception 'Expected every track to have audio and cover paths, got % incomplete rows', missing_media_count;
  end if;

  select count(*) - count(distinct audio_url) into duplicate_audio_count from public.tracks;
  if duplicate_audio_count <> 0 then
    raise exception 'Catalog has % duplicate audio storage paths', duplicate_audio_count;
  end if;

  select count(*) into playback_insert_policy_count
  from pg_policies
  where schemaname = 'public'
    and tablename = 'playback_events'
    and cmd = 'INSERT'
    and roles::text like '%authenticated%';
  if playback_insert_policy_count = 0 then
    raise exception 'playback_events is missing an authenticated INSERT policy';
  end if;
end;
$$;
