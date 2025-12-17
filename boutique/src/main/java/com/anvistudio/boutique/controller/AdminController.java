package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.model.Order; // NEW
import com.anvistudio.boutique.model.Review; // NEW
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.service.ContactService;
import com.anvistudio.boutique.service.OrderService; // NEW
import com.anvistudio.boutique.service.ReviewService; // NEW
import com.anvistudio.boutique.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controller for admin-specific functionality, requiring ROLE_ADMIN access.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final ContactService contactService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public AdminController(ProductService productService, UserService userService, ContactService contactService,
                           OrderService orderService, ReviewService reviewService) {
        this.productService = productService;
        this.userService = userService;
        this.contactService = contactService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }


    // --- Admin Security Check ---
    private String checkAdminCredentials(UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();
        if ("admin".equals(username) && !userService.isAdminCredentialsUpdated(username)) {
            redirectAttributes.addFlashAttribute("forceUpdateMessage", "SECURITY ALERT: Please update your default login credentials immediately.");
            return "redirect:/admin/profile";
        }
        return null;
    }


    private String[] getAllCategories() {
        return new String[]{
                "Sarees", "Lehengas", "Kurtis", "Long Frocks", "Mom & Me", "Crop Top – Skirts",
                "Handlooms", "Casual Frocks", "Ready To Wear", "Dupattas", "Kids wear",
                "Dress Material", "Blouses", "Fabrics"
        };
    }

    // =========================================================================
    // 1. Order Management & Moderation
    // =========================================================================

    /**
     * Displays a comprehensive dashboard for order management (Processing, Returns, etc.).
     */
    @GetMapping("/orders")
    public String adminOrderDashboard(Model model) {
        // FIX: Use the new service layer method to retrieve all orders
        List<Order> allOrders = orderService.getAllOrders();
        model.addAttribute("orders", allOrders);
        model.addAttribute("statusTypes", Order.OrderStatus.values());

        return "admin_order_dashboard";
    }

    /**
     * Endpoint to update an order's status (used for Shipping/Delivering).
     */
    @PostMapping("/order/updateStatus/{orderId}")
    public String updateOrderStatus(@PathVariable Long orderId,
                                    @RequestParam("newStatus") String newStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));

            Order.OrderStatus status = Order.OrderStatus.valueOf(newStatus);
            order.setStatus(status);

            // FIX: Use the new public service method to save the order
            orderService.saveOrder(order);

            redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " status updated to " + newStatus + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order status: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    /**
     * Endpoint to finalize a return/refund.
     */
    @PostMapping("/order/finalizeReturn/{orderId}")
    public String finalizeReturn(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));

            if (order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
                throw new IllegalStateException("Order must be RETURN_REQUESTED to finalize return.");
            }

            // Set status to indicate refund completion
            order.setStatus(Order.OrderStatus.RETURNED);
            // FIX: Use the new public service method to save the order
            orderService.saveOrder(order);

            System.out.println("LOG: Refund approved and finalized for Order: " + orderId);

            redirectAttributes.addFlashAttribute("successMessage", "Return for Order #" + orderId + " approved and refund processed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error finalizing return: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }



    // =========================================================================
    // 2. Review Moderation
    // =========================================================================

    /**
     * Displays a page for admin review moderation (unapproved reviews).
     */
    @GetMapping("/reviews/moderate")
    public String moderateReviews(Model model) {
        // FIX: Use the new service layer method to retrieve unapproved reviews
        List<Review> unapprovedReviews = reviewService.getUnapprovedReviews();
        model.addAttribute("reviews", unapprovedReviews);
        return "admin_review_moderate";
    }

    /**
     * Approves a specific review.
     */
    @PostMapping("/review/approve/{reviewId}")
    public String approveReview(@PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        try {
            reviewService.approveReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "Review approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve review: " + e.getMessage());
        }
        return "redirect:/admin/reviews/moderate";
    }

    /**
     * Deletes a specific review (moderation rejection).
     */
    @PostMapping("/review/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "Review deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete review: " + e.getMessage());
        }
        return "redirect:/admin/reviews/moderate";
    }

    // =========================================================================
    // 3. Product & Profile Management (Existing/Cleaned)
    // =========================================================================

    @GetMapping("/dashboard")
    public String adminDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "category", required = false) String category,
            Model model, RedirectAttributes redirectAttributes) {

        String securityRedirect = checkAdminCredentials(userDetails, model, redirectAttributes);
        if (securityRedirect != null) {
            return securityRedirect;
        }

        model.addAttribute("products", productService.getProductsByCategoryOrAll(category));
        model.addAttribute("newProduct", new Product());
        model.addAttribute("currentCategory", category);
        model.addAttribute("allCategories", getAllCategories());

        return "admin_dashboard";
    }

    @PostMapping("/addProduct")
    public String addProduct(@ModelAttribute Product newProduct, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(newProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product ID " + id + " deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/product/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        Optional<Product> productOptional = productService.getProductById(id);

        if (productOptional.isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("product", productOptional.get());
        model.addAttribute("allCategories", getAllCategories());

        return "admin_edit_product";
    }

    // --- Contact Message Management ---

    @GetMapping("/contacts")
    public String viewContactMessages(
            @AuthenticationPrincipal UserDetails userDetails, // Added for security check
            Model model, RedirectAttributes redirectAttributes) {

        // Run the security check first
        String securityRedirect = checkAdminCredentials(userDetails, model, redirectAttributes);
        if (securityRedirect != null) {
            return securityRedirect;
        }

        model.addAttribute("messages", contactService.getAllMessages());
        return "admin_contact_messages";
    }

    @PostMapping("/contact/delete/{id}")
    public String deleteContactMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contactService.deleteMessage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Message ID " + id + " deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting message: " + e.getMessage());
        }
        return "redirect:/admin/contacts";
    }

    @PostMapping("/product/update")
    public String updateProduct(@ModelAttribute("product") Product updatedProduct, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(updatedProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        String username = userDetails.getUsername();
        Optional<User> adminUserOptional = userService.findUserByUsername(username);
        String recoveryPhone = adminUserOptional.map(User::getRecoveryPhoneNumber).orElse("");

        if (!model.containsAttribute("forceUpdateMessage")) {
            boolean updated = userService.isAdminCredentialsUpdated(username);
            if (!updated && "admin".equals(username)) {
                model.addAttribute("forceUpdateMessage", "SECURITY ALERT: Please update your default login credentials immediately.");
            }
        }

        model.addAttribute("currentUsername", username);
        model.addAttribute("recoveryPhone", recoveryPhone);

        return "admin_profile";
    }

    @PostMapping("/updateProfile")
    public String updateAdminProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("recoveryPhoneNumber") String recoveryPhoneNumber,
            RedirectAttributes redirectAttributes) {

        String currentUsername = userDetails.getUsername();

        try {
            userService.updateAdminCredentials(currentUsername, newUsername, newPassword, recoveryPhoneNumber);

            redirectAttributes.addFlashAttribute("message", "Credentials updated successfully. Please log in with your new details.");
            return "redirect:/login?updated";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating credentials: An unexpected error occurred.");
            return "redirect:/admin/profile";
        }
    }
}
