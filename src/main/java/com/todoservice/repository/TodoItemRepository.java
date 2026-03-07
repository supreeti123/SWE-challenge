package com.todoservice.repository;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByStatus(TodoStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TodoItem t
               SET t.status = 'PAST_DUE'
             WHERE t.status = 'NOT_DONE'
               AND t.dueAt IS NOT NULL
               AND t.dueAt < :now
            """)
    int markOverdueItems(@Param("now") LocalDateTime now);
}
