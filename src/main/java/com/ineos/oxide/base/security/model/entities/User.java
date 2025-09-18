package com.ineos.oxide.base.security.model.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

public interface User extends UserDetails {
    public static final String[] COLUMNS = { "username", "displayName", "email", "roles", "enabled", "lastLoginDate" };

    String getDisplayName();

    String getEmail();

    LocalDateTime getLastLoginDate();

    List<ApplicationRole> getRoles();

    void setLastLoginDate(LocalDateTime now);

    void setEnabled(Boolean value);

}
