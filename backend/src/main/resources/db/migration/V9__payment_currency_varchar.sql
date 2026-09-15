ALTER TABLE payment
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING trim(currency)::varchar(3);
