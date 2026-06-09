package org.ngphthinh.repository;

import jakarta.validation.constraints.NotBlank;
import org.ngphthinh.entity.User;
import org.ngphthinh.repository.projection.CustomerRankItemProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Query("SELECT u.id FROM User u WHERE u.email = :userEmail")
    Optional<Long> findIdByEmail(@NotBlank(message = "Email is required") String userEmail);

    boolean existsByEmailAndIsLockedTrue(String email);

    @Query("""
            SELECT
                   ROW_NUMBER() OVER (ORDER BY SUM(o.totalAmount) DESC) as rank,
                   u.id as customerId,
                   u.fullName as fullName,
                   u.email as email,
                   COUNT (o.id) as totalOrders,
                   SUM(o.totalAmount) as totalSpent
            FROM Order o
            JOIN o.user u
            GROUP BY u.id, u.fullName, u.email
            ORDER BY totalSpent DESC, totalOrders DESC
            """)
    List<CustomerRankItemProjection> findTopCustomers(Pageable pageable);

}
