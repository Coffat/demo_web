package com.example.demo.service;

import com.example.demo.entity.Catalog;
import com.example.demo.entity.Product;
import com.example.demo.entity.enums.ProductStatus;
import com.example.demo.dto.ProductDetailDTO;
import com.example.demo.repository.CatalogRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Product Service for handling product business logic
 * Following rules.mdc specifications for business tier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final CatalogRepository catalogRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Find all products with pagination
     * @param pageable Pagination information
     * @return Page of products
     */
    public Page<Product> findAll(Pageable pageable) {
        log.info("Fetching products with pagination: page {}, size {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable);
    }

    /**
     * Find product by ID
     * @param id Product ID
     * @return Product if found, null otherwise
     */
    public Product findById(Long id) {
        if (id == null) {
            log.warn("Product ID is null");
            return null;
        }
        
        log.info("Fetching product with ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        
        if (product.isEmpty()) {
            log.warn("Product not found with ID: {}", id);
            return null;
        }
        
        return product.get();
    }

    /**
     * Search products by keyword with pagination
     * @param keyword Search keyword
     * @param pageable Pagination information
     * @return Page of matching products
     */
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.info("Empty search keyword, returning all products");
            return findAll(pageable);
        }
        
        String cleanKeyword = keyword.trim();
        log.info("Searching products with keyword: '{}', page: {}, size: {}", 
                cleanKeyword, pageable.getPageNumber(), pageable.getPageSize());
        
        return productRepository.searchProducts(cleanKeyword, pageable);
    }

    /**
     * Find products by name containing keyword with pagination
     * @param name Name keyword
     * @param pageable Pagination information
     * @return Page of matching products
     */
    public Page<Product> findByNameContaining(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) {
            return findAll(pageable);
        }
        
        log.info("Finding products by name containing: '{}'", name.trim());
        return productRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
    }

    /**
     * Find products by price range
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @return List of products in price range
     */
    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = new BigDecimal("999999999");
        
        log.info("Finding products in price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Get latest products
     * @param limit Maximum number of products to return
     * @return List of latest products
     */
    public List<Product> getLatestProducts(int limit) {
        log.info("Fetching {} latest products", limit);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository.findLatestProducts(pageable);
    }

    /**
     * Get best selling products
     * @param limit Maximum number of products to return
     * @return List of best selling products
     */
    public List<Product> getBestSellingProducts(int limit) {
        log.info("Fetching {} best selling products", limit);
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return productRepository.findBestSellingProducts(pageable);
        } catch (Exception e) {
            log.warn("Error fetching best selling products, falling back to latest products: {}", e.getMessage());
            // Fallback to latest products if best selling query fails
            return getLatestProducts(limit);
        }
    }

    /**
     * Get featured products for homepage
     * @return List of featured products
     */
    public List<Product> getFeaturedProducts() {
        log.info("Fetching featured products");
        // Return latest products as featured for now
        return getLatestProducts(8);
    }

    /**
     * Get products with reviews loaded
     * @return List of products with reviews
     */
    public List<Product> getProductsWithReviews() {
        log.info("Fetching products with reviews");
        return productRepository.findAllWithReviews();
    }

    /**
     * Get product with average rating and review count using optimized query
     * @param productId Product ID
     * @return ProductDetailDTO with rating information
     */
    public ProductDetailDTO getProductWithRating(Long productId) {
        if (productId == null) {
            log.warn("Product ID is null");
            return null;
        }
        
        log.info("Fetching product detail with rating for ID: {}", productId);
        return productRepository.findProductDetailById(productId).orElse(null);
    }

    /**
     * Get product with average rating and review count (legacy method for backward compatibility)
     * @param productId Product ID
     * @return ProductWithRating with rating information
     */
    public ProductWithRating getProductWithRatingLegacy(Long productId) {
        Product product = findById(productId);
        if (product == null) {
            return null;
        }
        
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countReviewsByProductId(productId);
        
        return new ProductWithRating(product, 
                averageRating != null ? averageRating : 0.0, 
                reviewCount != null ? reviewCount : 0L);
    }

    /**
     * Get products sorted by different criteria
     * @param sortBy Sort criteria (name, price, rating, newest)
     * @param direction Sort direction (asc, desc)
     * @param pageable Pagination information
     * @return Sorted page of products
     */
    public Page<Product> getProductsSorted(String sortBy, String direction, Pageable pageable) {
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Sort sort;
        switch (sortBy != null ? sortBy.toLowerCase() : "newest") {
            case "name":
                sort = Sort.by(sortDirection, "name");
                break;
            case "price":
                sort = Sort.by(sortDirection, "price");
                break;
            case "oldest":
                sort = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            case "newest":
            default:
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
        );
        
        log.info("Getting products sorted by: {} {}", sortBy, direction);
        return productRepository.findAll(sortedPageable);
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Get all products with pagination (Admin)
     */
    public Page<Product> getAllProducts(Pageable pageable) {
        log.info("Admin: Fetching all products with pagination");
        return productRepository.findAll(pageable);
    }

    /**
     * Get product by ID (Admin)
     */
    public Optional<Product> getProductById(Long productId) {
        log.info("Admin: Fetching product by ID: {}", productId);
        return productRepository.findByIdWithCatalogEager(productId);
    }
    
    /**
     * Get product by ID with rating info (Admin)
     */
    public Map<String, Object> getProductByIdWithRating(Long productId) {
        log.info("Admin: Fetching product by ID with rating: {}", productId);
        
        Optional<Product> productOpt = productRepository.findByIdWithCatalogEager(productId);
        if (productOpt.isEmpty()) {
            return null;
        }
        
        Product product = productOpt.get();
        Double averageRating = productRepository.getAverageRatingByProductId(productId);
        Long reviewCount = productRepository.getReviewCountByProductId(productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("averageRating", averageRating != null ? averageRating : 0.0);
        result.put("reviewCount", reviewCount != null ? reviewCount : 0L);
        
        return result;
    }

    /**
     * Get products by status (Admin)
     */
    public Page<Product> getProductsByStatus(ProductStatus status, Pageable pageable) {
        log.info("Admin: Fetching products by status: {}", status);
        return productRepository.findByStatus(status, pageable);
    }

    /**
     * Get product statistics for admin dashboard
     */
    public Map<String, Object> getProductStatistics() {
        log.info("Admin: Calculating product statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total products
        long totalProducts = productRepository.count();
        stats.put("totalProducts", totalProducts);
        
        // Active products
        long activeProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        stats.put("activeProducts", activeProducts);
        
        // Out of stock products
        long outOfStockProducts = productRepository.countByStatus(ProductStatus.OUT_OF_STOCK);
        stats.put("outOfStockProducts", outOfStockProducts);
        
        // Low stock products (less than 10)
        long lowStockProducts = productRepository.countByStockQuantityLessThan(10);
        stats.put("lowStockProducts", lowStockProducts);
        
        return stats;
    }

    /**
     * Create new product (Admin)
     */
    @Transactional
    public Product createProduct(String name, String description, BigDecimal price, 
                               Integer stockQuantity, ProductStatus status, 
                               MultipartFile image, Integer weightG, 
                               Integer lengthCm, Integer widthCm, Integer heightCm,
                               Long catalogId) {
        
        log.info("Admin: Creating new product: {}", name);
        
        // Validate price constraint (NUMERIC(10,2) max value: 99,999,999.99)
        if (price != null && price.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được vượt quá 99,999,999₫");
        }
        
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setStatus(status);
        product.setWeightG(weightG);
        product.setLengthCm(lengthCm);
        product.setWidthCm(widthCm);
        product.setHeightCm(heightCm);
        
        // Set catalog
        if (catalogId != null) {
            Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog not found with id: " + catalogId));
            product.setCatalog(catalog);
        }
        
        // Handle image upload
        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = saveImage(image);
                product.setImage(imagePath);
            } catch (IOException e) {
                log.error("Failed to save image for product: {}", name, e);
                // Continue without image
            }
        }
        
        Product savedProduct = productRepository.save(product);
        log.info("Admin: Successfully created product with ID: {}", savedProduct.getId());
        
        return savedProduct;
    }

    /**
     * Update product (Admin)
     */
    @Transactional
    public Product updateProduct(Long productId, String name, String description, 
                               BigDecimal price, Integer stockQuantity, ProductStatus status,
                               MultipartFile image, Integer weightG, 
                               Integer lengthCm, Integer widthCm, Integer heightCm,
                               Long catalogId) {
        
        log.info("Admin: Updating product ID: {}", productId);
        
        // Validate price constraint (NUMERIC(10,2) max value: 99,999,999.99)
        if (price != null && price.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được vượt quá 99,999,999₫");
        }
        
        // Use eager fetch to avoid lazy loading issues
        Product product = productRepository.findByIdWithCatalogEager(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setStatus(status);
        product.setWeightG(weightG);
        product.setLengthCm(lengthCm);
        product.setWidthCm(widthCm);
        product.setHeightCm(heightCm);
        
        // Update catalog
        if (catalogId != null) {
            Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog not found with id: " + catalogId));
            product.setCatalog(catalog);
        } else {
            product.setCatalog(null);
        }
        
        // Handle image upload
        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = saveImage(image);
                product.setImage(imagePath);
            } catch (IOException e) {
                log.error("Failed to save image for product: {}", name, e);
                // Continue with existing image
            }
        }
        
        Product updatedProduct = productRepository.save(product);
        log.info("Admin: Successfully updated product ID: {}", productId);
        
        return updatedProduct;
    }

    /**
     * Update product status (Admin)
     */
    @Transactional
    public Product updateProductStatus(Long productId, ProductStatus status) {
        log.info("Admin: Updating product status for ID: {} to {}", productId, status);
        
        // Use eager fetch to avoid lazy loading issues
        Product product = productRepository.findByIdWithCatalogEager(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        
        product.setStatus(status);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Admin: Successfully updated product status for ID: {}", productId);
        return updatedProduct;
    }

    /**
     * Update product stock (Admin)
     */
    @Transactional
    public Product updateProductStock(Long productId, Integer stockQuantity) {
        log.info("Admin: Updating product stock for ID: {} to {}", productId, stockQuantity);
        
        // Use eager fetch to avoid lazy loading issues
        Product product = productRepository.findByIdWithCatalogEager(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        
        product.setStockQuantity(stockQuantity);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Admin: Successfully updated product stock for ID: {}", productId);
        return updatedProduct;
    }

    /**
     * Delete product (Admin)
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("Admin: Deleting product ID: {}", productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
        
        productRepository.delete(product);
        log.info("Admin: Successfully deleted product ID: {}", productId);
    }

    /**
     * Find products by catalog
     * @param catalogId Catalog ID
     * @param pageable Pagination information
     * @return Page of products in the catalog
     */
    public Page<Product> findByCatalog(Long catalogId, Pageable pageable) {
        log.info("Fetching products by catalog ID: {}", catalogId);
        return productRepository.findByCatalogId(catalogId, pageable);
    }

    /**
     * Combined filter: Find products by catalog AND search keyword
     * @param catalogId Catalog ID (can be null)
     * @param search Search keyword (can be null or empty)
     * @param pageable Pagination information
     * @return Page of products matching both filters
     */
    @Transactional(readOnly = true)
    public Page<Product> findByCatalogAndSearch(Long catalogId, String search, Pageable pageable) {
        String keyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        log.info("Combined search: categoryId={}, keyword='{}'", catalogId, keyword);
        
        // Ensure we pass NULL if categoryId is 0 or null
        Long finalCatalogId = (catalogId != null && catalogId > 0) ? catalogId : null;
        
        return productRepository.findByCatalogIdAndNameContainingIgnoreCase(finalCatalogId, keyword, pageable);
    }

    /**
     * Save uploaded image file
     */
    private String saveImage(MultipartFile image) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        
        String filename = "product_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return "/uploads/" + filename;
    }

    /**
     * Search products for AI chat
     * Returns simplified product suggestions with essential info
     */
    public List<com.example.demo.dto.ProductSuggestionDTO> searchForAi(String query, BigDecimal maxPrice, Integer limit) {
        log.info("AI product search: query='{}', maxPrice={}, limit={}", query, maxPrice, limit);
        
        try {
            int searchLimit = limit != null ? limit : 3;
            Pageable pageable = PageRequest.of(0, searchLimit * 3, Sort.by(Sort.Direction.DESC, "createdAt")); // Get more to filter
            
            Page<Product> products;
            
            if (query == null || query.trim().isEmpty()) {
                // No query - return latest products
                products = productRepository.findAll(pageable);
            } else {
                // Normalize search query - extract key words
                String normalizedQuery = normalizeSearchQuery(query.trim());
                log.info("Normalized search query: '{}' -> '{}'", query, normalizedQuery);
                
                // Use enhanced search that includes name, description, and catalog
                products = productRepository.searchProductsForAi(normalizedQuery, pageable);
            }
            
            List<com.example.demo.dto.ProductSuggestionDTO> suggestions = new ArrayList<>();
            
            for (Product product : products) {
                // Filter by price if specified
                if (maxPrice != null && product.getPrice().compareTo(maxPrice) > 0) {
                    continue;
                }
                
                // Only include available products
                if (product.getStatus() != ProductStatus.ACTIVE || !product.isInStock()) {
                    continue;
                }
                
                com.example.demo.dto.ProductSuggestionDTO suggestion = new com.example.demo.dto.ProductSuggestionDTO();
                suggestion.setId(product.getId());
                suggestion.setName(product.getName());
                suggestion.setDescription(product.getDescription());
                suggestion.setPrice(product.getPrice());
                suggestion.setImageUrl(product.getImage());
                suggestion.setProductUrl("/products/" + product.getId());
                suggestion.setStockQuantity(product.getStockQuantity());
                suggestion.setAvailable(product.isAvailable());
                
                if (product.getCatalog() != null) {
                    suggestion.setCatalogName(product.getCatalog().getValue());
                }
                
                suggestions.add(suggestion);
                
                // Stop if we have enough
                if (suggestions.size() >= searchLimit) {
                    break;
                }
            }
            
            log.info("Found {} product suggestions for AI", suggestions.size());
            return suggestions;
            
        } catch (Exception e) {
            log.error("Error in AI product search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Normalize search query for better matching
     * Extract key flower names and types
     */
    private String normalizeSearchQuery(String query) {
        String normalized = query.toLowerCase();
        
        // Map common phrases to key product terms
        if (normalized.contains("người yêu") || normalized.contains("tình nhân") || normalized.contains("valentine")) {
            return "hồng"; // Most romantic flower
        }
        if (normalized.contains("sinh nhật") || normalized.contains("chúc mừng")) {
            return ""; // Return all for birthday/celebration
        }
        if (normalized.contains("chia buồn") || normalized.contains("tang lễ")) {
            return "ly"; // Funeral flowers
        }
        
        // Extract flower names
        String[] flowerKeywords = {"hồng", "ly", "tulip", "hướng dương", "cẩm chướng", 
                                   "lan", "peony", "baby", "cúc", "salem", "cẩm tú cầu"};
        for (String keyword : flowerKeywords) {
            if (normalized.contains(keyword)) {
                return keyword;
            }
        }
        
        // Extract color if mentioned
        String[] colors = {"đỏ", "trắng", "hồng", "vàng", "tím", "xanh", "cam"};
        for (String color : colors) {
            if (normalized.contains(color)) {
                return color;
            }
        }
        
        // If nothing specific, return original
        return query;
    }

    /**
     * Inner class for product with rating information
     */
    public static class ProductWithRating {
        private final Product product;
        private final Double averageRating;
        private final Long reviewCount;

        public ProductWithRating(Product product, Double averageRating, Long reviewCount) {
            this.product = product;
            this.averageRating = averageRating;
            this.reviewCount = reviewCount;
        }

        public Product getProduct() { return product; }
        public Double getAverageRating() { return averageRating; }
        public Long getReviewCount() { return reviewCount; }
    }
}
