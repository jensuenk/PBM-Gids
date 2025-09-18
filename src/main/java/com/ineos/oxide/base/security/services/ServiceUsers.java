package com.ineos.oxide.base.security.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ineos.oxide.base.security.ldap.model.entities.LdapUser;
import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.model.entities.DBUser;
import com.ineos.oxide.base.security.model.entities.User;
import com.ineos.oxide.base.services.HasLogger;

public abstract class ServiceUsers implements UserDetailsService, AuthenticationProvider, HasLogger {
    private ServiceDBUser serviceDBUser;
    private IneosIdentityProvider serviceIneosIdentityProvider;

    public ServiceUsers(ServiceDBUser serviceDBUser, IneosIdentityProvider serviceIneosIdentityProvider) {
        setServiceDBUser(serviceDBUser);
        setServiceIneosIdentityProvider(serviceIneosIdentityProvider);
    }

    private ServiceDBUser getServiceDBUser() {
        return serviceDBUser;
    }

    private void setServiceDBUser(ServiceDBUser serviceDBUser) {
        this.serviceDBUser = serviceDBUser;
    }

    private IneosIdentityProvider getServiceIneosIdentityProvider() {
        return serviceIneosIdentityProvider;
    }

    private void setServiceIneosIdentityProvider(IneosIdentityProvider serviceIneosIdentityProvider) {
        this.serviceIneosIdentityProvider = serviceIneosIdentityProvider;
    }

    public User findByUsername(String name) throws UsernameNotFoundException {
        LdapUser ldapUser = getServiceIneosIdentityProvider().findByUsername(name);
        DBUser dbUser = getServiceDBUser().findByUsername(name);

        if (ldapUser == null) {
            throw new UsernameNotFoundException(name);
        } else if (dbUser == null || dbUser.getId() == null) {
            return new DBUser(ldapUser);
        } else {
            return dbUser;
        }
    }

    /**
     * This is used to save the user to the database.
     */
    public User save(User entity) {
        if (entity == null)
            return null;
        if (entity instanceof DBUser dbUser) {
            return getServiceDBUser().save(dbUser);
        } else {
            throw new IllegalArgumentException(
                    "Invalid user type expect DBUser got " + entity.getClass().getName());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsername(username);

    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        if (this.serviceIneosIdentityProvider.authenticate(username, password)) {
            // Setting last login date
            try {
                User user = findByUsername(username);
                if (user == null) {
                    throw new UsernameNotFoundException("User not found");
                } else if (!user.isEnabled()) {
                    throw new UsernameNotFoundException("User is disabled");
                } else {
                    user.setLastLoginDate(LocalDateTime.now());
                    this.serviceDBUser.save(user);
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, password,
                            user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(token);
                    return token;
                }
            } catch (Exception e) {
                // Normally this should not happen
                getLogger().error(
                        "Exception : Normally this should not happen. Because this is just retrieving information from a user that is authenticated.",
                        e);
                throw new BadCredentialsException("Authentication failed (Authentication succeeded but user not found)",
                        e);
            }
        } else {
            authentication.setAuthenticated(false);
            throw new BadCredentialsException("Authentication failed");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public String authenticate(String uid, String psw) {
        if (this.serviceIneosIdentityProvider.authenticate(uid, psw)) {
            findByUsername(uid);
            return uid;
        } else {
            throw new BadCredentialsException("Authentication failed");
        }
    }

    public List<User> findAll() {
        List<DBUser> users = getServiceDBUser().findAll();
        // Convert list of DBUser to list of User and return a mutable list
        return new ArrayList<>(users.stream().map(user -> (User) user).toList());
    }

    public String getDatabaseServerName() {
        return getServiceDBUser().getDatabaseServerName();
    }

    public void delete(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user instanceof DBUser dbUser) {
            getServiceDBUser().delete(dbUser);
        } else {
            throw new IllegalArgumentException(
                    "Invalid user type expect DBUser got " + user.getClass().getName());
        }
    }

    public List<ApplicationRole> getAllAsignableRoles() {
        List<ApplicationRole> roles = getServiceDBUser().getAllAsignableRoles();
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException("No roles found in the database");
        }
        return roles;
    }

    public User createNewObject() {
        DBUser dbUser = new DBUser();
        dbUser.setEnabled(true);
        dbUser.setRoles(ApplicationRole.getDefaultApplicationRoles());
        return dbUser;
    }

    public boolean isUsernameUnique(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // Check case-insensitive by converting to lowercase
        String normalizedUsername = username.trim().toLowerCase();

        // Check all existing users for case-insensitive match
        List<User> existingUsers = findAll();
        return existingUsers.stream()
                .noneMatch(user -> user.getUsername() != null &&
                        user.getUsername().toLowerCase().equals(normalizedUsername));
    }

}
