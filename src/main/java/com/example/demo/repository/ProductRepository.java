package com.example.demo.repository;

import com.example.demo.entity.Product;
import com.example.demo.entity.enums.ProductStatus;
import com.example.demo.dto.ProductDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

       Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

       List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

       @Query("SELECT p FROM Product p LEFT JOIN p.catalog c WHERE " +
                     "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(c.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

       // Enhanced search for AI - includes catalog
       @Query("SELECT p FROM Product p LEFT JOIN p.catalog c WHERE " +
                     "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(c.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Product> searchProductsForAi(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews")
       List<Product> findAllWithReviews();

       @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
       List<Product> findLatestProducts(Pageable pageable);

       @Query("SELECT p FROM Product p LEFT JOIN p.orderItems oi GROUP BY p ORDER BY COUNT(oi) DESC")
       List<Product> findBestSellingProducts(Pageable pageable);

       // Query by catalog
       List<Product> findByCatalogId(Long catalogId);

       Page<Product> findByCatalogId(Long catalogId, Pageable pageable);

       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.catalog WHERE p.id = :productId")
       Optional<Product> findByIdWithCatalog(@Param("productId") Long productId);

       @Query("SELECT new com.example.demo.dto.ProductDetailDTO(p, AVG(r.rating), COUNT(r)) " +
                     "FROM Product p LEFT JOIN p.reviews r WHERE p.id = :productId GROUP BY p")
       Optional<ProductDetailDTO> findProductDetailById(@Param("productId") Long productId);

       // Admin methods
       Page<Product> findByStatus(ProductStatus status, Pageable pageable);

       @Query("SELECT COUNT(p) FROM Product p WHERE CAST(p.status AS string) = CAST(:status AS string)")
       long countByStatus(@Param("status") ProductStatus status);

       @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity < :threshold")
       long countByStockQuantityLessThan(@Param("threshold") Integer threshold);

       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id = :productId")
       Optional<Product> findByIdWithReviews(@Param("productId") Long productId);

       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.catalog WHERE p.id = :productId")
       Optional<Product> findByIdWithCatalogEager(@Param("productId") Long productId);

       @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
       Double getAverageRatingByProductId(@Param("productId") Long productId);

       @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
       Long getReviewCountByProductId(@Param("productId") Long productId);

       // Combined filter: Catalog ID AND Search keyword
       @Query("SELECT p FROM Product p WHERE " +
                     "(:catalogId IS NULL OR p.catalog.id = :catalogId) AND " +
                     "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")

       Page<Product> findByCatalogIdAndNameContainingIgnoreCase(
               @Param("catalogId") Long catalogId, 
               @Param("keyword") String keyword, 
               Pageable pageable);
}
