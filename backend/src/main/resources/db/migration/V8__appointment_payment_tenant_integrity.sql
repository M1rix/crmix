ALTER TABLE appointment
    ADD CONSTRAINT uq_appointment_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_appointment_id_fkey;
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_tenant_appointment
    FOREIGN KEY (tenant_id, appointment_id)
    REFERENCES appointment(tenant_id, id);
