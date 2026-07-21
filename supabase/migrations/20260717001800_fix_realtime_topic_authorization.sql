-- The Realtime client transports topics with a `realtime:` prefix; support either form safely.
drop policy if exists "members receive conversation realtime presence" on realtime.messages;
drop policy if exists "members send conversation realtime presence" on realtime.messages;

create policy "members receive conversation realtime presence" on realtime.messages
  for select to authenticated using (
    realtime.topic() like '%conversation:%'
    and exists (
      select 1 from public.conversation_members cm
      where cm.user_id = auth.uid()
        and cm.conversation_id::text = regexp_replace(realtime.topic(), '^.*conversation:', '')
    )
  );
create policy "members send conversation realtime presence" on realtime.messages
  for insert to authenticated with check (
    realtime.topic() like '%conversation:%'
    and exists (
      select 1 from public.conversation_members cm
      where cm.user_id = auth.uid()
        and cm.conversation_id::text = regexp_replace(realtime.topic(), '^.*conversation:', '')
    )
  );
