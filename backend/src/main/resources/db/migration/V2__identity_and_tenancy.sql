CREATE TABLE subscription_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    price_monthly NUMERIC(19,2) NOT NULL CHECK (price_monthly >= 0),
    max_employees INTEGER NOT NULL CHECK (max_employees > 0),
    features JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO subscription_plan(code, name, price_monthly, max_employees, features)
VALUES
    ('STARTER', 'Starter', 0, 5, '{"appointments":true,"telegram":true}'::jsonb),
    ('PRO', 'Pro', 199000, 30, '{"appointments":true,"telegram":true,"analytics":true}'::jsonb)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    business_type VARCHAR(80) NOT NULL,
    subscription_plan_id UUID REFERENCES subscription_plan(id),
    subscription_status VARCHAR(30) NOT NULL,
    trial_ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_app_user_tenant_email ON app_user(tenant_id, lower(email));
CREATE INDEX idx_app_user_tenant_role ON app_user(tenant_id, role) WHERE is_active;

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user_active ON refresh_token(tenant_id, user_id, expires_at) WHERE revoked_at IS NULL;

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID
LANGUAGE sql STABLE
AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid
$$;

ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_user FORCE ROW LEVEL SECURITY;
CREATE POLICY app_user_tenant_isolation ON app_user
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

ALTER TABLE refresh_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_token FORCE ROW LEVEL SECURITY;
CREATE POLICY refresh_token_tenant_isolation ON refresh_token
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
