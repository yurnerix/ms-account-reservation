package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.ProcessedClientEvent;
import by.yurnerix.msaccountreservation.entity.ProcessedClientEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProcessedClientEventRepository extends JpaRepository<ProcessedClientEvent, ProcessedClientEventId> {

    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value = """
                    INSERT INTO processed_client_event (
                        client_id,
                        event_id
                    )
                    VALUES (:clientId, :eventId)
                    ON CONFLICT (client_id, event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(@Param("clientId") UUID clientId, @Param("eventId") UUID eventId);
}
