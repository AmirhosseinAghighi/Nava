-- Supabase grants EXECUTE to anon by default; downloads must require authentication.
revoke execute on function public.authorize_track_download(uuid) from anon;
