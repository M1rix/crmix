package uz.mirix.crmix.messaging.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkTokenEntity, UUID> {
    Optional<TelegramLinkTokenEntity> findByTokenHash(String tokenHash);
}
