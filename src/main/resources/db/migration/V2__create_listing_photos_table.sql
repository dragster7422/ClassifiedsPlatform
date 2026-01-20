CREATE TABLE photos (
                        id UUID PRIMARY KEY,
                        listing_id UUID NOT NULL,
                        filename VARCHAR(255) NOT NULL,
                        content_type VARCHAR(50) NOT NULL,
                        file_size BIGINT NOT NULL,
                        storage_path VARCHAR(500) NOT NULL,
                        created_at TIMESTAMP NOT NULL,

                        CONSTRAINT fk_photos_listing FOREIGN KEY (listing_id)
                            REFERENCES listings(id) ON DELETE CASCADE,
                        CONSTRAINT chk_file_size_positive CHECK (file_size > 0),
                        CONSTRAINT chk_file_size_limit CHECK (file_size <= 2097152)
);

CREATE INDEX idx_photos_listing_id ON photos(listing_id);
CREATE INDEX idx_photos_created_at ON photos(created_at DESC);