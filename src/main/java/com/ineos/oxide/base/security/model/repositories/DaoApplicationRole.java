package com.ineos.oxide.base.security.model.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;

@Repository
public interface DaoApplicationRole extends JpaRepository<ApplicationRole, Long> {

    ApplicationRole findByName(String name);

    List<ApplicationRole> findAllByEnabled(boolean enabled);

    @Query("SELECT (COUNT(dr) > 0) FROM DBUser dr JOIN dr.roles ar WHERE ar = ?1")
    boolean isRoleInUse(ApplicationRole role);

}
