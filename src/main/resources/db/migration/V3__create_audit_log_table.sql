CREATE TABLE audit_log (
                           id UUID PRIMARY KEY,
                           event_type VARCHAR(100) NOT NULL,
                           listing_id UUID NOT NULL,
                           payload_json TEXT NOT NULL,
                           created_at TIMESTAMP NOT NULL,

                           CONSTRAINT fk_audit_log_listing FOREIGN KEY (listing_id)
                               REFERENCES listings(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_log_listing_id ON audit_log(listing_id);
CREATE INDEX idx_audit_log_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);