package com.ineos.oxide.base.security.model.entities;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.ineos.oxide.base.model.entities.DataAncestor;
import com.ineos.oxide.base.security.ldap.model.entities.LdapUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
public class DBUser extends DataAncestor implements User {
    @ManyToMany(fetch = FetchType.EAGER)
    private List<ApplicationRole> roles;

    @Column(nullable = false, unique = true)
    private String username;

    private String displayName;

    private String email;

    private boolean enabled;

    private String password;

    private LocalDateTime lastLogin;

    public DBUser(LdapUser ldapUser) {
        this.username = ldapUser.getUsername();
        this.displayName = ldapUser.getDisplayName();
        this.email = ldapUser.getEmail();
        this.setEnabled(true);
        this.setRoles(ApplicationRole.getDefaultApplicationRoles());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.roles == null) {
            return List.of();
        }

        return this.roles.stream()
                .map(role -> (GrantedAuthority) () -> role.getName())
                .toList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setLastLogin(LocalDateTime now) {
        // Assuming you have a lastLogin field in your DBUser class
        this.lastLogin = now;
    }

    @Override
    public LocalDateTime getLastLoginDate() {
        return lastLogin;
    }

    @Override
    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLogin = lastLoginDate;
    }

    @Override
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

    public DBUser(String username, String displayname) {
        this.username = username;
        this.displayName = displayname;
        this.enabled = true; // Default to enabled
        this.lastLogin = LocalDateTime.now(); // Set current time as last login
    }

}
