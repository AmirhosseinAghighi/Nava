-- Run as the Supabase test role after the Feature 9 migration.
-- The migration's check constraints protect empty messages and receipts without delivery state.
do $$
begin
  if not exists (select 1 from pg_tables where schemaname = 'public' and tablename = 'conversations') then
    raise exception 'conversations table is missing';
  end if;
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'conversation_messages'
  ) then
    raise exception 'conversation_messages is not in the supabase_realtime publication';
  end if;
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public' and tablename = 'conversation_messages'
      and policyname = 'members read conversation messages'
  ) then
    raise exception 'message membership RLS policy is missing';
  end if;
end;
$$;
