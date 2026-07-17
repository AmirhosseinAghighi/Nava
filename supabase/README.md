# Supabase setup

1. Link this directory to a Supabase project and run `supabase db reset` (or apply the migration then seed).
2. Upload the 50 supplied cover files as `covers/track-<1..50>.webp` and audio files as `audio/track-<1..50>.mp3`.
3. Run `supabase/tests/catalog_seed.sql` to verify catalog size and unique media paths.

The seed deliberately contains storage URIs rather than project-specific URLs. The client or a future Edge Function resolves cover objects and creates authenticated, short-lived audio URLs; this avoids committing a project reference or a permanent private-media URL.
