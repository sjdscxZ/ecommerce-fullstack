package com.sjdscxz.ecommerce;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@SpringBootApplication
public class EcommerceApplication {
    public static void main(String[] args) { SpringApplication.run(EcommerceApplication.class, args); }
}

@Entity @Table(name = "products")
class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) String name;
    @Size(max = 2000) @Column(length = 2000) String description;
    @NotNull @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 2) BigDecimal price;
    @NotNull @Min(0) @Column(nullable = false) Integer stock = 0;
    @Column(length = 500) String imageUrl;

    protected Product() {}
    Product(String name, String description, BigDecimal price, Integer stock, String imageUrl) {
        this.name = name; this.description = description; this.price = price; this.stock = stock; this.imageUrl = imageUrl;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getImageUrl() { return imageUrl; }
    public void setStock(Integer stock) { this.stock = stock; }
}

@Entity @Table(name = "customer_orders")
class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 80) String customerEmail;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal total;
    @Column(nullable = false, length = 20) String status = "PLACED";
    @Column(nullable = false, updatable = false) Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {}
    CustomerOrder(String email, BigDecimal total) { this.customerEmail = email; this.total = total; }
    public Long getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return lines; }
}

@Entity @Table(name = "order_lines")
class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id") CustomerOrder order;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id") Product product;
    @Column(nullable = false) Integer quantity;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal unitPrice;

    protected OrderLine() {}
    OrderLine(CustomerOrder order, Product product, Integer qty, BigDecimal unitPrice) {
        this.order = order; this.product = product; this.quantity = qty; this.unitPrice = unitPrice;
    }
    public Long getId() { return id; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Long getProductId() { return product != null ? product.getId() : null; }
    public String getProductName() { return product != null ? product.getName() : null; }
}

interface ProductRepository extends JpaRepository<Product, Long> {}
interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerEmailOrderByCreatedAtDesc(String email);
}

@RestController @RequestMapping("/api/products")
class ProductController {
    private final ProductRepository repo;
    ProductController(ProductRepository repo) { this.repo = repo; }

    @GetMapping public List<Product> list() { return repo.findAll(); }
    @GetMapping("/{id}") public ResponseEntity<Product> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping public ResponseEntity<Product> create(@Valid @RequestBody Product p) {
        Product saved = repo.save(p);
        return ResponseEntity.created(URI.create("/api/products/" + saved.id)).body(saved);
    }
}

@RestController @RequestMapping("/api/orders")
class OrderController {
    private final ProductRepository products;
    private final OrderRepository orders;
    OrderController(ProductRepository products, OrderRepository orders) {
        this.products = products; this.orders = orders;
    }

    record CheckoutItem(@NotNull Long productId, @Min(1) Integer quantity) {}
    record CheckoutRequest(@Email @NotBlank String email, @NotEmpty List<CheckoutItem> items) {}

    @PostMapping("/checkout")
    public ResponseEntity<CustomerOrder> checkout(@Valid @RequestBody CheckoutRequest req) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderLine> lines = new ArrayList<>();
        for (CheckoutItem ci : req.items()) {
            Product p = products.findById(ci.productId()).orElseThrow(
                    () -> new RuntimeException("product not found: " + ci.productId()));
            if (p.getStock() < ci.quantity()) {
                return ResponseEntity.status(409).build();
            }
            p.setStock(p.getStock() - ci.quantity());
            products.save(p);
            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ci.quantity()));
            total = total.add(lineTotal);
            lines.add(new OrderLine(null, p, ci.quantity(), p.getPrice()));
        }
        CustomerOrder order = new CustomerOrder(req.email(), total);
        for (OrderLine l : lines) { l.order = order; order.getLines().add(l); }
        return ResponseEntity.ok(orders.save(order));
    }

    @GetMapping("/by-email")
    public List<CustomerOrder> byEmail(@RequestParam String email) {
        return orders.findByCustomerEmailOrderByCreatedAtDesc(email);
    }
}

@Configuration
class SecurityConfig implements WebMvcConfigurer {
    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
    @Override
    public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/**").allowedOrigins("http://localhost:4200")
                .allowedMethods("GET","POST","PUT","DELETE");
    }
}
