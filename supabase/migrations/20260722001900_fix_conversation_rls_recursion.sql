-- Avoid recursive RLS evaluation when a conversation member checks the membership list.
create or replace function public.is_conversation_member(p_conversation_id uuid)
returns boolean
language sql
security definer
set search_path = public
as $$
  select auth.uid() is not null
    and exists (
      select 1
      from public.conversation_members
      where conversation_id = p_conversation_id
        and user_id = auth.uid()
    );
$$;

revoke all on function public.is_conversation_member(uuid) from public, anon;
grant execute on function public.is_conversation_member(uuid) to authenticated;

drop policy if exists "members read their conversations" on public.conversations;
drop policy if exists "members read conversation membership" on public.conversation_members;
drop policy if exists "members read conversation messages" on public.conversation_messages;

create policy "members read their conversations" on public.conversations
  for select to authenticated using (public.is_conversation_member(id));

create policy "members read conversation membership" on public.conversation_members
  for select to authenticated using (public.is_conversation_member(conversation_id));

create policy "members read conversation messages" on public.conversation_messages
  for select to authenticated using (public.is_conversation_member(conversation_id));
