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
}
