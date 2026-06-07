package org.ngphthinh.repository;

import org.ngphthinh.entity.Product;
import org.ngphthinh.repository.projection.ProductProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            
                    (SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = p.id) AS averageRating, 
            
                    (SELECT COUNT(r.id) FROM Review r WHERE r.product.id = p.id) AS reviewCount, 
            
                    (SELECT pi.imageUrl FROM ProductImage pi 
                     WHERE pi.product.id = p.id AND pi.isPrimary = true) AS imageUrl
            
                FROM Product p 
                LEFT JOIN p.category c 
                WHERE p.isDeleted = false
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
                WHERE p.isDeleted = false and (lower(p.name) like lower(concat('%', :keyword, '%')) or lower(p.description) like lower(concat('%', :keyword, '%')))
            """)
    Page<ProductProjection> findProductsByKeyword(String keyword, Pageable pageable);

    boolean existsByCategoryIdAndIsDeletedFalse(Long categoryId);

    Optional<Product> findByIdAndIsDeletedFalse(Long id);
}
