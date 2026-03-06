package com.todoservice.service;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import com.todoservice.exception.PastDueModificationException;
import com.todoservice.exception.TodoNotFoundException;
import com.todoservice.repository.TodoItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TodoService {

    private final TodoItemRepository repository;

    public TodoService(TodoItemRepository repository) {
        this.repository = repository;
    }

    public TodoItem addItem(String description, LocalDateTime dueAt) {
        TodoItem item = new TodoItem();
        item.setDescription(description);
        item.setDueAt(dueAt);

        if (dueAt != null && dueAt.isBefore(LocalDateTime.now())) {
            item.setStatus(TodoStatus.PAST_DUE);
        }

        return repository.save(item);
    }

    public TodoItem changeDescription(Long id, String newDescription) {
        TodoItem item = findByIdOrThrow(id);
        ensureNotPastDue(item);
        item.setDescription(newDescription);
        return repository.save(item);
    }

    public TodoItem markDone(Long id) {
        TodoItem item = findByIdOrThrow(id);
        ensureNotPastDue(item);
        item.setStatus(TodoStatus.DONE);
        item.setDoneAt(LocalDateTime.now());
        return repository.save(item);
    }

    public TodoItem markNotDone(Long id) {
        TodoItem item = findByIdOrThrow(id);
        ensureNotPastDue(item);
        item.setStatus(TodoStatus.NOT_DONE);
        item.setDoneAt(null);
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TodoItem> getAllItems(boolean includeAll) {
        if (includeAll) {
            return repository.findAll();
        }
        return repository.findByStatus(TodoStatus.NOT_DONE);
    }

    @Transactional(readOnly = true)
    public TodoItem getItemById(Long id) {
        return findByIdOrThrow(id);
    }

    private TodoItem findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    private void ensureNotPastDue(TodoItem item) {
        if (item.getStatus() == TodoStatus.PAST_DUE) {
            throw new PastDueModificationException(item.getId());
        }
        // Real-time check: catch items that became past due between scheduler runs
        if (item.getDueAt() != null
                && item.getStatus() == TodoStatus.NOT_DONE
                && item.getDueAt().isBefore(LocalDateTime.now())) {
            item.setStatus(TodoStatus.PAST_DUE);
            repository.save(item);
            throw new PastDueModificationException(item.getId());
        }
    }
}
