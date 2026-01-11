CREATE TABLE IF NOT EXISTS public.asset_deletion_job (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(1024),
    source_type VARCHAR(64),
    source_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_asset_deletion_job_status_next
    ON public.asset_deletion_job (status, next_attempt_at, id);
