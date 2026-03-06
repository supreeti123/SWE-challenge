package com.todoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import com.todoservice.exception.PastDueModificationException;
import com.todoservice.exception.TodoNotFoundException;
import com.todoservice.repository.TodoItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoItemRepository repository;

    @InjectMocks
    private TodoService todoService;

    private TodoItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new TodoItem();
        sampleItem.setId(1L);
        sampleItem.setDescription("Buy groceries");
        sampleItem.setStatus(TodoStatus.NOT_DONE);
        sampleItem.setCreatedAt(LocalDateTime.now());
        sampleItem.setDueAt(LocalDateTime.now().plusDays(1));
    }

    @Test
    void addItem_withValidDescription_createsItem() {
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> {
            TodoItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });

        TodoItem result = todoService.addItem("Buy groceries", LocalDateTime.now().plusDays(1));

        assertThat(result.getDescription()).isEqualTo("Buy groceries");
        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        verify(repository).save(any(TodoItem.class));
    }

    @Test
    void addItem_withPastDueDate_setsStatusPastDue() {
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoItem result = todoService.addItem("Overdue task", LocalDateTime.now().minusDays(1));

        assertThat(result.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void addItem_withNoDueDate_setsStatusNotDone() {
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoItem result = todoService.addItem("No deadline", null);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(result.getDueAt()).isNull();
    }

    @Test
    void changeDescription_existingNotDoneItem_updatesDescription() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoItem result = todoService.changeDescription(1L, "Buy vegetables");

        assertThat(result.getDescription()).isEqualTo("Buy vegetables");
    }

    @Test
    void changeDescription_pastDueItem_throwsPastDueException() {
        sampleItem.setStatus(TodoStatus.PAST_DUE);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));

        assertThatThrownBy(() -> todoService.changeDescription(1L, "New desc"))
                .isInstanceOf(PastDueModificationException.class);
    }

    @Test
    void changeDescription_nonExistentItem_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.changeDescription(99L, "New desc"))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void markDone_notDoneItem_setsStatusDoneAndDoneAt() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoItem result = todoService.markDone(1L);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(result.getDoneAt()).isNotNull();
    }

    @Test
    void markDone_pastDueItem_throwsPastDueException() {
        sampleItem.setStatus(TodoStatus.PAST_DUE);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));

        assertThatThrownBy(() -> todoService.markDone(1L))
                .isInstanceOf(PastDueModificationException.class);
    }

    @Test
    void markDone_itemWithExpiredDueDate_throwsPastDueException() {
        sampleItem.setDueAt(LocalDateTime.now().minusHours(1));
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> todoService.markDone(1L))
                .isInstanceOf(PastDueModificationException.class);

        // Verify the item was eagerly transitioned to PAST_DUE
        ArgumentCaptor<TodoItem> captor = ArgumentCaptor.forClass(TodoItem.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void markNotDone_doneItem_setsStatusNotDoneAndClearsDoneAt() {
        sampleItem.setStatus(TodoStatus.DONE);
        sampleItem.setDoneAt(LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(repository.save(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TodoItem result = todoService.markNotDone(1L);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(result.getDoneAt()).isNull();
    }

    @Test
    void markNotDone_pastDueItem_throwsPastDueException() {
        sampleItem.setStatus(TodoStatus.PAST_DUE);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));

        assertThatThrownBy(() -> todoService.markNotDone(1L))
                .isInstanceOf(PastDueModificationException.class);
    }

    @Test
    void getAllItems_includeAllFalse_returnsOnlyNotDone() {
        TodoItem doneItem = new TodoItem();
        doneItem.setStatus(TodoStatus.DONE);

        when(repository.findByStatus(TodoStatus.NOT_DONE)).thenReturn(List.of(sampleItem));

        List<TodoItem> result = todoService.getAllItems(false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        verify(repository).findByStatus(TodoStatus.NOT_DONE);
        verify(repository, never()).findAll();
    }

    @Test
    void getAllItems_includeAllTrue_returnsAll() {
        TodoItem doneItem = new TodoItem();
        doneItem.setStatus(TodoStatus.DONE);

        when(repository.findAll()).thenReturn(List.of(sampleItem, doneItem));

        List<TodoItem> result = todoService.getAllItems(true);

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void getItemById_existingItem_returnsItem() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleItem));

        TodoItem result = todoService.getItemById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Buy groceries");
    }

    @Test
    void getItemById_nonExistentItem_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.getItemById(99L))
                .isInstanceOf(TodoNotFoundException.class);
    }
}
