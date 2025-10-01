package com.ineos.oxide.pbmgids.model.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ineos.oxide.pbmgids.model.entities.Pbm;

public interface PbmRepository extends JpaRepository<Pbm, Integer> {
    List<Pbm> findByNameContainingIgnoreCase(String name);

    @Query("select p from Pbm p join p.categories c where c.id = :categoryId")
    List<Pbm> findByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("select distinct p from Pbm p join p.categories c left join fetch p.categories left join fetch p.documents left join fetch p.norms left join fetch p.warehouseItems where c.id = :categoryId")
    List<Pbm> findByCategoryIdWithRelations(@Param("categoryId") Integer categoryId);

    @Query("select p from Pbm p left join fetch p.categories left join fetch p.documents left join fetch p.norms left join fetch p.warehouseItems where p.id = :id")
    Optional<Pbm> findDetailedById(@Param("id") Integer id);
}
