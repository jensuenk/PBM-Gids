package com.ineos.oxide.pbmgids.services;

import java.util.List;
import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.model.entities.Pbm;

/**
 * Service class that handles category-related data operations for the catalog functionality.
 * Provides data access and validation logic for category-based PBM operations.
 */
public class CategoryDataService {
    private static final Logger logger = Logger.getLogger(CategoryDataService.class.getName());
    
    private final CatalogService catalogService;
    private Integer currentCategoryId;

    public CategoryDataService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Loads PBMs for the specified category ID
     * @param categoryId The category ID to load PBMs for
     * @return List of PBMs in the category
     */
    public List<Pbm> loadPbmsByCategory(Integer categoryId) {
        if (categoryId == null) {
            logger.warning("Category ID is null, returning empty list");
            return List.of();
        }

        this.currentCategoryId = categoryId;
        logger.info("Loading PBMs for category ID: " + categoryId);
        
        try {
            return catalogService.getByCategory(categoryId);
        } catch (Exception e) {
            logger.severe("Error loading PBMs for category " + categoryId + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Gets the current category ID
     * @return The current category ID or null if none is set
     */
    public Integer getCurrentCategoryId() {
        return currentCategoryId;
    }

    /**
     * Validates if a category ID string is valid
     * @param categoryIdParam The category ID parameter to validate
     * @return The parsed category ID or null if invalid
     */
    public Integer validateAndParseCategoryId(String categoryIdParam) {
        if (categoryIdParam == null || categoryIdParam.trim().isEmpty()) {
            logger.warning("Category ID parameter is null or empty");
            return null;
        }

        try {
            return Integer.parseInt(categoryIdParam.trim());
        } catch (NumberFormatException e) {
            logger.warning("Invalid category ID parameter: " + categoryIdParam);
            return null;
        }
    }

    /**
     * Gets the name of a category by its ID
     * @param categoryId The category ID
     * @return The category name or null if not found
     */
    public String getCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }

        try {
            var category = catalogService.getCategoryById(categoryId);
            return category != null ? category.getName() : null;
        } catch (Exception e) {
            logger.severe("Error getting category name for ID " + categoryId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Resets the current category
     */
    public void resetCategory() {
        this.currentCategoryId = null;
    }
}