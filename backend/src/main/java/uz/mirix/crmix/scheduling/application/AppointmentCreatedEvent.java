package uz.mirix.crmix.scheduling.application;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCreatedEvent(UUID tenantId, UUID appointmentId, UUID clientId, Instant scheduledAt) {}
