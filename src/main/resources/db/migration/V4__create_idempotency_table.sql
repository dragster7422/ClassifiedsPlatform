CREATE TABLE idempotency_records (
                                     id UUID PRIMARY KEY,
                                     idempotency_key VARCHAR(255) NOT NULL UNIQUE,
                                     listing_id UUID NOT NULL,
                                     result_json TEXT NOT NULL,
                                     http_status INTEGER NOT NULL,
                                     created_at TIMESTAMP NOT NULL,
                                     expires_at TIMESTAMP NOT NULL,

                                     CONSTRAINT fk_idempotency_listing FOREIGN KEY (listing_id)
                                         REFERENCES listings(id) ON DELETE CASCADE,
                                     CONSTRAINT chk_http_status CHECK (http_status >= 100 AND http_status <= 599)
);

CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);
CREATE INDEX idx_idempotency_listing_id ON idempotency_records(listing_id);