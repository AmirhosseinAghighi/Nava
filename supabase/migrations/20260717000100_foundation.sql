-- Nava foundation: auth-owned profiles, catalog, and safe media metadata.
create extension if not exists pgcrypto;

create type public.catalog_kind as enum ('trending', 'newest', 'global', 'local', 'featured');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (char_length(display_name) between 2 and 60),
  avatar_path text,
  is_premium boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.artists (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  biography text,
  image_path text,
  country_code text,
  created_at timestamptz not null default now()
);

create table public.tracks (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 1 and 160),
  artist_id uuid not null references public.artists(id) on delete restrict,
  -- Storage URIs are intentionally versioned metadata, resolved to signed URLs at playback time.
  cover_image_url text not null check (cover_image_url like 'storage://covers/%'),
  audio_url text not null check (audio_url like 'storage://audio/%'),
  duration_seconds integer not null check (duration_seconds between 1 and 3600),
  genre text not null,
  language_code text not null default 'en',
  published_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index tracks_artist_id_idx on public.tracks(artist_id);
create index tracks_published_at_idx on public.tracks(published_at desc, id);
create index tracks_title_search_idx on public.tracks using gin (to_tsvector('simple', title));

create table public.catalog_sections (
  id uuid primary key default gen_random_uuid(),
  kind public.catalog_kind not null,
  title_key text not null,
  position smallint not null check (position >= 0),
  track_id uuid not null references public.tracks(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (kind, position)
);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), nullif(split_part(coalesce(new.email, ''), '@', 1), ''), new.id::text));
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_updated_at before update on public.profiles
  for each row execute procedure public.touch_updated_at();

alter table public.profiles enable row level security;
alter table public.artists enable row level security;
alter table public.tracks enable row level security;
alter table public.catalog_sections enable row level security;

create policy "profiles are visible to signed-in users" on public.profiles
  for select to authenticated using (true);
create policy "users update only their own profile" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());
create policy "signed-in users read artists" on public.artists
  for select to authenticated using (true);
create policy "signed-in users read tracks" on public.tracks
  for select to authenticated using (true);
create policy "signed-in users read catalog sections" on public.catalog_sections
  for select to authenticated using (true);

-- Clients may edit their public identity but can never self-upgrade Premium status.
revoke update on public.profiles from authenticated;
grant update (display_name, avatar_path) on public.profiles to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('covers', 'covers', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
  ('audio', 'audio', false, 52428800, array['audio/mpeg', 'audio/ogg', 'audio/mp4'])
on conflict (id) do update set public = excluded.public, file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

create policy "covers are publicly readable" on storage.objects
  for select using (bucket_id = 'covers');
create policy "authenticated users read audio through signed URLs" on storage.objects
  for select to authenticated using (bucket_id = 'audio');
