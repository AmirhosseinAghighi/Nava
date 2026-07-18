-- User-owned library, playlists, likes, and listening history.
create table public.user_track_likes (
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, track_id)
);

create table public.listening_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  listened_at timestamptz not null default now(),
  seconds_played integer not null default 0 check (seconds_played >= 0)
);

create table public.playlists (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 120),
  description text,
  cover_image_url text,
  is_public boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.playlist_tracks (
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  position integer not null check (position >= 0),
  added_at timestamptz not null default now(),
  primary key (playlist_id, track_id),
  unique (playlist_id, position)
);

create index listening_history_user_listened_idx on public.listening_history (user_id, listened_at desc, id);
create index user_track_likes_user_created_idx on public.user_track_likes (user_id, created_at desc);
create index playlists_owner_updated_idx on public.playlists (owner_id, updated_at desc, id);

create trigger playlists_updated_at before update on public.playlists
for each row execute procedure public.touch_updated_at();

alter table public.user_track_likes enable row level security;
alter table public.listening_history enable row level security;
alter table public.playlists enable row level security;
alter table public.playlist_tracks enable row level security;

create policy "users manage their own likes" on public.user_track_likes for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "users manage their own listening history" on public.listening_history for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "owners manage private playlists" on public.playlists for all to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "owners manage playlist tracks" on public.playlist_tracks for all to authenticated
  using (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = auth.uid()))
  with check (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = auth.uid()));
