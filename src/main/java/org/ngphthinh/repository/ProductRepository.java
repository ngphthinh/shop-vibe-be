package org.ngphthinh.repository;

import org.ngphthinh.entity.Category;
import org.ngphthinh.entity.Product;
import org.ngphthinh.repository.projection.ProductProjection;
import org.ngphthinh.repository.projection.ProductRankItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
                SELECT 
                    p.id AS id, 
                    p.name AS name, 
                    p.slug AS slug, 
                    p.description AS description, 
                    p.price AS price, 
                    p.stockQuantity AS stockQuantity, 
                    c.id AS categoryId, 
                    c.name AS categoryName, 
                    COALESCE(AVG(r.rating), 0.0) AS averageRating, 
                    COUNT(DISTINCT r.id) AS reviewCount, 
                    pi.imageUrl AS imageUrl
                FROM Product p 
                LEFT JOIN p.category c 
                LEFT JOIN Review r ON r.product.id = p.id
                LEFT JOIN ProductImage pi ON pi.product.id = p.id AND pi.isPrimary = true
                WHERE p.isDeleted = false
                GROUP BY p.id, p.name, p.slug, p.description, p.price, p.stockQuantity, c.id, c.name, pi.imageUrl
            """)
    Page<ProductProjection> findAllProducts(Pageable pageable);

    @Query("""
                SELECT 
                    p.id AS id, 
                    p.name AS name, 
                    p.slug AS slug, 
                    p.description AS description, 
                    p.price AS price, 
                    p.stockQuantity AS stockQuantity, 
                    c.id AS categoryId, 
                    c.name AS categoryName,
                    (SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = p.id) AS averageRating, 
                    (SELECT COUNT(r.id) FROM Review r WHERE r.product.id = p.id) AS reviewCount, 
                    p.createdAt AS createdAt,
            
                    (SELECT pi.imageUrl FROM ProductImage pi 
                     WHERE pi.product.id = p.id AND pi.isPrimary = true) AS imageUrl
            
                FROM Product p 
                LEFT JOIN p.category c 
                WHERE p.isDeleted = false AND p.id = :id
            """)
    Optional<ProductProjection> findByIdWithProjection(@Param("id") Long id);


    @Query("""
                SELECT
                    p.id AS id,
                    p.name AS name,
                    p.slug AS slug,
                    p.description AS description, 
                    p.price AS price, 
                    p.stockQuantity AS stockQuantity, 
                    c.id AS categoryId, 
                    c.name AS categoryName,
            
                    (SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = p.id) AS averageRating, 
            
                    (SELECT COUNT(r.id) FROM Review r WHERE r.product.id = p.id) AS reviewCount, 
            
                    (SELECT pi.imageUrl FROM ProductImage pi 
                     WHERE pi.product.id = p.id AND pi.isPrimary = true) AS imageUrl
            
                FROM Product p 
                LEFT JOIN p.category c 
                WHERE p.isDeleted = false and (lower(p.name) like lower(concat('%', :keyword, '%')) or lower(p.description) like lower(concat('%', :keyword, '%'))) AND (:categoryId IS NULL OR p.category.id = :categoryId)
            """)
    Page<ProductProjection> findProductsByKeyword(String keyword, Long categoryId, Pageable pageable);

    boolean existsByCategoryIdAndIsDeletedFalse(Long categoryId);

    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    boolean existsByIdAndIsDeletedFalse(Long id);

    @Query("""
            SELECT
                   ROW_NUMBER() OVER (ORDER BY SUM(oi.quantity) DESC) as rank,
                   p.id as productId,
                   p.name as productName,
                   SUM(oi.quantity) as totalQuantitySold,
                   SUM(oi.subtotal) as totalRevenue,
                   (Select pi.imageUrl FROM ProductImage pi WHERE pi.product.id = p.id AND pi.isPrimary = true) as thumbnail
            FROM Order o
            JOIN o.items oi
            JOIN oi.product p
            GROUP BY p.id, p.name
            ORDER BY totalQuantitySold DESC, totalRevenue DESC
            """)
    List<ProductRankItemProjection> findTopProducts(Pageable pageable);

}
