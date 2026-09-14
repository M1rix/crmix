package uz.mirix.crmix.messaging.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uz.mirix.crmix.scheduling.application.AppointmentCancelledEvent;
import uz.mirix.crmix.scheduling.application.AppointmentCreatedEvent;

@Component
public class AppointmentReminderListener {
    private final ReminderPlanner reminderPlanner;

    public AppointmentReminderListener(ReminderPlanner reminderPlanner) {
        this.reminderPlanner = reminderPlanner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(AppointmentCreatedEvent event) {
        reminderPlanner.plan(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(AppointmentCancelledEvent event) {
        reminderPlanner.cancel(event);
    }
}
