ALTER TABLE tenant ADD COLUMN subscription_expires_at TIMESTAMPTZ;

CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    appointment_id UUID REFERENCES appointment(id),
    subscription_plan_id UUID REFERENCES subscription_plan(id),
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'UZS',
    provider VARCHAR(30) NOT NULL,
    provider_transaction_id VARCHAR(128),
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_created_at TIMESTAMPTZ,
    provider_performed_at TIMESTAMPTZ,
    provider_cancelled_at TIMESTAMPTZ,
    provider_reason INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);
CREATE UNIQUE INDEX uq_payment_provider_transaction
    ON payment(provider, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX idx_payment_tenant_created ON payment(tenant_id, created_at DESC);

-- RLS-free provider lookup contains no customer/payment amount data and is only used
-- to recover tenant context for provider callbacks that carry only provider transaction id.
CREATE TABLE payment_provider_transaction (
    provider VARCHAR(30) NOT NULL,
    provider_transaction_id VARCHAR(128) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    payment_id UUID NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(provider, provider_transaction_id),
    UNIQUE(provider, payment_id)
);

ALTER TABLE payment ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment FORCE ROW LEVEL SECURITY;
CREATE POLICY payment_access_gate ON payment
    AS PERMISSIVE FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY payment_tenant_isolation ON payment
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
