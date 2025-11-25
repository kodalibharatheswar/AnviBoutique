package com.anvistudio.boutique.repository;

import com.anvistudio.boutique.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds all items in the cart for a specific user ID.
     */
    List<CartItem> findByUserId(Long userId);

    /**
     * Finds a specific item in the cart by user ID and product ID.
     */
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Deletes all items in a user's cart.
     */
    void deleteByUserId(Long userId);
}