package com.ineos.oxide.pbmgids.services;

import java.util.List;

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
}
