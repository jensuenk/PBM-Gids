package com.ineos.oxide.pbmgids.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ineos.oxide.pbmgids.model.entities.Document;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
}
