-- Profile avatars are visible to signed-in users wherever public profiles appear.
drop policy if exists "users read their own avatars" on storage.objects;

create policy "signed-in users read profile avatars" on storage.objects
  for select to authenticated
  using (bucket_id = 'avatars');
