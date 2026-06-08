package org.ngphthinh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.ngphthinh.dto.request.user.UserCreateRequest;
import org.ngphthinh.entity.*;
import org.ngphthinh.enums.PaymentStatus;
import org.ngphthinh.enums.RoleName;
import org.ngphthinh.repository.CategoryRepository;
import org.ngphthinh.repository.CartRepository;
import org.ngphthinh.repository.OrderItemRepository;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.RoleRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.service.AuthenticationService;
import org.ngphthinh.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Profile("dev")
@Slf4j
@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final Faker faker = new Faker(Locale.ENGLISH);
    private final CategoryRepository categoryRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    @Value("${app.default-password}")
    private String defaultPassword;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
        initCategories();
        initProducts();
        initCart();
        initOrders();


    }

    private void initOrders() {

        long orderCount = orderRepository.count();
        if (orderCount > 0) {
            log.info("Database has existing orders ({} records), skipping seeding orders.", orderCount);
            return;
        }

        List<User> allUsers = userRepository.findAll();
        List<Product> availableProducts = productRepository.findAll().stream()
                .filter(product -> !Boolean.TRUE.equals(product.getIsDeleted()))
                .toList();

        if (allUsers.isEmpty() || availableProducts.isEmpty()) {
            log.warn("No users or products found. Skipping orders seeding.");
            return;
        }

        log.info("=== START SEEDING ORDERS ===");

        // Phân bổ trạng thái đơn hàng
        List<OrderStatus> statusDistribution = new ArrayList<>();
        for (int i = 0; i < 10; i++) statusDistribution.add(OrderStatus.PENDING);
        for (int i = 0; i < 10; i++) statusDistribution.add(OrderStatus.CONFIRMED);
        for (int i = 0; i < 10; i++) statusDistribution.add(OrderStatus.SHIPPING);
        for (int i = 0; i < 15; i++) statusDistribution.add(OrderStatus.DELIVERED);
        for (int i = 0; i < 5; i++) statusDistribution.add(OrderStatus.CANCELLED);

        Collections.shuffle(statusDistribution);

        int totalOrders = 50;
        int userCount = allUsers.size();
        int productCount = availableProducts.size();

        for (int orderIndex = 0; orderIndex < totalOrders; orderIndex++) {
            User user = allUsers.get(orderIndex % userCount);

            // Tạo mã đơn hàng ORD-YYYYMMDD-XXXX
            String timestamp = String.format("%s%04d",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    orderIndex + 1);
            String orderCode = "ORD-" + timestamp + "-" + faker.random().hex(4).toUpperCase();

            OrderStatus status = statusDistribution.get(orderIndex);
            String shippingAddress = faker.address().fullAddress();
            String note = faker.buffy().quotes();

            // Tạo ngẫu nhiên danh sách order items (1 - 5 sản phẩm)
            int itemCount = faker.number().numberBetween(1, 6);
            Set<OrderItem> orderItems = new HashSet<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                Product product = availableProducts.get((orderIndex + itemIndex) % productCount);
                int quantity = faker.number().numberBetween(1, 5);
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                String productThumbnail = null;
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    productThumbnail = product.getImages().stream()
                            .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                            .findFirst()
                            .map(ProductImage::getImageUrl)
                            .orElse(null);
                }

                OrderItem orderItem = OrderItem.builder()
                        .product(product)
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .subtotal(subtotal)
                        .productName(product.getName())
                        .productThumbnail(productThumbnail)
                        .build();

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(subtotal);
            }

            // Sinh dữ liệu Payment tương ứng (Sửa lỗi mất kết quả khi JOIN API)
            PaymentStatus paymentStatus = (status == OrderStatus.DELIVERED) ? PaymentStatus.SUCCESS : PaymentStatus.PENDING;
            String paymentMethod = (orderIndex % 2 == 0) ? "COD" : "VNPAY";

            Payment payment = Payment.builder()
                    .amount(totalAmount)
                    .method(paymentMethod)
                    .status(paymentStatus)
                    .paidAt(paymentStatus == PaymentStatus.SUCCESS ? LocalDateTime.now() : null)
                    .build();

            // Khởi tạo thực thể Order hoàn chỉnh
            Order order = Order.builder()
                    .orderCode(orderCode)
                    .user(user)
                    .totalAmount(totalAmount)
                    .status(status)
                    .shippingAddress(shippingAddress)
                    .note(note)
                    .items(orderItems)
                    .payment(payment)
                    .build();

            payment.setOrder(order); // Thiết lập quan hệ 1-1 giữa Order và Payment
            // Liên kết quan hệ hai chiều cho từng Item
            orderItems.forEach(item -> item.setOrder(order));

            if (status == OrderStatus.CANCELLED) {
                order.setCancelReason(faker.lorem().sentence());
                order.setCancelledAt(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 10)));
            }

            orderRepository.save(order);
        }

        log.info("=== SEEDING ORDERS COMPLETED! TOTAL ORDERS IN DB: {} ===", orderRepository.count());
    }


    private void initCart() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found in the database. Skipping cart seeding.");
            return;
        }

        List<Product> availableProducts = productRepository.findAll().stream()
                .filter(product -> !Boolean.TRUE.equals(product.getIsDeleted()))
                .toList();

        if (availableProducts.isEmpty()) {
            log.warn("No products found in the database. Skipping cart item seeding.");
            return;
        }

        int seededCarts = 0;
        int productCount = availableProducts.size();

        for (int userIndex = 0; userIndex < users.size(); userIndex++) {
            User user = users.get(userIndex);
            Cart cart = user.getCart();

            // 1. Nếu User chưa có Cart, tạo mới và gắn mối quan hệ 2 chiều
            if (cart == null) {
                cart = Cart.builder()
                        .user(user)
                        .totalAmount(BigDecimal.ZERO)
                        .totalItems(0)
                        .items(new LinkedHashSet<>()) // Khởi tạo sẵn Set trống
                        .build();
                user.setCart(cart);
                // Không cần gọi userRepository.save(user) lẻ tẻ ở đây nữa,
                // @Transactional sẽ tự động nhận diện thay đổi và tạo Cart khi kết thúc hàm.
            }

            // 2. Kiểm tra an toàn: Nếu giỏ hàng đã có sẵn đồ rồi thì bỏ qua không seed trùng nữa
            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                continue;
            }

            int itemCount = Math.min(3, productCount);
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalItems = 0;

            // Tận dụng lại Set sẵn có của Cart thay vì tạo Set mới toanh đè lên
            if (cart.getItems() == null) {
                cart.setItems(new LinkedHashSet<>());
            }

            for (int i = 0; i < itemCount; i++) {
                Product product = availableProducts.get((userIndex + i) % productCount);
                int quantity = i + 1;
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                CartItem cartItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .subtotal(subtotal)
                        .build();

                // Thêm trực tiếp vào Set được Hibernate quản lý của Cart
                cart.getItems().add(cartItem);

                totalAmount = totalAmount.add(subtotal);
                totalItems += quantity;
            }

            cart.setTotalAmount(totalAmount);
            cart.setTotalItems(totalItems);

            // 3. Chỉ cần save cart là đủ (Đảm bảo trong Cart entity có cascade = CascadeType.ALL)
            cartRepository.save(cart);
            seededCarts++;
        }

        log.info("Seeded cart data for {} users.", seededCarts);
    }

    private void initProducts() {
        long count = productRepository.count();
        if (count > 0) {
            log.info("Database has existing products ({} records), skipping seeding products.", count);
            return;
        }

        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            log.warn("No categories found in the database. Please seed categories before seeding products.");
            return;
        }

        log.info("=== START SEEDING PRODUCTS  ===");

        int totalProductsToSeed = 105; // Chọn 105 để chia đều cho các danh mục dễ hơn
        int categoriesCount = allCategories.size();
        int productsPerCategory = (int) Math.ceil((double) totalProductsToSeed / categoriesCount);

        for (Category category : allCategories) {
            for (int i = 0; i < productsPerCategory; i++) {

                // 1. Sinh tên sản phẩm ngẫu nhiên theo ngành hàng thương mại
                String letter = faker.regexify("[A-Z]"); // Sinh ra chữ ngẫu nhiên: A, B, C...
                String productName = category.getName() + " " + faker.commerce().productName() + " " + letter + i;

                if (productName.length() > 290) {
                    productName = productName.substring(0, 290);
                }

                // 2. Tạo Slug duy nhất không trùng lặp
                String slug = productName.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "")
                        .replaceAll("\\s+", "-") + "-" + faker.random().hex(5);

                // 3. Sinh giá ngẫu nhiên (từ 100.000đ đến 5.000.000đ hoặc dạng USD)
                double randomPrice = faker.number().randomDouble(2, 10, 500); // Ví dụ hệ đô la hoặc tiền trăm ngàn
                BigDecimal price = BigDecimal.valueOf(randomPrice);

                // 4. Định cấu hình StockQuantity từ 0 đến 200 theo đúng đề bài
                int stockQuantity = faker.number().numberBetween(0, 201);

                // 5. Build đối tượng Product
                Product product = Product.builder()
                        .name(productName)
                        .slug(slug)
                        .description(faker.lorem().paragraph(3))
                        .price(price)
                        .stockQuantity(stockQuantity)
                        .category(category) // Thiết lập quan hệ ManyToOne
                        .isDeleted(false)
                        .build();

                // 6. Sinh từ 2 đến 4 ảnh cho từng sản phẩm theo yêu cầu
                int numberOfImages = faker.number().numberBetween(2, 5); // trả về 2, 3, hoặc 4
                Set<ProductImage> images = new HashSet<>();

                for (int j = 0; j < numberOfImages; j++) {
                    // Dùng link ảnh mock từ Unsplash hoặc LoremFlickr để có ảnh thật hiển thị lên UI
                    String mockImageUrl = "https://loremflickr.com/640/480/"
                            + category.getName().replaceAll("\\s+", "") + "?random=" + faker.random().nextInt(1, 1000);

                    ProductImage img = ProductImage.builder()
                            .imageUrl(mockImageUrl)
                            .isPrimary(j == 0) // Ảnh đầu tiên (j = 0) luôn là ảnh chính
                            .sortOrder(j)
                            .product(product) // Thiết lập quan hệ ngược lại với Product để JPA Map
                            .build();

                    images.add(img);
                }

                // Gán danh sách ảnh vào Product (do sử dụng CascadeType.ALL nên lưu product sẽ tự lưu toàn bộ ảnh)
                product.setImages(images);

                // 7. Lưu xuống DB
                productRepository.save(product);
            }
        }

        log.info("=== SEEDING PRODUCTS COMPLETED! TOTAL PRODUCTS IN DB: {} ===", productRepository.count());
    }

    private void initUsers() {
        long count = userRepository.count();
        if (count > 0) {
            log.info("Database has existing users ({} records), skipping seeding users.", count);
            return;
        }

        log.info("=== START SEEDING USERS ===");

//        1 admin (admin1 admin (admin@shopvibe.vn), 9 user thường. Password: Test@12345@

        List<Role> adminRole = List.of(roleRepository.findById(RoleName.ROLE_ADMIN.name()).orElseThrow());

        // Create admin user
        User adminUser = User.builder()
                .email("admin@shopvibe.vn")
                .password(passwordEncoder.encode(defaultPassword))
                .fullName("Admin One")
                .phone(faker.phoneNumber().cellPhone())
                .roles(new HashSet<>(adminRole))
                .build();

        adminUser.setCart(Cart.builder()
                .user(adminUser)
                .build());

        userRepository.save(adminUser);
        log.warn("Admin user created with email:{} and password: {}, Change it immediately!", adminUser.getEmail(), defaultPassword);
        // Create 9 regular users
        for (int i = 1; i <= 9; i++) {
            String email = String.format("user%d@shopvibe.vn", i);
            String name = faker.name().fullName();
            String phone = faker.phoneNumber().cellPhone();
            authenticationService.register(UserCreateRequest.builder()
                    .phone(phone)
                    .email(email)
                    .fullName(name)
                    .password(defaultPassword)
                    .build());
        }

    }

    private void initCategories() {
        long count = categoryRepository.count();
        if (count > 0) {
            log.info("Database has existing categories ({} records), skipping seeding categories.", count);
            return;
        }

        log.info("=== START SEEDING CATEGORIES ===");

        // init root categories (Parent categories) - the main categories that will have sub-categories nested under them
        String[] rootCategories = {"Thời trang", "Điện tử", "Mỹ phẩm", "Thể thao", "Gia dụng"};
        List<Category> savedParents = new ArrayList<>();

        for (String parentName : rootCategories) {
            Category parent = Category.builder()
                    .name(parentName)
                    .parentCategory(null)
                    .build();
            savedParents.add(categoryRepository.save(parent));
        }

        // init sub-categories (Child categories) - these will be associated with the parent categories created above
        for (Category parent : savedParents) {
            switch (parent.getName()) {
                case "Thời trang":
                    String[] fashionSubs = {"Thời trang Nam", "Thời trang Nữ", "Thời trang Trẻ em", "Phụ kiện cao cấp", "Giày & Balo túi xách"};
                    createSubCategories(fashionSubs, parent);
                    break;

                case "Điện tử":
                    for (int i = 0; i < 4; i++) {
                        String finalName = faker.options().option("Điện thoại ", "Máy tính ", "Phụ kiện ")
                                + faker.brand().car() + " " + faker.lorem().word().toUpperCase();
                        saveSub(finalName, parent);
                    }
                    break;

                case "Mỹ phẩm":
                    String[] cosmeticKeywords = {"Son môi", "Kem dưỡng da", "Nước hoa", "Sữa rửa mặt", "Mặt nạ trị mụn"};
                    for (String keyword : cosmeticKeywords) {
                        String subName = keyword + " " + faker.commerce().productName().split(" ")[0];
                        saveSub(subName, parent);
                    }
                    break;

                case "Thể thao":
                    String[] sportKeywords = {"Bóng đá", "Cầu lông", "Gym & Yoga", "Chạy bộ", "Bơi lội"};
                    for (String keyword : sportKeywords) {
                        String subName = "Dụng cụ " + keyword + " " + faker.options().option("Chính hãng", "Cao cấp", "Bán chạy");
                        saveSub(subName, parent);
                    }
                    break;

                default:
                    for (int i = 0; i < 4; i++) {
                        String subName = "Đồ gia dụng bằng " + faker.commerce().material();
                        saveSub(subName, parent);
                    }
                    break;
            }
        }

        log.info("=== FINISHED SEEDING CATEGORIES: {} records ===", categoryRepository.count());

    }

    private void createSubCategories(String[] subNames, Category parent) {
        for (String subName : subNames) {
            saveSub(subName, parent);
        }
    }


    private void saveSub(String name, Category parent) {
        Category subCategory = Category.builder()
                .name(name)
                .parentCategory(parent)
                .build();
        categoryRepository.save(subCategory);
    }

    private void initRoles() {
        List<Role> roles = List.of(Role.builder()
                        .id(RoleName.ROLE_ADMIN.name())
                        .name(RoleName.ROLE_ADMIN)
                        .build(),
                Role.builder()
                        .id(RoleName.ROLE_USER.name())
                        .name(RoleName.ROLE_USER)
                        .build()
        );


        if (roleRepository.count() == 0) {
            roleRepository.saveAll(roles);
            log.info("Initialized roles: {}", roles);
        }
    }
}
