ALTER TABLE public.thrift_store
    ADD COLUMN IF NOT EXISTS whatsapp character varying(2048);
