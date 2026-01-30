ALTER TABLE public.auth_user
    ADD COLUMN terms_version character varying(64);

ALTER TABLE public.auth_user
    ADD COLUMN terms_accepted_at timestamp(6) with time zone;
