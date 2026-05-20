package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Project} entities.
 *
 * <p>All "active" queries filter {@code deleted_at IS NULL}.
 * "Deleted" queries filter {@code deleted_at IS NOT NULL}.</p>
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Returns all non-deleted projects. */
    List<Project> findAllByDeletedAtIsNull();

    /** Returns a non-deleted project by ID, or empty if deleted or not found. */
    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    /** ADMIN: returns all soft-deleted projects. */
    List<Project> findAllByDeletedAtIsNotNull();

    /** Checks whether a non-deleted project with the given ID exists. */
    boolean existsByIdAndDeletedAtIsNull(Long id);
}
