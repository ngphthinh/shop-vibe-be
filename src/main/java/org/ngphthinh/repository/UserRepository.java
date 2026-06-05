package org.ngphthinh.repository;

import jakarta.validation.constraints.NotBlank;
import org.ngphthinh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(@NotBlank(message = "Email is required") String email);

    Optional<User> findByEmail(@NotBlank(message = "Email is required") String email);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.isLocked = :isLocked, u.isActive = :isActive WHERE u.email = :email")
    int updateIsLockedByEmail(@Param("isLocked") boolean isLocked, @Param("isActive") boolean isActive, @NotBlank(message = "Email is required") String email);

    @Query("SELECT u.isLocked FROM User u WHERE u.email = :email")
    Optional<Boolean> isLockedByEmail(@NotBlank(message = "Email is required") String email);
}
