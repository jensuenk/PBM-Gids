package com.ineos.oxide.base.security.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.model.entities.DBUser;
import com.ineos.oxide.base.security.model.entities.User;
import com.ineos.oxide.base.security.model.repositories.DaoApplicationRole;
import com.ineos.oxide.base.security.model.repositories.DaoDBUser;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ServiceDBUser {
    private DaoDBUser daoDBUser;
    private DaoApplicationRole daoRole;
    @PersistenceContext
    private EntityManager entityManager;

    public ServiceDBUser(DaoDBUser daoDBUser, DaoApplicationRole daoRole) {
        this.daoDBUser = daoDBUser;
        this.daoRole = daoRole;
    }

    public User save(User entity) {
        if (entity instanceof User) {
            return save((DBUser) entity);
        }
        throw new IllegalArgumentException("Invalid user type");
    }

    public DBUser save(DBUser entity) {
        // If this the first user in the database, ensure that all roles are set
        if (entity.getId() == null && daoDBUser.count() == 0) {
            entity.setRoles(ApplicationRole.getDefaultApplicationRolesAdmin());
        } else if (entity.getId() == null && daoDBUser.count() > 0) {
            entity.setRoles(ApplicationRole.getDefaultApplicationRoles());
        }
        // First Save roles if they are not already saved
        if (entity.getRoles() != null) {
            List<ApplicationRole> savedApplicationRoles = new ArrayList<>();
            entity.getRoles().forEach(role -> {
                if (role.getId() == null) {
                    ApplicationRole roleInDB = daoRole.findByName(role.getName());
                    savedApplicationRoles.add(roleInDB != null ? roleInDB : daoRole.save(role));
                } else {
                    savedApplicationRoles.add(role);
                }
            });
            entity.setRoles(savedApplicationRoles);
        }

        return daoDBUser.saveAndFlush(entity);
    }

    public List<DBUser> findAll() {
        return daoDBUser.findAll();
    }

    public long count() {
        return daoDBUser.count();
    }

    public void delete(DBUser entity) {
        daoDBUser.delete(entity);
    }

    public DBUser findByUsername(String name) {
        // Try to find user if not found create user with no roles.
        DBUser user = daoDBUser.findByUsername(name);
        if (user == null) {
            user = new DBUser();
            user.setUsername(name);
            user.setEnabled(true);
            user.setRoles(ApplicationRole.getDefaultApplicationRoles());
        }
        return user;
    }

    public String getDatabaseServerName() {
        try {
            var session = entityManager.unwrap(org.hibernate.Session.class);
            return session.doReturningWork(connection -> {
                try {
                    var meta = connection.getMetaData();
                    String url = meta.getURL();
                    // Extract host from URL (jdbc:mysql://host:port/database)
                    String server = url.replaceFirst("jdbc:mysql://", "");
                    int colonIdx = server.indexOf(":");
                    if (colonIdx > 0) {
                        server = server.substring(0, colonIdx);
                    }
                    int slashIdx = server.indexOf("/");
                    if (slashIdx > 0) {
                        server = server.substring(0, slashIdx);
                    }
                    String dbName = connection.getCatalog();
                    return server + ":" + dbName;
                } catch (Exception e) {
                    return "Unknown";
                }
            });
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public List<ApplicationRole> getAllAsignableRoles() {
        List<ApplicationRole> roles = daoRole.findAllByEnabled(true);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException("No roles found in the database");
        }
        return roles;
    }
}
