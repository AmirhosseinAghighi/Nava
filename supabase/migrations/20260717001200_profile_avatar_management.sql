-- Private per-user avatars and server-validated profile updates.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', false, 2097152, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy "users read their own avatars" on storage.objects
  for select to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "users upload their own avatars" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "users update their own avatars" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "users delete their own avatars" on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create or replace function public.get_my_profile()
returns table (display_name text, avatar_path text, is_premium boolean)
language sql
security definer
set search_path = public
as $$
  select p.display_name, p.avatar_path, p.is_premium
  from public.profiles as p
  where p.id = auth.uid();
$$;

create or replace function public.update_my_profile(p_display_name text, p_avatar_path text)
returns table (display_name text, avatar_path text, is_premium boolean)
language plpgsql
security definer
set search_path = public
as $$
declare
  requesting_user uuid := auth.uid();
begin
  if requesting_user is null then
    raise exception 'authentication required' using errcode = '28000';
  end if;

  if char_length(trim(coalesce(p_display_name, ''))) not between 2 and 60 then
    raise exception 'display name must contain 2 to 60 characters' using errcode = '22023';
  end if;

  if p_avatar_path is not null
    and p_avatar_path <> ''
    and p_avatar_path !~ ('^storage://avatars/' || requesting_user::text || '/[^/]+$') then
    raise exception 'avatar path must belong to the current user' using errcode = '42501';
  end if;

  update public.profiles as p
  set display_name = trim(p_display_name),
      avatar_path = nullif(p_avatar_path, '')
  where p.id = requesting_user;

  return query
  select p.display_name, p.avatar_path, p.is_premium
  from public.profiles as p
  where p.id = requesting_user;
end;
$$;

revoke update on public.profiles from authenticated;
revoke all on function public.get_my_profile() from public, anon;
revoke all on function public.update_my_profile(text, text) from public, anon;
grant execute on function public.get_my_profile() to authenticated;
grant execute on function public.update_my_profile(text, text) to authenticated;
