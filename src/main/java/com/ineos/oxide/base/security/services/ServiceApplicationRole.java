package com.ineos.oxide.base.security.services;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.model.repositories.DaoApplicationRole;

@Service
public class ServiceApplicationRole {
    private DaoApplicationRole daoApplicationRole;

    public ServiceApplicationRole(DaoApplicationRole daoApplicationRole) {
        this.daoApplicationRole = daoApplicationRole;
    }

    public List<ApplicationRole> findAll() {
        return daoApplicationRole.findAll();
    }

    public boolean isNameUnique(String name) {
        return daoApplicationRole.findByName(name) == null;
    }

    public void delete(ApplicationRole applicationRole) throws IllegalStateException {
        if (applicationRole != null) {
            // Check if the role is not in use by any user
            if (daoApplicationRole.isRoleInUse(applicationRole)) {
                throw new IllegalStateException("Cannot delete role, it is assigned to one or more users.");
            }
            daoApplicationRole.delete(applicationRole);
        }
    }

    public ApplicationRole createNewObject() {
        ApplicationRole applicationRole = new ApplicationRole();
        if (SecurityUtils.getCurrentUser().isPresent()) {
            applicationRole.setCreatedBy(getCurrentUserName());
        }
        return applicationRole;
    }

    public ApplicationRole save(ApplicationRole changedItem) {
        if (changedItem != null) {
            String currentUser = getCurrentUserName();
            if (changedItem.getId() == null) {
                // New role, set createdBy
                if (SecurityUtils.getCurrentUser().isPresent()) {
                    changedItem.setCreatedBy(currentUser);
                }
            } else {
                // Existing role, set modifiedBy
                if (SecurityUtils.getCurrentUser().isPresent()) {
                    changedItem.setModifiedBy(currentUser);
                }
            }
            return daoApplicationRole.save(changedItem);
        }
        return null;
    }

    private String getCurrentUserName() {
        return SecurityUtils.getCurrentUser()
                .map(UserDetails::getUsername)
                .orElse("unknown");
    }

}
