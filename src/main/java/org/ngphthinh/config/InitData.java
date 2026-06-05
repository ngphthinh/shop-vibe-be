package org.ngphthinh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.ngphthinh.dto.request.user.UserCreateRequest;
import org.ngphthinh.entity.*;
import org.ngphthinh.enums.RoleName;
import org.ngphthinh.repository.CategoryRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.RoleRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Profile("dev")
@Slf4j
@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final Faker faker = new Faker(Locale.ENGLISH);
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    @Value("${app.default-password}")
    private String defaultPassword;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
        initUsers();
        initCategories();
        initProducts();


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
