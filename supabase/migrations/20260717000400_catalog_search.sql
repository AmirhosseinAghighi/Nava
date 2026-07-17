-- Paged, stable catalog search for authenticated clients.
create index if not exists artists_name_search_idx
  on public.artists using gin (to_tsvector('simple', name));

create index if not exists tracks_catalog_filter_idx
  on public.tracks (language_code, genre, published_at desc, id);

create or replace function public.search_catalog(
  p_query text,
  p_language_code text default null,
  p_genre text default null,
  p_limit integer default 20,
  p_offset integer default 0
)
returns table (
  id uuid,
  title text,
  artist_name text,
  cover_image_url text,
  audio_url text,
  duration_seconds integer,
  genre text,
  language_code text,
  published_at timestamptz,
  total_count bigint
)
language sql
stable
security invoker
set search_path = public
as $$
  with parameters as (
    select nullif(trim(p_query), '') as query_text
  ), matches as (
    select
      t.id,
      t.title,
      a.name as artist_name,
      t.cover_image_url,
      t.audio_url,
      t.duration_seconds,
      t.genre,
      t.language_code,
      t.published_at,
      case when parameters.query_text is null then 0::real else
        greatest(
          ts_rank_cd(to_tsvector('simple', t.title), plainto_tsquery('simple', parameters.query_text)),
          ts_rank_cd(to_tsvector('simple', a.name), plainto_tsquery('simple', parameters.query_text))
        )
      end as relevance
    from public.tracks t
    join public.artists a on a.id = t.artist_id
    cross join parameters
    where
      (parameters.query_text is null
        or to_tsvector('simple', t.title) @@ plainto_tsquery('simple', parameters.query_text)
        or to_tsvector('simple', a.name) @@ plainto_tsquery('simple', parameters.query_text)
        or t.title ilike '%' || parameters.query_text || '%'
        or a.name ilike '%' || parameters.query_text || '%')
      and (p_language_code is null or t.language_code = p_language_code)
      and (p_genre is null or t.genre = p_genre)
  )
  select
    id,
    title,
    artist_name,
    cover_image_url,
    audio_url,
    duration_seconds,
    genre,
    language_code,
    published_at,
    count(*) over () as total_count
  from matches
  order by relevance desc, published_at desc, title asc, id asc
  limit greatest(1, least(p_limit, 50))
  offset greatest(0, p_offset);
$$;

revoke all on function public.search_catalog(text, text, text, integer, integer) from public;
grant execute on function public.search_catalog(text, text, text, integer, integer) to authenticated;
