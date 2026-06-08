package org.ngphthinh.repository;

import org.ngphthinh.entity.Order;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.repository.projection.OrderProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
                           o.payment.status as paymentStatus,
                           o.payment.method as paymentMethod,
                           o.createdAt as createdAt,
                           (SELECT COUNT(oi.id) FROM OrderItem oi WHERE oi.order.id = o.id) as itemCount
                    FROM Order o
                    WHERE
                       (:status IS NULL OR o.status = :status)
                        AND o.createdAt >= :createdAtAfter
                        AND o.createdAt <= :createdAtBefore
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
    Page<OrderProjection> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, Pageable pageable);
}

