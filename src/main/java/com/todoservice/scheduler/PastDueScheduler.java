package com.todoservice.scheduler;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import com.todoservice.repository.TodoItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PastDueScheduler {

    private static final Logger log = LoggerFactory.getLogger(PastDueScheduler.class);

    private final TodoItemRepository repository;

    public PastDueScheduler(TodoItemRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${past-due.check.cron}")
    @Transactional
    public void markOverdueItems() {
        LocalDateTime now = LocalDateTime.now();
        List<TodoItem> overdueItems = repository.findOverdueItems(now);

        if (!overdueItems.isEmpty()) {
            log.info("Marking {} item(s) as PAST_DUE", overdueItems.size());
            overdueItems.forEach(item -> item.setStatus(TodoStatus.PAST_DUE));
            repository.saveAll(overdueItems);
        }
    }
}
