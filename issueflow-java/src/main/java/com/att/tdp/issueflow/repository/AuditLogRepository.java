package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.ActorType;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link AuditLog} entries.
 *
 * <p>Audit logs are append-only — no update or delete operations are exposed.</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Flexible filter query for the {@code GET /audit-logs} endpoint.
     * All parameters are optional; passing {@code null} disables that filter.
     *
     * <p>Uses JPQL coalesce pattern so that a {@code null} parameter matches
     * every row for that column.</p>
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId   IS NULL OR a.entityId   = :entityId)
              AND (:action     IS NULL OR a.action     = :action)
              AND (:actor      IS NULL OR a.actor      = :actor)
            ORDER BY a.timestamp DESC
            """)
    List<AuditLog> findWithFilters(
            @Param("entityType") EntityType entityType,
            @Param("entityId")   Long        entityId,
            @Param("action")     AuditAction action,
            @Param("actor")      ActorType   actor
    );
}
