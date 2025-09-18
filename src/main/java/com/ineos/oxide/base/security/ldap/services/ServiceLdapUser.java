package com.ineos.oxide.base.security.ldap.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ineos.oxide.base.security.ldap.model.entities.LdapUser;
import com.ineos.oxide.base.security.ldap.model.repositories.AdLdapClient;
import com.ineos.oxide.base.security.services.IneosIdentityProvider;

@Service
public class ServiceLdapUser implements IneosIdentityProvider {

    private AdLdapClient userRepository;

    public ServiceLdapUser(@Autowired AdLdapClient userRepository) {
        this.userRepository = userRepository;
    }

    public boolean authenticate(String u, String p) {
        return userRepository.authenticate(u, p);
    }

    public Boolean exists(String u) {
        return userRepository.userExists(u);
    }

    public LdapUser findByUsername(String name) {
        return userRepository.getUserInfo(name);
    }
}
