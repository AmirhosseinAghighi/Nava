create or replace function public.set_follow(p_target_id uuid, p_follow boolean)
returns boolean
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
  if p_target_id = requesting_user then
    raise exception 'users cannot follow themselves' using errcode = '22023';
  end if;

  if p_follow then
    insert into public.user_follows (follower_id, following_id)
    values (requesting_user, p_target_id)
    on conflict do nothing;
  else
    delete from public.user_follows
    where follower_id = requesting_user and following_id = p_target_id;
  end if;
  return p_follow;
end;
$$;

revoke all on function public.set_follow(uuid, boolean) from public, anon;
grant execute on function public.set_follow(uuid, boolean) to authenticated;
