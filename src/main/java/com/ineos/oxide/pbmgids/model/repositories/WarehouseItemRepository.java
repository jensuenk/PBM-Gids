package com.ineos.oxide.pbmgids.model.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ineos.oxide.pbmgids.model.entities.WarehouseItem;

public interface WarehouseItemRepository extends JpaRepository<WarehouseItem, Integer> {
    List<WarehouseItem> findByPbm_Id(Integer pbmId);
}
