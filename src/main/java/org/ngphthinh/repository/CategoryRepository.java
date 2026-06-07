package org.ngphthinh.repository;

import org.ngphthinh.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {
            "subCategories",
            "subCategories.subCategories"
    })
    List<Category> findByParentCategoryIsNull();

    @EntityGraph(attributePaths = {
            "subCategories",
            "subCategories.subCategories"
    })
    Optional<Category> findById(Long id);

    boolean existsByIdAndProductsIsEmpty(Long id);

    boolean existsByIdAndSubCategoriesIsEmpty(Long id);

    Long findParentIdById(Long current);
}

