ALTER TABLE tenant
    ADD COLUMN next_subscription_plan_id UUID REFERENCES subscription_plan(id);
