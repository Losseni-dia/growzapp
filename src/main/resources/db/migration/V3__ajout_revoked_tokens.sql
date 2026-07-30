CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_jti VARCHAR(255) NOT NULL UNIQUE,
    revoked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_revoked_tokens_jti ON revoked_tokens (token_jti);