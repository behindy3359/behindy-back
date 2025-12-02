package com.example.backend.repository.common;

import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NoRepositoryBean
public interface UserOwnedSoftDeleteRepository<T, ID> extends SoftDeleteRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.user = :user AND e.deletedAt IS NULL")
    List<T> findByUserActive(@Param("user") User user);

    @Query("SELECT e FROM #{#entityName} e WHERE e.user = :user AND e.deletedAt IS NULL")
    Page<T> findByUserActive(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.user = :user AND e.deletedAt IS NULL")
    long countByUserActive(@Param("user") User user);
}
