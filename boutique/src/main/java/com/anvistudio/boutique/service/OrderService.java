package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.CartItem;
import com.anvistudio.boutique.model.Order;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
     * NEW: Fulfills the order by creating an Order entity from the current cart items.
     * This method assumes payment verification (like Stripe session ID lookup) has already occurred.
     * @param userId The ID of the user.
     * @param cartItems The list of cart items to convert into an order.
     * @return The newly created Order entity.
     */
    @Transactional
    public Order createOrderFromCart(Long userId, List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order from an empty cart.");
        }

        User user = cartItems.get(0).getUser(); // Assuming all items belong to the same user

        // 1. Calculate Total Amount
        // Note: The conversion from double to BigDecimal helps maintain currency precision.
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> BigDecimal.valueOf(item.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // 2. Create Order Item Snapshot (UPDATED to include Product ID)
        // Format: <QTY>x<NAME>[ID:<ID>](<PRICE>); ...
        String orderItemsSnapshot = cartItems.stream()
                .map(item -> String.format("%dx %s [ID:%d] (₹%.2f)",
                        item.getQuantity(),
                        item.getProduct().getName(),
                        item.getProduct().getId(),  // <<< CRITICAL: Include Product ID
                        item.getTotalPrice()))
                .collect(Collectors.joining("; "));

        // 3. Mock Shipping Address Snapshot
        String shippingAddressSnapshot = "Shipping Address: Pending Address Selection - Mock Data for Demo";

        // 4. Create the Order
        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setOrderDate(new Date());
        newOrder.setTotalAmount(totalAmount);
        newOrder.setStatus(Order.OrderStatus.PROCESSING); // Status remains PROCESSING (payment confirmed)
        newOrder.setShippingAddressSnapshot(shippingAddressSnapshot);
        newOrder.setOrderItemsSnapshot(orderItemsSnapshot);

        return orderRepository.save(newOrder);
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


//package com.anvistudio.boutique.service;
//
//import com.anvistudio.boutique.model.CartItem;
//import com.anvistudio.boutique.model.Order;
//import com.anvistudio.boutique.model.User;
//import com.anvistudio.boutique.repository.OrderRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.math.BigDecimal;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class OrderService {
//
//    private final OrderRepository orderRepository;
//    private final UserService userService;
//
//    public OrderService(OrderRepository orderRepository, UserService userService) {
//        this.orderRepository = orderRepository;
//        this.userService = userService;
//    }
//
//    /**
//     * Retrieves all orders for the authenticated user.
//     */
//    public List<Order> getOrdersByUsername(String username) {
//        User user = userService.findUserByUsername(username)
//                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
//
//        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
//    }
//
//    /**
//     * NEW: Fulfills the order by creating an Order entity from the current cart items.
//     * This method assumes payment verification (like Stripe session ID lookup) has already occurred.
//     * @param userId The ID of the user.
//     * @param cartItems The list of cart items to convert into an order.
//     * @return The newly created Order entity.
//     */
//    @Transactional
//    public Order createOrderFromCart(Long userId, List<CartItem> cartItems) {
//        if (cartItems.isEmpty()) {
//            throw new IllegalStateException("Cannot create an order from an empty cart.");
//        }
//
//        User user = cartItems.get(0).getUser(); // Assuming all items belong to the same user
//
//        // 1. Calculate Total Amount
//        // Note: The conversion from double to BigDecimal helps maintain currency precision.
//        BigDecimal totalAmount = cartItems.stream()
//                .map(item -> BigDecimal.valueOf(item.getTotalPrice()))
//                .reduce(BigDecimal.ZERO, BigDecimal::add)
//                .setScale(2, BigDecimal.ROUND_HALF_UP);
//
//        // 2. Create Order Item Snapshot (Simplified list of purchased items)
//        String orderItemsSnapshot = cartItems.stream()
//                .map(item -> String.format("%dx %s (₹%.2f)",
//                        item.getQuantity(),
//                        item.getProduct().getName(),
//                        item.getTotalPrice()))
//                .collect(Collectors.joining("; "));
//
//        // 3. Mock Shipping Address Snapshot (In a real app, this would come from AddressService/checkout form selection)
//        // For now, we use a placeholder:
//        String shippingAddressSnapshot = "Shipping Address: Pending Address Selection - Mock Data for Demo";
//
//        // 4. Create the Order
//        Order newOrder = new Order();
//        newOrder.setUser(user);
//        newOrder.setOrderDate(new Date());
//        newOrder.setTotalAmount(totalAmount);
//        newOrder.setStatus(Order.OrderStatus.PROCESSING); // Set to processing as payment is confirmed
//        newOrder.setShippingAddressSnapshot(shippingAddressSnapshot);
//        newOrder.setOrderItemsSnapshot(orderItemsSnapshot);
//
//        return orderRepository.save(newOrder);
//    }
//
//    /**
//     * NOTE: For demonstration, this method can be used to populate dummy data.
//     */
//    public void populateDummyOrders(User user) {
//        // Mock implementation for demo purposes
//        if (orderRepository.findByUserIdOrderByOrderDateDesc(user.getId()).isEmpty()) {
//            // In a real application, this logic would handle actual placed orders.
//        }
//    }
//}


/*
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

    */
/**
     * Retrieves all orders for the authenticated user.
     *//*

    public List<Order> getOrdersByUsername(String username) {
        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
    }

    */
/**
     * NOTE: For demonstration, this method can be used to populate dummy data.
     *//*

    public void populateDummyOrders(User user) {
        // Mock implementation for demo purposes
        if (orderRepository.findByUserIdOrderByOrderDateDesc(user.getId()).isEmpty()) {
            // In a real application, this logic would handle actual placed orders.
        }
    }
}*/
