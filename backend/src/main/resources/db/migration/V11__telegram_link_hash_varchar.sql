ALTER TABLE telegram_link_token
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING trim(token_hash)::varchar(64);
