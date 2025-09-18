package com.ineos.oxide.pbmgids.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ineos.oxide.pbmgids.model.entities.Norm;

public interface NormRepository extends JpaRepository<Norm, Integer> {
}
