-- Authenticated playback metadata and server-validated listening-event recording.
create table public.playback_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  event_type text not null check (event_type in ('started', 'progress', 'completed', 'skipped')),
  position_seconds integer not null default 0 check (position_seconds >= 0),
  created_at timestamptz not null default now()
);

create index playback_events_user_created_idx on public.playback_events (user_id, created_at desc, id);

alter table public.playback_events enable row level security;

create policy "users read their playback events" on public.playback_events
  for select to authenticated using (user_id = auth.uid());

create or replace function public.record_playback_event(
  p_track_id uuid,
  p_event_type text,
  p_position_seconds integer default 0
)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'authentication required';
  end if;
  if p_event_type not in ('started', 'progress', 'completed', 'skipped') then
    raise exception 'invalid playback event';
  end if;
  insert into public.playback_events (user_id, track_id, event_type, position_seconds)
  values (auth.uid(), p_track_id, p_event_type, greatest(p_position_seconds, 0));
end;
$$;

revoke all on function public.record_playback_event(uuid, text, integer) from public;
grant execute on function public.record_playback_event(uuid, text, integer) to authenticated;
