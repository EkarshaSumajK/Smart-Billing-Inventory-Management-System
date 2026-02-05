package com.smartretail.backend.service;

import com.smartretail.backend.exception.ProductNotFoundException;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FileService fileService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Locale locale;

    @BeforeEach
    void setUp() {
        locale = Locale.forLanguageTag("en");

        product = new Product();
        product.setProductId("p12345678");
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(999.99);
        product.setQuantity(8);
        product.setReorderLevel(10);
        product.setExpiryDate(null);
        product.setAddedBy("ravi@shop.com");
        product.setLastUpdated(new Date());

        // Mock MessageSource for error messages
        when(messageSource.getMessage(eq("product.exists"), any(), eq(locale)))
                .thenReturn("Product already exists: p12345678");
        when(messageSource.getMessage(eq("product.not.found"), any(), eq(locale)))
                .thenReturn("Product not found: p12345678");
        when(messageSource.getMessage(eq("product.restock.invalid.quantity"), any(), eq(locale)))
                .thenReturn("Restock quantity must be positive");
        when(messageSource.getMessage(eq("product.update.failed"), any(), eq(locale)))
                .thenReturn("Product update failed: p12345678");
    }

    @Test
    void testAddProductSuccess() {
        when(productRepository.existsByProductId("p12345678")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.createProduct(product, locale);

        assertNotNull(result);
        assertEquals("p12345678", result.getProductId());
        verify(productRepository).save(product);
    }

    @Test
    void testAddProductDuplicateId() {
        when(productRepository.existsByProductId("p12345678")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.createProduct(product, locale));
        assertEquals("Product already exists: p12345678", exception.getMessage());
    }

    @Test
    void testGetLowStockProducts() {
        when(productRepository.findLowStockProducts()).thenReturn(Arrays.asList(product));

        List<Product> products = productService.getLowStockProducts();

        assertEquals(1, products.size());
        assertEquals("p12345678", products.get(0).getProductId());
    }

    @Test
    void testGetExpiringProducts() {
        product.setExpiryDate(new Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L));
        when(productRepository.findByExpiryDateBefore(any(Date.class))).thenReturn(Arrays.asList(product));

        List<Product> products = productService.getExpiringProducts(
                new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));

        assertEquals(1, products.size());
        assertEquals("p12345678", products.get(0).getProductId());
    }

    @Test
    void testRestockProductSuccess() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.restockProduct("p12345678", 5, locale);

        assertEquals(13, result.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void testRestockProductInvalidQty() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.restockProduct("p12345678", 0, locale));
        assertEquals("Restock quantity must be positive", exception.getMessage());
    }

    @Test
    void testRestockProductNotFound() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productService.restockProduct("p12345678", 5, locale));
        assertEquals("Product not found: p12345678", exception.getMessage());
    }

    @Test
    void testGetProductByIdSuccess() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));

        Product result = productService.getProductById("p12345678", locale);

        assertNotNull(result);
        assertEquals("p12345678", result.getProductId());
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById("p12345678", locale));
        assertEquals("Product not found: p12345678", exception.getMessage());
    }

    // NEW TESTS FOR SYNC MODE FUNCTIONALITY

    @Test
    void testUpdateProductSyncMode_Success() {
        Product updatedProduct = new Product();
        updatedProduct.setProductId("p12345678");
        updatedProduct.setName("Laptop Pro");
        updatedProduct.setCategory("Electronics");
        updatedProduct.setPrice(1099.99);
        updatedProduct.setQuantity(15);

        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        Product result = productService.updateProduct("p12345678", updatedProduct, locale, true);

        assertNotNull(result);
        assertEquals("Laptop Pro", result.getName());
        assertEquals(1099.99, result.getPrice());
        assertEquals(15, result.getQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testUpdateProductSyncMode_Unchanged_SkipsUpdate() {
        Product unchangedProduct = new Product();
        unchangedProduct.setProductId("p12345678");
        unchangedProduct.setName("Laptop");
        unchangedProduct.setCategory("Electronics");
        unchangedProduct.setPrice(999.99);
        unchangedProduct.setQuantity(8);

        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));

        Product result = productService.updateProduct("p12345678", unchangedProduct, locale, true);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals(999.99, result.getPrice());
        assertEquals(8, result.getQuantity());
        // Should not save since no changes
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProductSyncMode_NullCategory_Success() {
        Product updatedProduct = new Product();
        updatedProduct.setProductId("p12345678");
        updatedProduct.setName("Laptop");
        updatedProduct.setCategory(null); // Null category
        updatedProduct.setPrice(999.99);
        updatedProduct.setQuantity(10); // Changed quantity

        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        Product result = productService.updateProduct("p12345678", updatedProduct, locale, true);

        assertNotNull(result);
        assertEquals(10, result.getQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testUpdateProductSyncMode_ProductNotFound() {
        Product updatedProduct = new Product();
        updatedProduct.setProductId("p12345678");
        updatedProduct.setName("Laptop Pro");

        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productService.updateProduct("p12345678", updatedProduct, locale, true));
    }

    @Test
    void testUpdateProductQuantity_Success() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateProductQuantity("p12345678", 3, locale);

        assertEquals(5, result.getQuantity()); // 8 - 3 = 5
        verify(productRepository).save(product);
    }

    @Test
    void testUpdateProductQuantity_InsufficientStock() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProductQuantity("p12345678", 10, locale));
        assertTrue(exception.getMessage().contains("p12345678"));
    }

    @Test
    void testUpdateProductQuantity_ProductNotFound() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productService.updateProductQuantity("p12345678", 3, locale));
        assertEquals("Product not found: p12345678", exception.getMessage());
    }

    @Test
    void testExistsByProductId_True() {
        when(productRepository.existsByProductId("p12345678")).thenReturn(true);

        boolean exists = productService.existsByProductId("p12345678");

        assertTrue(exists);
        verify(productRepository).existsByProductId("p12345678");
    }

    @Test
    void testExistsByProductId_False() {
        when(productRepository.existsByProductId("p12345678")).thenReturn(false);

        boolean exists = productService.existsByProductId("p12345678");

        assertFalse(exists);
        verify(productRepository).existsByProductId("p12345678");
    }

    @Test
    void testGetAllProducts() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product));

        List<Product> products = productService.getAllProducts();

        assertEquals(1, products.size());
        assertEquals("p12345678", products.get(0).getProductId());
        verify(productRepository).findAll();
    }

    @Test
    void testDeleteProduct_Success() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteByProductId("p12345678");

        productService.deleteProduct("p12345678", locale);

        verify(productRepository).deleteByProductId("p12345678");
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(productRepository.findByProductId("p12345678")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct("p12345678", locale));
        assertEquals("Product not found: p12345678", exception.getMessage());
    }
}