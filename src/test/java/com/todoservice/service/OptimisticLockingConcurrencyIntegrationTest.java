package com.todoservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todoservice.entity.TodoItem;
import com.todoservice.repository.TodoItemRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingConcurrencyIntegrationTest {

    @Autowired
    private TodoItemRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void concurrentDescriptionUpdates_oneTransactionFailsWithOptimisticLock() throws Exception {
        TodoItem item = new TodoItem();
        item.setDescription("Original");
        item.setDueAt(Instant.now().plus(1, ChronoUnit.DAYS));
        item = repository.saveAndFlush(item);

        CyclicBarrier barrier = new CyclicBarrier(2);
        Long id = item.getId();

        Future<Void> first = executorService.submit(() -> {
            updateDescriptionInNewTransaction(id, "First update", barrier);
            return null;
        });
        Future<Void> second = executorService.submit(() -> {
            updateDescriptionInNewTransaction(id, "Second update", barrier);
            return null;
        });

        int successfulUpdates = 0;
        int optimisticConflicts = 0;

        for (Future<Void> future : java.util.List.of(first, second)) {
            try {
                future.get();
                successfulUpdates++;
            } catch (ExecutionException ex) {
                if (isOptimisticLockFailure(ex.getCause())) {
                    optimisticConflicts++;
                } else {
                    throw ex;
                }
            }
        }

        assertThat(successfulUpdates).isEqualTo(1);
        assertThat(optimisticConflicts).isEqualTo(1);

        TodoItem persisted = repository.findById(id).orElseThrow();
        assertThat(persisted.getDescription()).isIn("First update", "Second update");
    }

    void updateDescriptionInNewTransaction(Long id, String description, CyclicBarrier barrier) throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            TodoItem item = repository.findById(id).orElseThrow();
            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to align concurrent transactions", ex);
            }
            item.setDescription(description);
            repository.saveAndFlush(item);
        });
    }

    private boolean isOptimisticLockFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ObjectOptimisticLockingFailureException
                    || current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
