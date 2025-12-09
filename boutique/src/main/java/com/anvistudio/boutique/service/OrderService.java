package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.Order;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;

    public OrderService(OrderRepository orderRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    /**
     * Retrieves all orders for the authenticated user.
     */
    public List<Order> getOrdersByUsername(String username) {
        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
    }

    /**
     * NOTE: For demonstration, this method can be used to populate dummy data.
     */
    public void populateDummyOrders(User user) {
        // Mock implementation for demo purposes
        if (orderRepository.findByUserIdOrderByOrderDateDesc(user.getId()).isEmpty()) {
            // In a real application, this logic would handle actual placed orders.
        }
    }
}