package org.ngphthinh.repository;

import org.ngphthinh.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByProductIdAndCartId(Long productId, Long cartId);

    void deleteAllByCartId(Long cartId);
}

