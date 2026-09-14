ALTER TABLE client ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'ru';

CREATE TABLE notification_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenant(id) ON DELETE CASCADE,
    code VARCHAR(80) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_notification_template_system
    ON notification_template(code, locale) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_notification_template_tenant
    ON notification_template(tenant_id, code, locale) WHERE tenant_id IS NOT NULL;

CREATE TABLE notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id),
    appointment_id UUID REFERENCES appointment(id) ON DELETE CASCADE,
    channel VARCHAR(30) NOT NULL,
    template_code VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 3),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_notification_due
    ON notification_log(tenant_id, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX idx_notification_appointment
    ON notification_log(tenant_id, appointment_id, created_at DESC);

CREATE TABLE telegram_link_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_telegram_link_client ON telegram_link_token(tenant_id, client_id, expires_at DESC);

ALTER TABLE notification_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_template FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_template_select ON notification_template
    AS RESTRICTIVE FOR SELECT
    USING (tenant_id IS NULL OR tenant_id = current_tenant_id());
CREATE POLICY notification_template_insert ON notification_template
    AS RESTRICTIVE FOR INSERT
    WITH CHECK (tenant_id = current_tenant_id());
CREATE POLICY notification_template_update ON notification_template
    AS RESTRICTIVE FOR UPDATE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
CREATE POLICY notification_template_delete ON notification_template
    AS RESTRICTIVE FOR DELETE
    USING (tenant_id = current_tenant_id());

ALTER TABLE notification_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_log FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_log_tenant_isolation ON notification_log
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

ALTER TABLE telegram_link_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_link_token FORCE ROW LEVEL SECURITY;
CREATE POLICY telegram_link_token_tenant_isolation ON telegram_link_token
    AS RESTRICTIVE
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

INSERT INTO notification_template(tenant_id, code, locale, body) VALUES
(NULL, 'APPOINTMENT_REMINDER_24H', 'ru', 'Напоминание: завтра в {{time}} у вас запись на {{service}} к {{employee}}.'),
(NULL, 'APPOINTMENT_REMINDER_24H', 'uz', 'Eslatma: ertaga soat {{time}} da {{employee}} bilan {{service}} uchun yozilgansiz.'),
(NULL, 'APPOINTMENT_REMINDER_2H', 'ru', 'До визита осталось около 2 часов. Ждём вас в {{time}}.'),
(NULL, 'APPOINTMENT_REMINDER_2H', 'uz', 'Tashrifingizgacha taxminan 2 soat qoldi. Sizni {{time}} da kutamiz.')
ON CONFLICT DO NOTHING;
