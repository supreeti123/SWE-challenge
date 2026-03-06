package com.todoservice.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todoservice.dto.CreateTodoRequest;
import com.todoservice.dto.UpdateDescriptionRequest;
import java.time.LocalDateTime;
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
                .andExpect(jsonPath("$.error", notNullValue()));
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
                .andExpect(status().isNotFound());
    }

    @Test
    void changeDescription_pastDueItem_returns422() throws Exception {
        Long id = createTodoAndGetId("Past due task", LocalDateTime.now().minusDays(1));

        UpdateDescriptionRequest updateRequest = new UpdateDescriptionRequest("New desc");

        mockMvc.perform(patch("/api/todos/{id}/description", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", notNullValue()));
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
        mockMvc.perform(patch("/api/todos/{id}/done", doneId));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Not done task")));
    }

    @Test
    void getAllItems_includeAllReturnsEverything() throws Exception {
        createTodoAndGetId("Not done task", LocalDateTime.now().plusDays(1));
        Long doneId = createTodoAndGetId("Done task", LocalDateTime.now().plusDays(1));
        mockMvc.perform(patch("/api/todos/{id}/done", doneId));

        mockMvc.perform(get("/api/todos").param("includeAll", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
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
                .andExpect(status().isNotFound());
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
