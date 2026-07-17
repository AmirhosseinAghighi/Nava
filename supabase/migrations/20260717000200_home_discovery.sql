-- Stable, authenticated catalog projections for the Home discovery experience.
create or replace view public.home_track_cards
with (security_invoker = true)
as
select
  t.id,
  t.title,
  a.name as artist_name,
  t.cover_image_url,
  t.audio_url,
  t.duration_seconds,
  t.language_code,
  t.published_at
from public.tracks t
join public.artists a on a.id = t.artist_id;

create or replace function public.get_home_section(
  p_kind public.catalog_kind,
  p_limit integer default 10
)
returns table (
  id uuid,
  title text,
  artist_name text,
  cover_image_url text,
  audio_url text,
  duration_seconds integer,
  language_code text,
  section_position smallint
)
language sql
stable
security invoker
set search_path = public
as $$
  select
    c.id,
    c.title,
    c.artist_name,
    c.cover_image_url,
    c.audio_url,
    c.duration_seconds,
    c.language_code,
    s.position as section_position
  from public.catalog_sections s
  join public.home_track_cards c on c.id = s.track_id
  where s.kind = p_kind
  order by s.position asc, c.id asc
  limit greatest(1, least(p_limit, 30));
$$;

-- The local/global/featured sections are repeatable projections over the seeded catalog.
insert into public.catalog_sections (kind, title_key, position, track_id)
select 'featured', 'section_featured', row_number() over (order by t.published_at desc) - 1, t.id
from public.tracks t
order by t.published_at desc
limit 5
on conflict (kind, position) do update set track_id = excluded.track_id, title_key = excluded.title_key;

insert into public.catalog_sections (kind, title_key, position, track_id)
select 'global', 'section_global', row_number() over (order by t.published_at desc) - 1, t.id
from public.tracks t
where t.language_code = 'en'
order by t.published_at desc
limit 10
on conflict (kind, position) do update set track_id = excluded.track_id, title_key = excluded.title_key;

insert into public.catalog_sections (kind, title_key, position, track_id)
select 'local', 'section_local', row_number() over (order by t.published_at desc) - 1, t.id
from public.tracks t
where t.language_code = 'fa'
order by t.published_at desc
limit 10
on conflict (kind, position) do update set track_id = excluded.track_id, title_key = excluded.title_key;
