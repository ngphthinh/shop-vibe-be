package org.ngphthinh.repository;

import org.ngphthinh.entity.Order;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.repository.projection.*;
import org.ngphthinh.repository.projection.AllTimeStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(
            value = """
                    SELECT o.id as id,
                           o.orderCode as orderCode,
                           o.status as status,
                           o.totalAmount as totalAmount,
                           o.shippingAddress as shippingAddress,
                           o.note as note,
                           o.payment.status as paymentStatus,
                           o.payment.method as paymentMethod,
                           o.createdAt as createdAt,
                           (SELECT COUNT(oi.id) FROM OrderItem oi WHERE oi.order.id = o.id) as itemCount
                    FROM Order o
                    WHERE o.user.email = :userEmail
                        AND (:status IS NULL OR o.status = :status)
                        AND o.createdAt >= :createdAtAfter
                        AND o.createdAt <= :createdAtBefore
                    """,
            countQuery = """
                    SELECT COUNT(o.id)
                    FROM Order o
                    WHERE o.user.email = :userEmail
                        AND (:status IS NULL OR o.status = :status)
                        AND o.createdAt >= :createdAtAfter
                        AND o.createdAt <= :createdAtBefore
                    """
    )
    Page<OrderProjection> findByUserEmailAndStatusAndCreatedAtBetween(String userEmail, OrderStatus status, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, Pageable pageable);

    @Query(
            value = """
                    SELECT o.id as id,
                           o.orderCode as orderCode,
                           o.status as status,
                           o.totalAmount as totalAmount,
                           o.shippingAddress as shippingAddress,
                           o.note as note,
                           o.payment.status as paymentStatus,
                           o.payment.method as paymentMethod,
                           o.createdAt as createdAt,
                           oi.product.id as productId,
                           oi.productName as productName,
                           oi.productThumbnail as productImageUrl,
                           oi.unitPrice as price,
                           oi.quantity as quantity, oi.subtotal as subtotal
                    
                    FROM Order o
                    JOIN o.items oi
                    WHERE o.id = :orderId
                        AND o.user.email = :email
                    """
    )
    List<OrderProjection> findProjectionByIdAndUserEmail(Long orderId, String email);

    @Query("""
            SELECT o
            FROM Order o
            JOIN FETCH o.items oi
            JOIN FETCH oi.product
            WHERE o.id =:orderId
            AND o.user.email =:email
            """)
    Optional<Order> findByIdAndUserEmail(Long orderId, String email);

    @Query(
            value = """
                    SELECT o.id as id,
                           o.orderCode as orderCode,
                           o.status as status,
                           o.totalAmount as totalAmount,
                           o.shippingAddress as shippingAddress,
                           o.note as note,
                           u.fullName as customerName,
                           o.payment.status as paymentStatus,
                           o.payment.method as paymentMethod,
                           o.createdAt as createdAt,
                           (SELECT COUNT(oi.id) FROM OrderItem oi WHERE oi.order.id = o.id) as itemCount
                    FROM Order o
                    JOIN o.user u
                    WHERE
                       (:status IS NULL OR o.status = :status)
                        AND o.createdAt >= :createdAtAfter
                        AND o.createdAt <= :createdAtBefore
                        AND (lower(u.fullName) like lower(concat('%', :keyword, '%')) OR lower(o.orderCode) like lower(concat('%', :keyword, '%')))
                    """,
            countQuery = """
                    SELECT COUNT(o.id)
                    FROM Order o
                    WHERE
                       (:status IS NULL OR o.status = :status)
                        AND o.createdAt >= :createdAtAfter
                        AND o.createdAt <= :createdAtBefore
                    """
    )
    Page<OrderProjection> findByStatusAndCreatedAtBetweenAndCustomerNameContaining(OrderStatus status, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, String keyword, Pageable pageable);

    List<Order> findByStatusAndUserIdIn(OrderStatus status, Collection<Long> userIds);

    @Query("""
            SELECT COUNT(o) > 0
            FROM Order o
            JOIN o.items oi
            WHERE o.user.id = :userId
            AND oi.product.id = :productId
            AND o.status = :status
            """)
    boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, OrderStatus status);

    @Query("""
            SELECT COUNT(o) > 0
            FROM Order o
            JOIN o.items oi
            WHERE o.user.id = :userId
            AND oi.product.id = :productId
            """)
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("""
            SELECT
                DATE(o.createdAt) AS date,
                SUM(o.totalAmount) AS revenue
            FROM Order o
            WHERE o.createdAt BETWEEN :from AND :to
            GROUP BY DATE(o.createdAt)
            ORDER BY DATE(o.createdAt) ASC
            """)
    List<RevenueProjection> findRevenueByDateRange(LocalDateTime from, LocalDateTime to);


    @Query("""
                SELECT
                    COUNT(DISTINCT o.id) AS orders,
                    SUM(o.totalAmount) AS revenue,
                    COUNT(DISTINCT o.user.id) AS newCustomers
                FROM Order o
                WHERE o.createdAt BETWEEN :from AND :to
            """)
    PeriodStatsProjection findPeriodStats(LocalDateTime from, LocalDateTime to);


    @Query("""
                    SELECT o.status AS status,
                           COUNT(o.id) AS count
                    FROM Order o
                    WHERE o.createdAt BETWEEN :from AND :to
                      AND (:isStatusesEmpty = true OR o.status NOT IN :orderStatuses)
                    GROUP BY o.status
            """)
    List<OrdersByStatusProjection> statisticOrderByByWithoutStatus(
            List<OrderStatus> orderStatuses, boolean isStatusesEmpty,
            LocalDateTime from, LocalDateTime to);

    @Query("""
                SELECT
                    COUNT(DISTINCT o.id) AS totalOrders,
                    COUNT(DISTINCT o.user.id) AS totalCustomers,
                    (SELECT COUNT(p.id) FROM Product p WHERE p.isDeleted = false) AS totalProducts
                FROM Order o
            """)
    AllTimeStatsProjection findAllTimeStats();

    @Query("""
            SELECT o
            FROM Order o
            JOIN FETCH o.items oi
            JOIN FETCH o.payment
            JOIN FETCH o.user
            JOIN FETCH oi.product
            WHERE o.id =:id
            """)
    Optional<Order> findOrderById(Long id);
}

