DELETE FROM public.guide_content_comment
WHERE deleted_at IS NOT NULL;
