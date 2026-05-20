# 🤖 AI Prompt Log

The IssueFlow backend was designed and constructed iteratively through a pair-programming session with an AI Agent. Below is a summary of the core prompts that drove the architecture and implementation.

---

### **Prompt 1: Design Phase & Master Plan**
> "We are building this backend API using Java with Spring Boot and PostgreSQL. Before writing any code, please carefully review all the requirements and constraints, then generate a detailed, step-by-step development plan. The plan must break down the project into logical, manageable phases..."

### **Prompt 2: Project Setup (Phase 1)**
> "The plan is excellent and incredibly detailed. Let's start implementing. Please implement Phase 1 only: Project setup, update the pom.xml, configure application.yaml, and create the global exception handlers (NotFoundException, ConflictException, etc. along with the GlobalExceptionHandler). Do not write code for any other features or phases yet."

### **Prompt 3: Entity Design (Phase 2)**
> "Phase 1 looks perfect. Let's move to Phase 2: Entity Design & JPA Mapping. Please implement all 7 JPA entities and their respective repositories exactly as specified in the plan. Show me the code for the main entities (User, Project, Ticket) and where you are placing them."

### **Prompt 4: Security Layer (Phase 3)**
> "Phase 2 looks complete. Let's move to Phase 3: Security & JWT Authentication. Please implement the JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl, SecurityConfig, and the AuthController/AuthService exactly as specified in the plan."

### **Prompt 5: Core Tickets & Assignment (Phases 4-6)**
> "Phases 4 & 5 look excellent. Let's move to Phase 6: Tickets API (Core) & Auto-Assignment. Please implement the TicketController, TicketService, and AutoAssignmentService exactly as specified in the plan... Pay close attention to: 1) Declaring the /tickets/deleted route BEFORE /tickets/{ticketId}, 2) Correctly computing the isOverdue field... 3) Implementing the auto-assignment logic based on finding the developer with the minimum open ticket count."

### **Prompt 6: Comments & Mentions (Phase 7)**
> "Phase 6 looks complete and the core tickets API is ready. Let's move to Phase 7: Comments & Mentions API. Please implement the CommentController, CommentService, and MentionService exactly as specified in the plan. Make sure it correctly applies the regex pattern @([a-zA-Z0-9_]+) to extract usernames..."

### **Prompt 7: Audit Logging AOP (Phase 8)**
> "Phase 7 is complete. Let's move to Phase 8: Audit Log (AOP-based). Please implement the custom @Auditable annotation, the AuditAspect to intercept mutating operations, and the corresponding AuditLogService and AuditLogController... Ensure the aspect correctly distinguishes between a regular USER context and a SYSTEM context."

### **Prompt 8: DFS Cycle Detection (Phase 9)**
> "Phase 8 is complete. Let's move to Phase 9: Ticket Dependencies API. Please implement the DependencyController, DependencyService, and DependencyRepository... Pay extreme attention to the DFS Cycle Detection logic: before saving a new dependency, you must ensure it does not create a circular dependency..."

### **Prompt 9: Attachments API (Phase 10)**
> "Phase 9 looks solid and the DFS cycle detection is working perfectly. Let's move to Phase 10: File Storage Integration (S3/Local). Please implement the AttachmentController, AttachmentService, and AttachmentRepository exactly as specified in the plan to handle file uploads and metadata persistence..."

### **Prompt 10: CSV Import/Export (Phase 11)**
> "Phase 10 is complete and file attachments are working. Let's move to Phase 11: CSV Import/Export & Reporting. Please implement the CsvController and CsvService exactly as specified in the plan. Ensure that the Export endpoint correctly generates a downloadable CSV file... Secure these endpoints for ADMIN roles."

### **Prompt 11: Auto-Escalation Scheduler (Phase 12)**
> "Phase 11 is complete. Let's move to Phase 12: Auto-Escalation Background Job. Please enable scheduling in the application and implement the TicketEscalationScheduler exactly as specified in the plan. The scheduler should run every 5 minutes... Ensure that these mutations trigger the Audit Log with 'SYSTEM' as the actor."

### **Prompt 12: Edge Case Testing (Phase 13)**
> "Phase 12 is complete and background jobs are running. Let's move to Phase 13: Unit & Integration Testing. Please implement the complete test suite using JUnit 5 and Mockito... Focus heavily on testing the edge cases: 1) The DFS cycle detection in DependencyService, 2) The auto-assignment logic behavior, and 3) The soft-delete filtering."

### **Prompt 13: Swagger UI (Phase 14)**
> "The test code looks exceptional and perfectly validates our core edge cases. Let's move to the final stage — Phase 14: Documentation & OpenAPI/Swagger. Please integrate Springdoc OpenAPI (Swagger UI) into the project as specified in the plan, configure the local manual testing instructions, and generate the final comprehensive README.md layout."
