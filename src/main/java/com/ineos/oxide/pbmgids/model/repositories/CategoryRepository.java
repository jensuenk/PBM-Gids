package com.ineos.oxide.pbmgids.model.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ineos.oxide.pbmgids.model.entities.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Query(value = "select * from category where parent_id = 0 or parent_id is null order by name", nativeQuery = true)
    List<Category> findRootCategories();

    List<Category> findByParent_IdOrderByNameAsc(Integer parentId);
}
