package com.ineos.oxide.base.security.model.entities;

import java.util.List;

import com.ineos.oxide.base.model.entities.AuditableDataAncestor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity(name = "roles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ApplicationRole extends AuditableDataAncestor {

    public static final String[] COLUMNS = { "name", "description" };
    @Column(unique = true, nullable = false)
    private String name;
    private String description;
    private boolean enabled = true;

    public ApplicationRole(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    public static List<ApplicationRole> getDefaultApplicationRoles() {
        return List.of(
                new ApplicationRole("ROLE_USER", "Default role for regular users"));
    }

    public static List<ApplicationRole> getDefaultApplicationRolesAdmin() {
        return List.of(
                new ApplicationRole("ROLE_USER", "Default role for regular users"),
                new ApplicationRole("ROLE_ADMIN", "Administrator role with full access"),
                new ApplicationRole("ROLE_EDITOR", "Editor role with limited access"),
                new ApplicationRole("ROLE_VIEWER", "Viewer role with view only access"));
    }

    @Override
    public String toString() {
        return name;
    }

}
