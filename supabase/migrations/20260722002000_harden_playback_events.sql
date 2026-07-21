-- Feature 10: allow authenticated clients to record their own playback events.
-- The RPC runs as the caller, so the table needs an explicit INSERT policy.
drop policy if exists "users insert their playback events" on public.playback_events;
create policy "users insert their playback events" on public.playback_events
  for insert to authenticated
  with check (user_id = auth.uid());

revoke all on function public.record_playback_event(uuid, text, integer) from public, anon;
grant execute on function public.record_playback_event(uuid, text, integer) to authenticated;
