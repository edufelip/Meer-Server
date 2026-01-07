CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_guide_content_comment_body_trgm
    ON public.guide_content_comment USING gin (lower(body) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_guide_content_title_trgm
    ON public.guide_content USING gin (lower(title) gin_trgm_ops);

DROP INDEX IF EXISTS idx_guide_content_comment_deleted_at;
ALTER TABLE public.guide_content_comment
    DROP CONSTRAINT IF EXISTS fk_guide_content_comment_deleted_by;
ALTER TABLE public.guide_content_comment
    DROP COLUMN IF EXISTS deleted_at,
    DROP COLUMN IF EXISTS deleted_by_user_id,
    DROP COLUMN IF EXISTS deleted_reason;
