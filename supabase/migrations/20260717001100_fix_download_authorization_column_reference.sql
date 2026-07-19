-- Qualify the source column: audio_url is also a RETURNS TABLE output name.
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

  select p.is_premium into premium_enabled
  from public.profiles as p
  where p.id = requesting_user;

  if coalesce(premium_enabled, false) is false then
    insert into public.download_audit (user_id, track_id, status)
    values (requesting_user, p_track_id, 'denied');
    raise exception 'premium subscription required' using errcode = '42501';
  end if;

  select t.audio_url into storage_uri
  from public.tracks as t
  where t.id = p_track_id;

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
