package com.smartretail.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import com.smartretail.backend.dto.ProductRequest;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.service.FileService;
import com.smartretail.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    static {
        DATE_FMT.setLenient(false);
    }

    private final ProductService productService;
    private final FileService fileService;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ProductController(ProductService productService,
            FileService fileService,
            MessageSource messageSource) {
        this.productService = productService;
        this.fileService = fileService;
        this.messageSource = messageSource;
    }

    /* -------------------- CREATE -------------------- */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> createJson(
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {

        Product product = toEntity(request);
        Product saved = productService.createProduct(product, parseLocale(lang));
        return buildCreatedResponse(saved);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> createMultipart(
            @RequestParam("product") String productJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) throws Exception {

        ProductRequest req = objectMapper.readValue(productJson, ProductRequest.class);
        Product product = toEntity(req);
        Product saved = productService.createProduct(product, imageFile, parseLocale(lang));
        return buildCreatedResponse(saved);
    }

    /* -------------------- IMAGE -------------------- */
    @GetMapping("/image/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        try {
            Map<String, Object> imageData = fileService.getImage(imageId);
            byte[] bytes = (byte[]) imageData.get("bytes");
            String contentType = (String) imageData.get("contentType");

            ByteArrayResource resource = new ByteArrayResource(bytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .contentLength(bytes.length)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to retrieve image: {}", imageId, e);
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Map<String, String>> buildCreatedResponse(Product p) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Product added successfully", "productId", p.getProductId()));
    }

    /* -------------------- READ -------------------- */
    @GetMapping("/{productId}")
    public ResponseEntity<Product> get(@PathVariable String productId,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return ResponseEntity.ok(productService.getProductById(productId, parseLocale(lang)));
    }

    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /* -------------------- UPDATE -------------------- */
    @PutMapping(value = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> patch(
            @PathVariable String productId,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {

        Locale locale = parseLocale(lang);
        Product patch = new Product();

        updates.forEach((k, v) -> {
            try {
                switch (k) {
                    case "name" -> patch.setName(safeString(v));
                    case "category" -> patch.setCategory(safeString(v));
                    case "price" -> patch.setPrice(safeDouble(v));
                    case "quantity" -> patch.setQuantity(safeInt(v));
                    case "minQuantity" -> patch.setMinQuantity(safeInt(v));
                    case "reorderLevel" -> patch.setReorderLevel(safeInt(v));
                    case "expiryDate" -> patch.setExpiryDate(parseDate(safeString(v)));
                    case "imageUrl" -> patch.setImageUrl(safeString(v));
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid value for field '" + k + "': " + v);
            }
        });

        Product updated = productService.updateProduct(productId, patch, locale);
        return ResponseEntity.ok(Map.of(
                "message", "Product updated successfully",
                "productId", updated.getProductId()));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> updateMultipart(
            @PathVariable String productId,
            @RequestParam("product") String productJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) throws Exception {

        Locale locale = parseLocale(lang);

        // 1. Deserialize the JSON string into a Product object
        ProductRequest req = objectMapper.readValue(productJson, ProductRequest.class);
        Product productPatch = toEntity(req);

        // 2. Call the service method that handles file uploads
        Product updated = productService.updateProduct(productId, productPatch, imageFile, locale);

        // 3. Return the same response as your other methods
        return ResponseEntity.ok(Map.of(
                "message", "Product updated successfully",
                "productId", updated.getProductId()));
    }

    /* -------------------- DELETE -------------------- */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('OWNER')")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String productId,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {

        productService.deleteProduct(productId, parseLocale(lang));

        return ResponseEntity.ok(Map.of(
                "message", "Product deleted successfully",
                "productId", productId));
    }

    /* -------------------- BULK CSV -------------------- */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkUpload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) throws Exception {

        Locale locale = parseLocale(lang);
        validateCsvFile(file, locale);

        List<Product> success = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<ProductCsvBean> beans = new CsvToBeanBuilder<ProductCsvBean>(r)
                    .withType(ProductCsvBean.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            // --- MODIFICATION: Updated loop logic ---
            for (int i = 0; i < beans.size(); i++) {
                ProductCsvBean b = beans.get(i);
                int row = i + 2;
                try {
                    // 1. Validate row (now only checks for name)
                    int errorsBefore = errors.size();
                    validateCsvRow(b, row, errors);
                    if (errors.size() > errorsBefore) {
                        continue; // validateCsvRow added an error, skip this row
                    }

                    // 2. Convert from bean (now doesn't set productId)
                    Product p = fromCsvBean(b);

                    // 3. Handle Product ID (Generate or Validate)
                    if (b.getProductId() == null || b.getProductId().isBlank()) {
                        // ID is missing: Generate a new one
                        String generatedId = "P-" + System.currentTimeMillis() + "-" + i;
                        p.setProductId(generatedId);
                        // No need to check for existence, we just created it
                    } else {
                        // ID is present: Use it and check for duplicates
                        p.setProductId(b.getProductId());
                        if (productService.existsByProductId(b.getProductId())) {
                            skipped.add("Row " + row + ": Product ID " + b.getProductId() + " already exists");
                            continue;
                        }
                    }

                    // 4. Create the product
                    success.add(productService.createProduct(p, locale));

                } catch (Exception ex) {
                    errors.add("Row " + row + ": " + ex.getMessage());
                }
            }
            // --- END MODIFICATION ---

        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successful", success.size());
        result.put("skipped", skipped.size());
        result.put("errors", errors.size());
        result.put("successfulProductIds", success.stream().map(Product::getProductId).collect(Collectors.toList()));
        result.put("skippedDetails", skipped);
        result.put("errorDetails", errors);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /* -------------------- HELPERS -------------------- */
    private Locale parseLocale(String header) {
        if (header == null || header.isBlank())
            return Locale.ENGLISH;
        String primary = header.split(",")[0].split(";")[0].trim();
        String[] parts = primary.split("-");
        return parts.length == 2 ? new Locale(parts[0], parts[1]) : new Locale(parts[0]);
    }

    private void validateCsvFile(MultipartFile file, Locale locale) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(messageSource.getMessage("file.missing", null, locale));
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException(messageSource.getMessage("file.invalid.format", null, locale));
        }
    }

    // --- MODIFICATION: Removed productId check ---
    private void validateCsvRow(ProductCsvBean b, int row, List<String> errors) {
        if (b.getName() == null || b.getName().isBlank()) {
            errors.add("Row " + row + ": Product name missing");
        }
    }

    // --- MODIFICATION: Removed productId setting ---
    private Product fromCsvBean(ProductCsvBean b) {
        Product p = new Product();
        // p.setProductId(b.getProductId()); // <-- This is now handled in the main loop
        p.setName(b.getName());
        p.setCategory(b.getCategory());
        p.setPrice(b.getPrice());
        p.setQuantity(b.getQuantity());
        p.setMinQuantity(b.getMinQuantity());
        p.setReorderLevel(b.getReorderLevel());
        p.setAddedBy(Optional.ofNullable(b.getAddedBy()).orElse("system"));
        p.setSupplierEmail("default@supplier.com");

        if (b.getExpiryDate() != null && !b.getExpiryDate().isBlank()) {
            p.setExpiryDate(parseDate(b.getExpiryDate()));
        }
        p.setCreatedAt(new Date());
        p.setLastUpdated(new Date());
        return p;
    }

    private Date parseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return DATE_FMT.parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format, expected yyyy-MM-dd: " + s);
        }
    }

    private String safeString(Object o) {
        return o == null ? null : o.toString();
    }

    private double safeDouble(Object o) {
        return o == null ? 0.0 : Double.parseDouble(o.toString());
    }

    private int safeInt(Object o) {
        return o == null ? 0 : Integer.parseInt(o.toString());
    }

    // Convert ProductRequest to Product entity
    private Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setProductId(request.getProductId());
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setMinQuantity(request.getMinQuantity());
        product.setReorderLevel(request.getReorderLevel());
        product.setExpiryDate(parseDate(request.getExpiryDate()));
        product.setImageUrl(request.getImageUrl());
        product.setSupplierEmail(request.getSupplierEmail());
        product.setAddedBy("system"); // Default value
        product.setCreatedAt(new Date());
        product.setLastUpdated(new Date());
        return product;
    }

    /* -------------------- CSV BEAN -------------------- */
    public static class ProductCsvBean {
        // --- MODIFICATION: made productId not required ---
        @CsvBindByName(required = false)
        private String productId;
        @CsvBindByName(required = true)
        private String name;
        @CsvBindByName
        private String category;
        @CsvBindByName
        private double price;
        @CsvBindByName
        private int quantity;
        @CsvBindByName
        private int minQuantity;
        @CsvBindByName
        private int reorderLevel;
        @CsvBindByName
        private String expiryDate;
        @CsvBindByName
        private String addedBy;

        // Manual getters and setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getMinQuantity() {
            return minQuantity;
        }

        public void setMinQuantity(int minQuantity) {
            this.minQuantity = minQuantity;
        }

        public int getReorderLevel() {
            return reorderLevel;
        }

        public void setReorderLevel(int reorderLevel) {
            this.reorderLevel = reorderLevel;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getAddedBy() {
            return addedBy;
        }

        public void setAddedBy(String addedBy) {
            this.addedBy = addedBy;
        }
    }
}