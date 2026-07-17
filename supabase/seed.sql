-- Repeatable catalog metadata seed. Upload matching files to Storage before using playback.
insert into public.artists (name, biography, country_code) values
  ('Ava North', 'Atmospheric electronic artist.', 'GB'),
  ('Kian Darya', 'Independent Persian instrumental composer.', 'IR'),
  ('Mira Sol', 'Warm acoustic songwriter.', 'ES'),
  ('The Meridian', 'Late-night jazz collective.', 'US'),
  ('Niloofar', 'Persian ambient producer.', 'IR')
on conflict (name) do nothing;

with catalog(title, artist_name, genre, language_code, duration_seconds, track_no) as (
  values
    ('First Light','Ava North','Electronic','en',214,1), ('Blue Hour','Ava North','Electronic','en',198,2),
    ('Glass Tides','Ava North','Electronic','en',231,3), ('Signal Bloom','Ava North','Electronic','en',205,4),
    ('Afterimage','Ava North','Electronic','en',222,5), ('Tehran Dawn','Kian Darya','Instrumental','fa',244,6),
    ('Cedar Wind','Kian Darya','Instrumental','fa',227,7), ('Silver Setar','Kian Darya','Instrumental','fa',236,8),
    ('Quiet Bazaar','Kian Darya','Instrumental','fa',219,9), ('Rain over Alborz','Kian Darya','Instrumental','fa',251,10),
    ('Paper Sun','Mira Sol','Acoustic','en',191,11), ('Open Window','Mira Sol','Acoustic','en',203,12),
    ('Small Maps','Mira Sol','Acoustic','en',216,13), ('Golden Thread','Mira Sol','Acoustic','en',188,14),
    ('Northbound','Mira Sol','Acoustic','en',209,15), ('Velvet Steps','The Meridian','Jazz','en',278,16),
    ('Midnight Receipt','The Meridian','Jazz','en',265,17), ('Cobalt Room','The Meridian','Jazz','en',249,18),
    ('Last Train Home','The Meridian','Jazz','en',282,19), ('Soft Focus','The Meridian','Jazz','en',258,20),
    ('Shiraz Sky','Niloofar','Ambient','fa',260,21), ('Pomegranate','Niloofar','Ambient','fa',247,22),
    ('Saffron Mist','Niloofar','Ambient','fa',273,23), ('Garden Wall','Niloofar','Ambient','fa',239,24),
    ('Moonlit Tile','Niloofar','Ambient','fa',255,25), ('Neon Current','Ava North','Electronic','en',225,26),
    ('Slow Satellite','Ava North','Electronic','en',217,27), ('Bloom State','Ava North','Electronic','en',208,28),
    ('Night Circuit','Ava North','Electronic','en',234,29), ('Horizon Code','Ava North','Electronic','en',221,30),
    ('Stone and Stream','Kian Darya','Instrumental','fa',242,31), ('Amber Road','Kian Darya','Instrumental','fa',229,32),
    ('Rooftop Tea','Kian Darya','Instrumental','fa',217,33), ('Morning Caravan','Kian Darya','Instrumental','fa',253,34),
    ('Woven Rain','Kian Darya','Instrumental','fa',238,35), ('Lanterns','Mira Sol','Acoustic','en',196,36),
    ('Soft Landing','Mira Sol','Acoustic','en',207,37), ('Wildflower Note','Mira Sol','Acoustic','en',213,38),
    ('Close to Home','Mira Sol','Acoustic','en',202,39), ('August Postcard','Mira Sol','Acoustic','en',218,40),
    ('Brass and Birch','The Meridian','Jazz','en',271,41), ('Window Seat','The Meridian','Jazz','en',257,42),
    ('Low Lantern','The Meridian','Jazz','en',284,43), ('City in Sepia','The Meridian','Jazz','en',263,44),
    ('Sunday Vinyl','The Meridian','Jazz','en',276,45), ('Distant Cypress','Niloofar','Ambient','fa',248,46),
    ('Blue Courtyard','Niloofar','Ambient','fa',262,47), ('Wandering Light','Niloofar','Ambient','fa',241,48),
    ('Salt Garden','Niloofar','Ambient','fa',256,49), ('Night Jasmine','Niloofar','Ambient','fa',269,50)
)
insert into public.tracks (title, artist_id, cover_image_url, audio_url, duration_seconds, genre, language_code, published_at)
select c.title, a.id, 'storage://covers/track-' || c.track_no || '.webp', 'storage://audio/track-' || c.track_no || '.mp3',
       c.duration_seconds, c.genre, c.language_code, now() - (51 - c.track_no) * interval '1 day'
from catalog c join public.artists a on a.name = c.artist_name
where not exists (select 1 from public.tracks t where t.title = c.title and t.artist_id = a.id);

insert into public.catalog_sections (kind, title_key, position, track_id)
select 'trending', 'section_trending', row_number() over (order by t.published_at desc) - 1, t.id
from public.tracks t order by t.published_at desc limit 10
on conflict (kind, position) do update set track_id = excluded.track_id, title_key = excluded.title_key;

insert into public.catalog_sections (kind, title_key, position, track_id)
select 'newest', 'section_newest', row_number() over (order by t.published_at desc) - 1, t.id
from public.tracks t order by t.published_at desc limit 10
on conflict (kind, position) do update set track_id = excluded.track_id, title_key = excluded.title_key;
