package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.dto.response.WorkloadResponse;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.annotation.Auditable;
import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.EntityType;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for project management.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllByDeletedAtIsNull().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = getActiveProjectEntity(id);
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, entityType = EntityType.PROJECT)
    public ProjectResponse createProject(CreateProjectRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new NotFoundException("Owner User", request.getOwnerId()));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        project = projectRepository.save(project);
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.PROJECT)
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = getActiveProjectEntity(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        project = projectRepository.save(project);
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, entityType = EntityType.PROJECT)
    public void softDeleteProject(Long id) {
        Project project = getActiveProjectEntity(id);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    // -----------------------------------------------------------------------
    // Soft Delete API (ADMIN only)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjectResponse> getDeletedProjects() {
        return projectRepository.findAllByDeletedAtIsNotNull().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.PROJECT)
    public void restoreProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project", id));
        
        if (project.getDeletedAt() == null) {
            // Already active, do nothing
            return;
        }

        project.setDeletedAt(null);
        projectRepository.save(project);
    }

    // -----------------------------------------------------------------------
    // Workload API
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<WorkloadResponse> getProjectWorkload(Long projectId) {
        // Validate project exists and is active
        getActiveProjectEntity(projectId);

        List<TicketStatus> closedStatuses = List.of(TicketStatus.DONE, TicketStatus.CANCELLED);
        return ticketRepository.getProjectWorkload(projectId, closedStatuses);
    }

    // -----------------------------------------------------------------------
    // Internal Helper
    // -----------------------------------------------------------------------

    public Project getActiveProjectEntity(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Project", id));
    }
}
