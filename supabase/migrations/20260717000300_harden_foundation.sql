-- Tighten trigger-function execution and avoid public object-listing exposure.
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

revoke execute on function public.handle_new_user() from anon, authenticated;
drop policy if exists "covers are publicly readable" on storage.objects;
