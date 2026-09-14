package uz.mirix.crmix.scheduling.application;

import java.util.UUID;

public record AppointmentCancelledEvent(UUID tenantId, UUID appointmentId, UUID clientId) {}
