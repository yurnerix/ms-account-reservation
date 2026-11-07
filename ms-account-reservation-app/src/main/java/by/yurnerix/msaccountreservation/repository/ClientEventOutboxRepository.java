package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.ClientEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ClientEventOutboxRepository extends JpaRepository<ClientEventOutbox, Long> {

    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value = """
                    SELECT o.*
                    FROM client_event_outbox o
                    WHERE o.published_at IS NULL
                      AND o.next_attempt_at <= CURRENT_TIMESTAMP
                      AND NOT EXISTS (
                          SELECT 1
                          FROM client_event_outbox previous
                          WHERE previous.client_id = o.client_id
                            AND previous.id < o.id
                            AND previous.published_at IS NULL
                      )
                    ORDER BY o.id
                    LIMIT 1
                    FOR UPDATE OF o SKIP LOCKED
                    """,
            nativeQuery = true
    )
    Optional<ClientEventOutbox> findNextForPublishing();
}
