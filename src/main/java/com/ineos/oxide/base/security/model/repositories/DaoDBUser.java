package com.ineos.oxide.base.security.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ineos.oxide.base.security.model.entities.DBUser;

@Repository
public interface DaoDBUser extends JpaRepository<DBUser, Long> {

    DBUser findByUsername(String name);
    // This interface is intentionally left empty. It extends JpaRepository to
    // provide CRUD operations for DBUser entities.
    // Additional custom query methods can be defined here if needed.

}
