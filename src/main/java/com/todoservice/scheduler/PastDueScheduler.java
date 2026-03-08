package com.todoservice.scheduler;

import com.todoservice.repository.TodoItemRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled component that batch-updates overdue not-done items to past-due.
 */
@Component
public class PastDueScheduler {

    private static final Logger log = LoggerFactory.getLogger(PastDueScheduler.class);

    private final TodoItemRepository repository;
    private final Clock clock;

    public PastDueScheduler(TodoItemRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${past-due.check.cron}")
    @Transactional
    public void markOverdueItems() {
        int updatedItems = repository.markOverdueItems(Instant.now(clock));

        if (updatedItems > 0) {
            log.info("Marked {} item(s) as PAST_DUE", updatedItems);
        }
    }
}
