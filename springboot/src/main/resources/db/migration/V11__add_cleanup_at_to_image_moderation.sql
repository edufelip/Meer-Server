-- Track when blocked/rejected images have been cleaned up
ALTER TABLE image_moderation
    ADD COLUMN cleanup_at TIMESTAMP;

CREATE INDEX idx_image_moderation_cleanup_at ON image_moderation(cleanup_at);

COMMENT ON COLUMN image_moderation.cleanup_at IS 'Timestamp when blocked or rejected image cleanup completed';
