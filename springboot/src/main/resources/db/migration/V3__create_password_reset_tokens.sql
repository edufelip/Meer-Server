-- Store password reset tokens with expiration.
CREATE TABLE public.password_reset_token (
    token uuid NOT NULL,
    auth_user_id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    CONSTRAINT password_reset_token_pkey PRIMARY KEY (token)
);

CREATE INDEX password_reset_token_auth_user_id_idx
    ON public.password_reset_token (auth_user_id);

CREATE INDEX password_reset_token_expires_at_idx
    ON public.password_reset_token (expires_at);

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT password_reset_token_auth_user_id_fkey
    FOREIGN KEY (auth_user_id) REFERENCES public.auth_user(id);
