package org.ngphthinh.repository;

import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.entity.Cart;
import org.ngphthinh.repository.projection.CartProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {


    @Query("""
            SELECT c.id AS id,
                   c.totalAmount AS totalAmount,
                   c.totalItems AS totalItems,
                   ci.id AS itemId,
                   ci.quantity AS quantity,
                   ci.subtotal AS subtotal,
                   ci.unitPrice AS unitPrice,
                   p.id AS productId,
                   p.name AS productName,
                   p.price AS productPrice,
                   (SELECT pi.imageUrl FROM ProductImage pi
                   WHERE pi.product.id = p.id AND pi.isPrimary = true) AS productPrimaryImageUrl
            FROM Cart c
            JOIN c.items ci
            JOIN ci.product p
            WHERE c.user.email = :email and p.isDeleted = false
            """)
    List<CartProjection>    findByUserEmail(String email);



    @Query("""
            SELECT c.id
            FROM Cart c
            WHERE c.user.email = :email
            """)
    Optional<Long> findIdByUserEmail(String email);

    @Modifying
    @Query("""
            UPDATE Cart c
            SET c.totalAmount = (SELECT COALESCE(SUM(ci.subtotal), 0) FROM CartItem ci WHERE ci.cart.id = c.id),
                c.totalItems = (SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = c.id)
            WHERE c.id = :cartId
            """)
    void updateTotalAmountAndTotalItems(Long cartId);
}

