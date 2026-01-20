CREATE TABLE listings (
                          id UUID PRIMARY KEY,
                          title VARCHAR(120) NOT NULL,
                          description VARCHAR(5000),
                          price_amount DECIMAL(19, 2) NOT NULL,
                          price_currency VARCHAR(3) NOT NULL,
                          category VARCHAR(50) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT chk_price_positive CHECK (price_amount >= 0),
                          CONSTRAINT chk_title_length CHECK (LENGTH(title) >= 3 AND LENGTH(title) <= 120),
                          CONSTRAINT chk_description_length CHECK (LENGTH(description) <= 5000)
);

CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_created_at ON listings(created_at DESC);
CREATE INDEX idx_listings_price ON listings(price_amount);