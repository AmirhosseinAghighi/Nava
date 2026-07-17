-- Trigger functions are internal implementation details, never REST RPC endpoints.
revoke all on function public.handle_new_user() from public, anon, authenticated;
