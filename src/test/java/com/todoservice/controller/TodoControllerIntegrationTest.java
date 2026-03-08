package com.todoservice.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todoservice.dto.CreateTodoRequest;
import com.todoservice.dto.UpdateDescriptionRequest;
import com.todoservice.repository.TodoItemRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoItemRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void addItem_validRequest_returns201() throws Exception {
        CreateTodoRequest request = new CreateTodoRequest("Buy groceries", LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.description", is("Buy groceries")))
                .andExpect(jsonPath("$.status", is("not done")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.doneAt", nullValue()));
    }

    @Test
    void addItem_blankDescription_returns400() throws Exception {
        CreateTodoRequest request = new CreateTodoRequest("", LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void addItem_pastDueDate_createsWithPastDueStatus() throws Exception {
        CreateTodoRequest request = new CreateTodoRequest("Overdue task", LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("past due")));
    }

    @Test
    void addItem_descriptionTooLong_returns400() throws Exception {
        String longDescription = "a".repeat(501);
        CreateTodoRequest request = new CreateTodoRequest(longDescription, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/todos")));
    }

    @Test
    void changeDescription_validRequest_returns200() throws Exception {
        Long id = createTodoAndGetId("Original description", LocalDateTime.now().plusDays(1));

        UpdateDescriptionRequest updateRequest = new UpdateDescriptionRequest("Updated description");

        mockMvc.perform(patch("/api/todos/{id}/description", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void changeDescription_nonExistentItem_returns404() throws Exception {
        UpdateDescriptionRequest updateRequest = new UpdateDescriptionRequest("New desc");

        mockMvc.perform(patch("/api/todos/{id}/description", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TODO_NOT_FOUND")))
                .andExpect(jsonPath("$.path", is("/api/todos/999/description")));
    }

    @Test
    void changeDescription_pastDueItem_returns422() throws Exception {
        Long id = createTodoAndGetId("Past due task", LocalDateTime.now().minusDays(1));

        UpdateDescriptionRequest updateRequest = new UpdateDescriptionRequest("New desc");

        mockMvc.perform(patch("/api/todos/{id}/description", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PAST_DUE_MODIFICATION_FORBIDDEN")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void markDone_notDoneItem_returns200() throws Exception {
        Long id = createTodoAndGetId("Task to complete", LocalDateTime.now().plusDays(1));

        mockMvc.perform(patch("/api/todos/{id}/done", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("done")))
                .andExpect(jsonPath("$.doneAt", notNullValue()));
    }

    @Test
    void markDone_pastDueItem_returns422() throws Exception {
        Long id = createTodoAndGetId("Past due task", LocalDateTime.now().minusDays(1));

        mockMvc.perform(patch("/api/todos/{id}/done", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void markNotDone_doneItem_returns200() throws Exception {
        Long id = createTodoAndGetId("Task to undo", LocalDateTime.now().plusDays(1));

        // First mark done
        mockMvc.perform(patch("/api/todos/{id}/done", id))
                .andExpect(status().isOk());

        // Then mark not-done
        mockMvc.perform(patch("/api/todos/{id}/not-done", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("not done")))
                .andExpect(jsonPath("$.doneAt", nullValue()));
    }

    @Test
    void getAllItems_defaultReturnsNotDoneOnly() throws Exception {
        createTodoAndGetId("Not done task", LocalDateTime.now().plusDays(1));
        Long doneId = createTodoAndGetId("Done task", LocalDateTime.now().plusDays(1));
        mockMvc.perform(patch("/api/todos/{id}/done", doneId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Not done task")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void getAllItems_includeAllReturnsEverything() throws Exception {
        createTodoAndGetId("Not done task", LocalDateTime.now().plusDays(1));
        Long doneId = createTodoAndGetId("Done task", LocalDateTime.now().plusDays(1));
        mockMvc.perform(patch("/api/todos/{id}/done", doneId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos").param("includeAll", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void getAllItems_withPagination_appliesPageAndSize() throws Exception {
        createTodoAndGetId("Task 1", LocalDateTime.now().plusDays(1));
        createTodoAndGetId("Task 2", LocalDateTime.now().plusDays(1));
        createTodoAndGetId("Task 3", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/todos")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void getAllItems_withOversizedPageSize_isClampedToConfiguredMaximum() throws Exception {
        createTodoAndGetId("Task 1", LocalDateTime.now().plusDays(1));
        createTodoAndGetId("Task 2", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/todos").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    void getItemById_existingItem_returns200() throws Exception {
        Long id = createTodoAndGetId("My task", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.description", is("My task")));
    }

    @Test
    void getItemById_nonExistentItem_returns404() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TODO_NOT_FOUND")))
                .andExpect(jsonPath("$.path", is("/api/todos/999")));
    }

    @Test
    void getItemById_invalidPathVariable_returns400() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    void requestId_isPropagatedWhenProvided() throws Exception {
        Long id = createTodoAndGetId("Request ID task", LocalDateTime.now().plusDays(1));
        String requestId = "test-request-id-123";

        mockMvc.perform(get("/api/todos/{id}", id).header("X-Request-Id", requestId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", requestId));
    }

    @Test
    void requestId_isGeneratedForErrorResponses() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    private Long createTodoAndGetId(String description, LocalDateTime dueAt) throws Exception {
        CreateTodoRequest request = new CreateTodoRequest(description, dueAt);

        MvcResult result = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("id").asLong();
    }
}
