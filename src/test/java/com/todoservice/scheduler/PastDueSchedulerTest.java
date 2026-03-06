package com.todoservice.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import com.todoservice.repository.TodoItemRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PastDueSchedulerTest {

    @Autowired
    private PastDueScheduler scheduler;

    @Autowired
    private TodoItemRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void markOverdueItems_transitionsNotDoneItemsWithPastDueDate() {
        TodoItem overdueItem = createItem("Overdue", TodoStatus.NOT_DONE, LocalDateTime.now().minusHours(1));
        repository.save(overdueItem);

        scheduler.markOverdueItems();

        TodoItem result = repository.findById(overdueItem.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void markOverdueItems_doesNotAffectDoneItems() {
        TodoItem doneItem = createItem("Done", TodoStatus.DONE, LocalDateTime.now().minusHours(1));
        doneItem.setDoneAt(LocalDateTime.now().minusHours(2));
        repository.save(doneItem);

        scheduler.markOverdueItems();

        TodoItem result = repository.findById(doneItem.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    void markOverdueItems_doesNotAffectItemsWithNoDueDate() {
        TodoItem noDueDate = createItem("No deadline", TodoStatus.NOT_DONE, null);
        repository.save(noDueDate);

        scheduler.markOverdueItems();

        TodoItem result = repository.findById(noDueDate.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
    }

    @Test
    void markOverdueItems_doesNotAffectItemsWithFutureDueDate() {
        TodoItem futureItem = createItem("Future", TodoStatus.NOT_DONE, LocalDateTime.now().plusDays(1));
        repository.save(futureItem);

        scheduler.markOverdueItems();

        TodoItem result = repository.findById(futureItem.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
    }

    private TodoItem createItem(String description, TodoStatus status, LocalDateTime dueAt) {
        TodoItem item = new TodoItem();
        item.setDescription(description);
        item.setStatus(status);
        item.setDueAt(dueAt);
        return item;
    }
}
