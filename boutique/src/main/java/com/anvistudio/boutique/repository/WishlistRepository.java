package com.anvistudio.boutique.repository;

import com.anvistudio.boutique.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Finds all wishlist items for a specific user ID.
     */
    List<Wishlist> findByUserId(Long userId);

    /**
     * Finds a specific wishlist item by user ID and product ID (used for checking if an item is already wished).
     */
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Deletes a specific wishlist item by user ID and product ID.
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
}