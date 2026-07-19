create or replace function public.enable_demo_premium()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'authentication required'; end if;
  update public.profiles set is_premium = true where id = auth.uid();
end;
$$;
revoke all on function public.enable_demo_premium() from public, anon;
grant execute on function public.enable_demo_premium() to authenticated;
