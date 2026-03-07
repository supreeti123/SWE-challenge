package com.todoservice.scheduler;

import com.todoservice.repository.TodoItemRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        int updatedItems = repository.markOverdueItems(LocalDateTime.now(clock));

        if (updatedItems > 0) {
            log.info("Marked {} item(s) as PAST_DUE", updatedItems);
        }
    }
}
