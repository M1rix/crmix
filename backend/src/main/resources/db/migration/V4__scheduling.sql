ALTER TABLE tenant ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Tashkent';

CREATE TABLE appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    service_id UUID NOT NULL REFERENCES service(id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 5 AND 1440),
    status VARCHAR(30) NOT NULL,
    cancellation_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_appointment_employee_time ON appointment(tenant_id, employee_id, scheduled_at);
CREATE INDEX idx_appointment_tenant_status_time ON appointment(tenant_id, status, scheduled_at);
CREATE INDEX idx_appointment_client_time ON appointment(tenant_id, client_id, scheduled_at DESC);

ALTER TABLE appointment ADD CONSTRAINT no_appointment_overlap
EXCLUDE USING gist (
    tenant_id WITH =,
    employee_id WITH =,
    tstzrange(scheduled_at, scheduled_at + duration_minutes * interval '1 minute', '[)') WITH &&
)
WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

ALTER TABLE appointment ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment FORCE ROW LEVEL SECURITY;
CREATE POLICY appointment_tenant_isolation ON appointment
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
