-- Create image_moderation table for tracking NSFW content detection
CREATE TABLE image_moderation (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(2048) NOT NULL,
    status VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    nsfw_score DOUBLE PRECISION,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(255),
    review_notes TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- Indexes for efficient querying
CREATE INDEX idx_image_moderation_status ON image_moderation(status);
CREATE INDEX idx_image_moderation_image_url ON image_moderation(image_url);
CREATE INDEX idx_image_moderation_entity ON image_moderation(entity_type, entity_id);
CREATE INDEX idx_image_moderation_created_at ON image_moderation(created_at);
CREATE INDEX idx_image_moderation_processed_at ON image_moderation(processed_at) WHERE processed_at IS NOT NULL;

-- Add comment for documentation
COMMENT ON TABLE image_moderation IS 'Tracks AI-based moderation status and results for images (NSFW detection)';
COMMENT ON COLUMN image_moderation.status IS 'PENDING, PROCESSING, APPROVED, FLAGGED_FOR_REVIEW, BLOCKED, MANUALLY_APPROVED, MANUALLY_REJECTED, FAILED';
COMMENT ON COLUMN image_moderation.entity_type IS 'STORE_PHOTO, USER_AVATAR, GUIDE_CONTENT_IMAGE';
COMMENT ON COLUMN image_moderation.nsfw_score IS 'Probability score between 0.0 (safe) and 1.0 (NSFW)';
