package com.ineos.oxide.pbmgids.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ineos.oxide.pbmgids.model.entities.Category;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.model.repositories.CategoryRepository;
import com.ineos.oxide.pbmgids.model.repositories.PbmRepository;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final PbmRepository pbmRepository;

    public CatalogService(CategoryRepository categoryRepository, PbmRepository pbmRepository) {
        this.categoryRepository = categoryRepository;
        this.pbmRepository = pbmRepository;
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    public List<Pbm> getByCategory(Integer categoryId) {
        return pbmRepository.findByCategoryIdWithRelations(categoryId);
    }

    public Pbm getPbm(Integer id) {
        return pbmRepository.findDetailedById(id).orElse(null);
    }

    public List<Category> getChildren(Integer parentId) {
        return categoryRepository.findByParent_IdOrderByNameAsc(parentId);
    }

    public Category getCategoryById(Integer categoryId) {
        return categoryRepository.findById(categoryId).orElse(null);
    }

    public List<Pbm> searchAllPbms(String searchTerm) {
        return pbmRepository.findAll().stream()
                .filter(pbm -> matchesPbm(pbm, searchTerm.toLowerCase()))
                .toList();
    }

    private boolean matchesPbm(Pbm pbm, String searchTerm) {
        return containsIgnoreCase(pbm.getName(), searchTerm) ||
                containsIgnoreCase(pbm.getBrand(), searchTerm) ||
                containsIgnoreCase(pbm.getTypeName(), searchTerm) ||
                containsIgnoreCase(pbm.getDescription(), searchTerm) ||
                containsIgnoreCase(pbm.getProtectsAgainst(), searchTerm) ||
                containsIgnoreCase(pbm.getDoesNotProtectAgainst(), searchTerm) ||
                containsIgnoreCase(pbm.getNotes(), searchTerm) ||
                containsIgnoreCase(pbm.getUsageInstructions(), searchTerm) ||
                containsIgnoreCase(pbm.getDistribution(), searchTerm) ||
                containsIgnoreCase(pbm.getStandards(), searchTerm);
    }

    public List<String> getPbmNameSuggestions(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Return top 10 PBM names when no search term
            return pbmRepository.findAll().stream()
                    .map(Pbm::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .sorted()
                    .limit(10)
                    .toList();
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();
        return pbmRepository.findAll().stream()
                .map(Pbm::getName)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> name.toLowerCase().contains(lowerSearchTerm))
                .distinct()
                .sorted()
                .limit(10)
                .toList();
    }

    private boolean containsIgnoreCase(String value, String searchTerm) {
        return value != null && value.toLowerCase().contains(searchTerm);
    }

    /**
     * Gets all categories including parent hierarchy for a given category.
     * This method ensures all parent categories are properly loaded to avoid
     * LazyInitializationException.
     * The returned list is ordered from most specific (leaf) to most general
     * (root).
     * 
     * @param category The starting category
     * @return List of categories from leaf to root (most specific to most general)
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesWithParents(Category category) {
        List<Category> categories = new ArrayList<>();
        Category current = category;

        while (current != null) {
            // Re-fetch the category with parent eagerly loaded to ensure it's in the
            // current session
            Category fetchedCategory = categoryRepository.findByIdWithParent(current.getId()).orElse(null);
            if (fetchedCategory != null) {
                categories.add(fetchedCategory);
                current = fetchedCategory.getParent();
            } else {
                break;
            }
        }

        return categories;
    }

    /**
     * Optimized method to get all categories with parents for multiple categories
     * at once.
     * This reduces the number of database queries by fetching all required
     * categories in batches.
     * 
     * @param categories The starting categories
     * @return Map of category ID to list of categories including parents (leaf to
     *         root order)
     */
    @Transactional(readOnly = true)
    public java.util.Map<Integer, List<Category>> getAllCategoriesWithParentsBatch(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return new java.util.HashMap<>();
        }

        // Collect all category IDs we need to fetch (including potential parents)
        Set<Integer> allCategoryIds = new LinkedHashSet<>();

        // First, get all direct category IDs
        for (Category category : categories) {
            if (category.getId() != null) {
                allCategoryIds.add(category.getId());
            }
        }

        // Fetch all categories with parents eagerly loaded to avoid N+1 queries and
        // lazy loading issues
        List<Category> allCategories = categoryRepository.findByIdInWithParent(new ArrayList<>(allCategoryIds));
        java.util.Map<Integer, Category> categoryMap = allCategories.stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, c -> c));

        // Now build parent hierarchies for each category
        java.util.Map<Integer, List<Category>> result = new java.util.HashMap<>();

        for (Category category : categories) {
            List<Category> hierarchyList = new ArrayList<>();
            Category current = categoryMap.get(category.getId());

            while (current != null) {
                hierarchyList.add(current);
                Integer parentId = current.getParent() != null ? current.getParent().getId() : null;

                if (parentId != null) {
                    // Try to get parent from our existing map first
                    current = categoryMap.get(parentId);

                    // If not in map, fetch it with parent eagerly loaded and add to map (this
                    // handles deep hierarchies)
                    if (current == null) {
                        current = categoryRepository.findByIdWithParent(parentId).orElse(null);
                        if (current != null) {
                            categoryMap.put(parentId, current);
                        }
                    }
                } else {
                    current = null;
                }
            }

            result.put(category.getId(), hierarchyList);
        }

        return result;
    }

    /**
     * Gets all unique categories including parent hierarchies for a PBM.
     * Uses optimized batch fetching and ID-based deduplication.
     * 
     * @param pbm The PBM to get categories for
     * @return List of unique categories including all parents, ordered from most
     *         specific to most general
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesWithParents(Pbm pbm) {
        if (pbm.getCategories() == null || pbm.getCategories().isEmpty()) {
            return new ArrayList<>();
        }

        // Use the optimized batch method for better performance
        java.util.Map<Integer, List<Category>> batchResult = getAllCategoriesWithParentsBatch(
                new ArrayList<>(pbm.getCategories()));

        // Collect ALL categories from all hierarchies first
        java.util.Map<Integer, Category> uniqueCategories = new java.util.LinkedHashMap<>();

        // Process each category's hierarchy and collect unique categories
        for (Category category : pbm.getCategories()) {
            List<Category> categoriesWithParents = batchResult.get(category.getId());

            if (categoriesWithParents != null) {
                // Add all categories from this hierarchy, but only if we haven't seen their ID
                // before
                for (Category cat : categoriesWithParents) {
                    if (cat.getId() != null && !uniqueCategories.containsKey(cat.getId())) {
                        uniqueCategories.put(cat.getId(), cat);
                    }
                }
            }
        }

        // Convert to list maintaining insertion order
        return new ArrayList<>(uniqueCategories.values());
    }

    /**
     * Gets all unique categories including parent hierarchies for multiple PBMs.
     * Highly optimized for batch processing to minimize database queries.
     * 
     * @param pbms The PBMs to get categories for
     * @return List of unique categories including all parents from all PBMs
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesWithParents(List<Pbm> pbms) {
        if (pbms == null || pbms.isEmpty()) {
            return new ArrayList<>();
        }

        // Collect all unique categories from all PBMs first
        Set<Integer> uniqueCategoryIds = new LinkedHashSet<>();
        List<Category> allPbmCategories = new ArrayList<>();

        for (Pbm pbm : pbms) {
            if (pbm.getCategories() != null) {
                for (Category category : pbm.getCategories()) {
                    if (category.getId() != null && !uniqueCategoryIds.contains(category.getId())) {
                        uniqueCategoryIds.add(category.getId());
                        allPbmCategories.add(category);
                    }
                }
            }
        }

        // Use batch method to get all hierarchies at once - much more efficient!
        java.util.Map<Integer, List<Category>> batchResult = getAllCategoriesWithParentsBatch(allPbmCategories);

        // Collect ALL categories from all hierarchies using improved deduplication
        java.util.Map<Integer, Category> uniqueCategories = new java.util.LinkedHashMap<>();

        for (Category category : allPbmCategories) {
            List<Category> categoriesWithParents = batchResult.get(category.getId());

            if (categoriesWithParents != null) {
                for (Category cat : categoriesWithParents) {
                    if (cat.getId() != null && !uniqueCategories.containsKey(cat.getId())) {
                        uniqueCategories.put(cat.getId(), cat);
                    }
                }
            }
        }

        // Convert to list maintaining insertion order
        return new ArrayList<>(uniqueCategories.values());
    }

    /**
     * Checks if a PBM has any categories.
     * 
     * @param pbm The PBM to check
     * @return true if the PBM has categories, false otherwise
     */
    public boolean hasCategories(Pbm pbm) {
        return pbm.getCategories() != null && !pbm.getCategories().isEmpty();
    }
}
