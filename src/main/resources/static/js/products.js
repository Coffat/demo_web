/**
 * Products Page JavaScript
 * Professional interactions for product listing and detail pages
 * Following rules.mdc specifications for client-side functionality
 */

(function () {
  "use strict";

  // ================================
  // GLOBAL VARIABLES & CONFIG
  // ================================

  const CONFIG = {
    DEBOUNCE_DELAY: 300,
    ANIMATION_DURATION: 300,
    TOAST_DURATION: 3000,
    MAX_QUANTITY: 99,
    MIN_QUANTITY: 1,
  };

  let searchTimeout = null;
  let isLoading = false;

  // ================================
  // UTILITY FUNCTIONS
  // ================================

  function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // Helper function to safely set class on icon (SVG or regular element)
  function setIconClass(iconElement, className) {
    if (!iconElement) return;

    try {
      if (iconElement.tagName === "SVG") {
        iconElement.setAttribute("class", className);
      } else {
        iconElement.className = className;
      }
    } catch (error) {
      console.warn("Error setting icon class:", error);
      // Fallback: try setAttribute for all elements
      try {
        iconElement.setAttribute("class", className);
      } catch (fallbackError) {
        console.error(
          "Failed to set class even with setAttribute:",
          fallbackError
        );
      }
    }
  }

  // Helper function to safely get class from icon
  function getIconClass(iconElement) {
    if (!iconElement) return "";

    if (iconElement.tagName === "SVG") {
      return iconElement.getAttribute("class") || "";
    } else {
      return iconElement.className || "";
    }
  }

  function setLoadingState(element, loading = true) {
    if (loading) {
      element.dataset.originalContent = element.innerHTML;
      element.innerHTML =
        '<svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clip-rule="evenodd" /></svg> Đang xử lý...';
      element.disabled = true;
    } else {
      element.innerHTML = element.dataset.originalContent || element.innerHTML;
      element.disabled = false;
      delete element.dataset.originalContent;
    }
  }

  function formatCurrency(amount) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  }

  function validateQuantity(value) {
    const quantity = parseInt(value) || CONFIG.MIN_QUANTITY;
    return Math.min(
      Math.max(quantity, CONFIG.MIN_QUANTITY),
      CONFIG.MAX_QUANTITY
    );
  }

  // (Search is initialized globally in main.js)

  // ================================
  // PRODUCT GRID FUNCTIONALITY
  // ================================

  function initializeProductGrid() {
    // View toggle functionality
    initializeViewToggle();

    // Product actions - Event delegation for wishlist and cart
    initializeProductActions();

    // Tối ưu hóa hiệu ứng AOS cho lưới sản phẩm
    const productCards = document.querySelectorAll(".product-card");
    productCards.forEach((card, index) => {
      // Gán độ trễ tăng dần cho mỗi sản phẩm (50ms, 100ms, 150ms...)
      // Điều này giúp hiệu ứng xuất hiện nối tiếp nhau một cách mượt mà
      card.setAttribute("data-aos-delay", (index % 10) * 100);
    });
  }
  
  function initializeProductActions() {
    // Event delegation is handled by main.js
    // No need to duplicate here - main.js already handles .btn-wishlist and .btn-add-to-cart
    console.log('Product actions: Using event delegation from main.js');
  }

  function initializeViewToggle() {
    const viewToggleButtons = document.querySelectorAll(".btn-view-toggle");
    const productsGrid = document.getElementById("productsGrid");

    if (!viewToggleButtons.length || !productsGrid) return;

    viewToggleButtons.forEach((button) => {
      button.addEventListener("click", function () {
        const view = this.dataset.view;

        // Update active state
        viewToggleButtons.forEach((btn) => btn.classList.remove("active"));
        this.classList.add("active");

        // Update grid class with animation
        if (view === "list") {
          productsGrid.style.opacity = "1";
          setTimeout(() => {
            productsGrid.classList.add("products-list-view");
            productsGrid.style.opacity = "1";
          }, 150);
        } else {
          productsGrid.style.opacity = "1";
          setTimeout(() => {
            productsGrid.classList.remove("products-list-view");
            productsGrid.style.opacity = "1";
          }, 150);
        }

        // Save preference
        localStorage.setItem("productsView", view);

        // Track analytics
        if (typeof gtag !== "undefined") {
          gtag("event", "view_toggle", {
            view_type: view,
          });
        }
      });
    });

    // Load saved view preference
    const savedView = localStorage.getItem("productsView");
    if (savedView === "list") {
      const listButton = document.querySelector('[data-view="list"]');
      if (listButton) listButton.click();
    }
  }
  // SSR renders wishlist state; no client hydration needed


  // ================================
  // PRODUCT DETAIL PAGE
  // ================================

  function initializeProductDetail() {
    initializeQuantityControls();
    initializeImageGallery();
    initializeProductTabs();
    initializeProductActions();
  }

  function initializeQuantityControls() {
    const quantityInput = document.getElementById("quantity");
    const decreaseBtn = document.querySelector(".btn-quantity-decrease");
    const increaseBtn = document.querySelector(".btn-quantity-increase");

    if (!quantityInput) return;

    // Decrease quantity
    if (decreaseBtn) {
      decreaseBtn.addEventListener("click", function () {
        const currentValue =
          parseInt(quantityInput.value) || CONFIG.MIN_QUANTITY;
        if (currentValue > CONFIG.MIN_QUANTITY) {
          quantityInput.value = currentValue - 1;
          updateQuantityDisplay();
        }
      });
    }

    // Increase quantity
    if (increaseBtn) {
      increaseBtn.addEventListener("click", function () {
        const currentValue =
          parseInt(quantityInput.value) || CONFIG.MIN_QUANTITY;
        if (currentValue < CONFIG.MAX_QUANTITY) {
          quantityInput.value = currentValue + 1;
          updateQuantityDisplay();
        }
      });
    }

    // Direct input validation
    quantityInput.addEventListener("input", function () {
      this.value = validateQuantity(this.value);
      updateQuantityDisplay();
    });

    quantityInput.addEventListener("blur", function () {
      this.value = validateQuantity(this.value);
      updateQuantityDisplay();
    });
  }

  function updateQuantityDisplay() {
    // Update any quantity-dependent displays
    const quantity = parseInt(document.getElementById("quantity")?.value) || 1;

    // Update total price if shown
    const priceElement = document.querySelector(".current-price");
    if (priceElement && priceElement.dataset.unitPrice) {
      const unitPrice = parseFloat(priceElement.dataset.unitPrice);
      const totalPrice = unitPrice * quantity;
      // Update display if needed
    }
  }

  function initializeImageGallery() {
    const mainImage = document.getElementById("mainProductImage");
    const thumbnails = document.querySelectorAll(".thumbnail-image");
    const zoomOverlay = document.getElementById("imageZoomOverlay");

    // Thumbnail click handlers
    thumbnails.forEach((thumbnail) => {
      thumbnail.addEventListener("click", function () {
        if (mainImage) {
          mainImage.src = this.src;
          mainImage.alt = this.alt;

          // Update active thumbnail
          document.querySelectorAll(".thumbnail-item").forEach((item) => {
            item.classList.remove("active");
          });
          this.closest(".thumbnail-item").classList.add("active");
        }
      });
    });

    // Image zoom functionality
    if (zoomOverlay && mainImage) {
      zoomOverlay.addEventListener("click", function () {
        openImageModal(mainImage.src, mainImage.alt);
      });
    }
  }

  function openImageModal(imageSrc, imageAlt) {
    const modal = document.createElement("div");
    modal.className = "image-modal";
    modal.innerHTML = `
            <div class="image-modal-backdrop" onclick="this.parentElement.remove()">
                <img src="${imageSrc}" alt="${imageAlt}" class="image-modal-content">
                <button class="image-modal-close" onclick="this.parentElement.parentElement.remove()">
                    <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" /></svg>
                </button>
            </div>
        `;

    document.body.appendChild(modal);

    // Prevent body scrolling
    document.body.style.overflow = "hidden";

    // Remove modal when clicking outside or pressing Escape
    modal.addEventListener("click", function (e) {
      if (
        e.target === modal ||
        e.target.classList.contains("image-modal-backdrop")
      ) {
        document.body.style.overflow = "";
        modal.remove();
      }
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && modal.parentElement) {
        document.body.style.overflow = "";
        modal.remove();
      }
    });
  }

  function initializeProductTabs() {
    // Enhanced tab functionality
    const tabButtons = document.querySelectorAll(".product-tabs .nav-link");

    tabButtons.forEach((button) => {
      button.addEventListener("shown.bs.tab", function (e) {
        const targetTab = e.target.getAttribute("data-bs-target");

        // Track tab views
        if (typeof gtag !== "undefined") {
          gtag("event", "view_product_tab", {
            tab_name: targetTab.replace("#", ""),
          });
        }

        // Lazy load content if needed
        if (targetTab === "#reviews") {
          loadReviews();
        }
      });
    });
  }

  function loadReviews() {
    // TODO: Implement lazy loading of reviews
    console.log("Loading reviews...");
  }

  // ================================
  // SORTING & FILTERING
  // ================================

  function initializeSorting() {
    const sortSelect = document.getElementById("sortSelect");

    if (!sortSelect) return;

    sortSelect.addEventListener("change", function () {
      const value = this.value;
      changeSorting(value);
    });
  }

  function changeSorting(value) {
    const url = new URL(window.location);

    // Preserve categoryId if present
    const categoryId = url.searchParams.get("categoryId");

    // Update URL parameters based on sort value
    switch (value) {
      case "newest":
        url.searchParams.set("sort", "newest");
        url.searchParams.delete("direction");
        break;
      case "oldest":
        url.searchParams.set("sort", "oldest");
        url.searchParams.delete("direction");
        break;
      case "name":
        url.searchParams.set("sort", "name");
        url.searchParams.set("direction", "asc");
        break;
      case "price-asc":
        url.searchParams.set("sort", "price");
        url.searchParams.set("direction", "asc");
        break;
      case "price-desc":
        url.searchParams.set("sort", "price");
        url.searchParams.set("direction", "desc");
        break;
    }

    // Preserve categoryId
    if (categoryId) {
      url.searchParams.set("categoryId", categoryId);
    }

    // Reset to first page
    url.searchParams.set("page", "0");

    // Show loading state
    const productsGrid = document.getElementById("productsGrid");
    if (productsGrid) {
      productsGrid.style.opacity = "0.6";
    }

    // Navigate to new URL
    window.location.href = url.toString();
  }

  // ================================
  // PERFORMANCE OPTIMIZATIONS
  // ================================

  function initializePerformanceOptimizations() {
    // Lazy loading for product images
    if ("IntersectionObserver" in window) {
      const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src || img.src;
            img.classList.remove("lazy");
            observer.unobserve(img);
          }
        });
      });

      document.querySelectorAll('img[loading="lazy"]').forEach((img) => {
        imageObserver.observe(img);
      });
    }

    // // Preload critical resources
    // const preloadLinks = [
    //     '/css/products.css',
    //     '/js/products.js'
    // ];

    // preloadLinks.forEach(href => {
    //     const link = document.createElement('link');
    //     link.rel = 'preload';
    //     link.href = href;
    //     link.as = href.endsWith('.css') ? 'style' : 'script';
    //     document.head.appendChild(link);
    // });
  }

  // ================================
  // ERROR HANDLING
  // ================================

  function initializeErrorHandling() {
    // Global error handler for AJAX requests
    window.addEventListener("unhandledrejection", function (event) {
      console.error("Unhandled promise rejection:", event.reason);
      showToast("Đã xảy ra lỗi. Vui lòng thử lại sau.", "error");
    });

    // Network error detection
    window.addEventListener("online", function () {
      showToast("Kết nối internet đã được khôi phục");
    });

    window.addEventListener("offline", function () {
      showToast("Mất kết nối internet", "error");
    });
  }

  // ================================
  // INITIALIZATION
  // ================================

  function initialize() {
    console.log("Products.js: Initializing...");

    // Check if we're on a products page
    const isProductsPage =
      document.querySelector(".products-section") ||
      document.querySelector(".product-detail-section");

    console.log("Products.js: isProductsPage =", isProductsPage);

    if (!isProductsPage) {
      console.log("Products.js: Not on products page, skipping initialization");
      return;
    }

    // Initialize common functionality
    initializeErrorHandling();
    initializePerformanceOptimizations();
    // Search initialized in main.js
    initializeSorting();

    // Initialize page-specific functionality
    if (document.querySelector(".products-section")) {
      // Products listing page
      console.log("Products.js: Initializing product grid...");

      // Test toast notification
      setTimeout(() => {
        showToast("Products.js loaded successfully!", "success");
      }, 1000);

      initializeProductGrid();
    }

    if (document.querySelector(".product-detail-section")) {
      // Product detail page
      initializeProductDetail();
    }

    console.log("Products page initialized successfully");
  }

  // (Wishlist toggle logic handled by main.js)

// ================================
// ERROR HANDLING
// ================================

function initializeErrorHandling() {
// Global error handler for AJAX requests
window.addEventListener("unhandledrejection", function (event) {
  console.error("Unhandled promise rejection:", event.reason);
  showToast("Đã xảy ra lỗi. Vui lòng thử lại sau.", "error");
});

// Network error detection
window.addEventListener("online", function () {
  showToast("Kết nối internet đã được khôi phục");
});

window.addEventListener("offline", function () {
  showToast("Mất kết nối internet", "error");
});
}

// ================================
// INITIALIZATION
// ================================

function initialize() {
console.log("Products.js: Initializing...");

// Check if we're on a products page
const isProductsPage =
  document.querySelector(".products-section") ||
  document.querySelector(".product-detail-section");

console.log("Products.js: isProductsPage =", isProductsPage);

if (!isProductsPage) {
  console.log("Products.js: Not on products page, skipping initialization");
  return;
}

// Initialize common functionality
initializeErrorHandling();
initializePerformanceOptimizations();
initializeSearch();
initializeSorting();

// Initialize page-specific functionality
if (document.querySelector(".products-section")) {
  // Products listing page
  console.log("Products.js: Initializing product grid...");

  // Test toast notification
  setTimeout(() => {
    showToast("Products.js loaded successfully!", "success");
  }, 1000);

  initializeProductGrid();
}

if (document.querySelector(".product-detail-section")) {
  // Product detail page
  initializeProductDetail();
}

console.log("Products page initialized successfully");
}

// ================================
// WISHLIST FUNCTIONALITY
// ================================

function handleWishlistToggle(button) {
const productId = button.dataset.productId;
  
if (!productId) {
  showToast('Không thể thêm sản phẩm vào danh sách yêu thích', 'error');
  return;
}
  
// Disable button to prevent multiple clicks
button.disabled = true;
const icon = button.querySelector('i');
const originalIconClass = icon ? icon.className : '';
  
// Show loading state
if (icon) {
  icon.className = 'fa-solid fa-spinner fa-spin';
}
  
// Get CSRF token
const csrfToken = getCsrfToken();
const csrfHeaderElement = document.querySelector('meta[name="_csrf_header"]');
const csrfHeader = csrfHeaderElement ? csrfHeaderElement.getAttribute('content') : 'X-CSRF-TOKEN';
  
const headers = {
  'Content-Type': 'application/json',
  'X-Requested-With': 'XMLHttpRequest'
};
  
if (csrfToken && csrfHeader) {
  headers[csrfHeader] = csrfToken;
}
  
// API call to toggle wishlist
fetch('/api/wishlist/toggle', {
  method: 'POST',
  headers: headers,
  credentials: 'same-origin',
  body: JSON.stringify({ productId: parseInt(productId) })
})
.then(response => {
  if (response.status === 401) {
    showToast('Vui lòng đăng nhập để sử dụng tính năng yêu thích', 'warning');
    if (icon) icon.className = originalIconClass;
    button.disabled = false;
    return null;
  }
  
  if (response.status === 403) {
    showToast('Lỗi bảo mật: Vui lòng refresh trang và thử lại', 'error');
    if (icon) icon.className = originalIconClass;
    button.disabled = false;
    return null;
  }
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  return response.json();
})
.then(data => {
  if (!data) return;
  
  console.log('Wishlist API response:', data);
  
  if (data && data.success && data.data && data.data.success) {
    // Update UI based on server response
    const isInWishlist = data.data.isFavorite || data.data.isInWishlist;
    
    if (isInWishlist) {
      button.classList.add('active');
      if (icon) icon.className = 'fa-solid fa-heart';
      showToast('Đã thêm vào danh sách yêu thích', 'success');
    } else {
      button.classList.remove('active');
      if (icon) icon.className = 'fa-regular fa-heart';
      showToast('Đã xóa khỏi danh sách yêu thích', 'success');
    }
    
    // Update wishlist count in header - ALWAYS update
    console.log('Full API response data:', data.data);
    
    // Try to get count from response
    let wishlistCount = data.data.userWishlistCount || data.data.favoriteCount || data.data.wishlistCount;
    console.log('Wishlist count from API:', wishlistCount);
    
    // If no count in response, fetch it
    if (wishlistCount === undefined || wishlistCount === null) {
      console.warn('No wishlist count in toggle response, fetching from API...');
      fetchAndUpdateWishlistCount();
    } else {
      // Update count immediately
      console.log('Updating wishlist count to:', wishlistCount);
      if (typeof window.updateWishlistCount === 'function') {
        window.updateWishlistCount(wishlistCount);
        console.log('✅ updateWishlistCount called successfully');
      }
      return null;
    }
  }})
  .then(data => {
      if (!data) return;
      
      console.log('Wishlist API response:', data);
      
      if (data && data.success && data.data && data.data.success) {
        // Update UI based on server response
        const isInWishlist = data.data.isFavorite || data.data.isInWishlist;
        
        if (isInWishlist) {
          button.classList.add('active');
          if (icon) icon.className = 'fa-solid fa-heart';
          showToast('Đã thêm vào danh sách yêu thích', 'success');
        } else {
          button.classList.remove('active');
          if (icon) icon.className = 'fa-regular fa-heart';
          showToast('Đã xóa khỏi danh sách yêu thích', 'success');
        }
        
        // Update wishlist count in header - ALWAYS update
        console.log('Full API response data:', data.data);
        
        // Try to get count from response
        let wishlistCount = data.data.userWishlistCount || data.data.favoriteCount || data.data.wishlistCount;
        console.log('Wishlist count from API:', wishlistCount);
        
        // If no count in response, fetch it
        if (wishlistCount === undefined || wishlistCount === null) {
          console.warn('No wishlist count in toggle response, fetching from API...');
          fetchAndUpdateWishlistCount();
        } else {
          // Update count immediately
          console.log('Updating wishlist count to:', wishlistCount);
          if (typeof window.updateWishlistCount === 'function') {
            window.updateWishlistCount(wishlistCount);
            console.log('✅ updateWishlistCount called successfully');
          } else {
            console.error('❌ updateWishlistCount function not found!');
            // Try direct update as fallback
            updateWishlistCountDirect(wishlistCount);
          }
        }
      } else {
        if (icon) icon.className = originalIconClass;
        const errorMessage = (data && data.error) || (data && data.data && data.data.message) || 'Có lỗi xảy ra';
        showToast(errorMessage, 'error');
        console.error('Wishlist error:', errorMessage, data);
      }
    })
    .catch(error => {
      console.error('Error toggling wishlist:', error);
      if (icon) icon.className = originalIconClass;
      showToast('Có lỗi xảy ra khi thực hiện yêu cầu', 'error');
    })
    .finally(() => {
      button.disabled = false;
    });
  }

  // ================================
  // GLOBAL FUNCTIONS (for inline handlers)
  // ================================

  // ================================
  // CATEGORY FILTER FUNCTIONALITY
  // ================================

  function changeCategory(value) {
    const url = new URL(window.location);
    
    if (value) {
      url.searchParams.set("categoryId", value);
    } else {
      url.searchParams.delete("categoryId");
    }
    
    // Reset to first page when changing category
    url.searchParams.set("page", "0");
    
    // Show loading state
    const productsGrid = document.getElementById("productsGrid");
    if (productsGrid) {
      productsGrid.style.opacity = "0.6";
    }
    
    window.location.href = url.toString();
  }

  // ================================
  // GLOBAL FUNCTIONS (for inline handlers)
  // ================================

  // Make some functions globally accessible for inline event handlers
  window.changeSorting = changeSorting;
  window.changeCategory = changeCategory;
  
  // Note: Wishlist now uses event delegation, no need for global function

  window.buyNow = function (button) {
    const productId = button.dataset.productId;
    const quantity = document.getElementById("quantity")?.value || 1;

    // TODO: Implement buy now functionality
    console.log("Buy now:", { productId, quantity });

    // For now, redirect to cart
    window.location.href = "/cart";
  };

  // window.increaseQuantity = function () {
  //   const input = document.getElementById("quantity");
  //   if (input) {
  //     const currentValue = parseInt(input.value) || CONFIG.MIN_QUANTITY;
  //     if (currentValue < CONFIG.MAX_QUANTITY) {
  //       input.value = currentValue + 1;
  //       updateQuantityDisplay();
  //     }
  //   }
  // };

  // window.decreaseQuantity = function () {
  //   const input = document.getElementById("quantity");
  //   if (input) {
  //     const currentValue = parseInt(input.value) || CONFIG.MIN_QUANTITY;
  //     if (currentValue > CONFIG.MIN_QUANTITY) {
  //       input.value = currentValue - 1;
  //       updateQuantityDisplay();
  //     }
  //   }
  // };

  // ================================
  // UTILITY FUNCTIONS
  // ================================

  // Use global getCsrfToken provided by main.js

  // Use showToast from main.js if available, otherwise create a simple fallback
  // Use global showToast provided by main.js
  // ================================
  // DOM READY
  // ================================

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize);
  } else {
    initialize();
  }
})();
