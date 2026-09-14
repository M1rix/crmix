CREATE TABLE employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    full_name VARCHAR(160) NOT NULL,
    specialization VARCHAR(160),
    work_schedule JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_employee_tenant_active ON employee(tenant_id, is_active);

CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    telegram_chat_id BIGINT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_client_tenant_phone UNIQUE (tenant_id, phone)
);
CREATE INDEX idx_client_tenant_created ON client(tenant_id, created_at DESC);
CREATE INDEX idx_client_name_trgm ON client USING gin (lower(full_name) gin_trgm_ops);
CREATE INDEX idx_client_phone_trgm ON client USING gin (phone gin_trgm_ops);

CREATE TABLE service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 5 AND 1440),
    price NUMERIC(19,2) NOT NULL CHECK (price >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_service_tenant_active ON service(tenant_id, is_active);

DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['employee', 'client', 'service']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format(
            'CREATE POLICY %I ON %I AS PERMISSIVE FOR ALL USING (true) WITH CHECK (true)',
            table_name || '_access_gate', table_name);
        EXECUTE format(
            'CREATE POLICY %I ON %I AS RESTRICTIVE USING (tenant_id = current_tenant_id()) WITH CHECK (tenant_id = current_tenant_id())',
            table_name || '_tenant_isolation', table_name);
    END LOOP;
END $$;
