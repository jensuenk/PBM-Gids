package com.ineos.oxide.base.security.services;

import com.ineos.oxide.base.security.ldap.model.entities.LdapUser;

public interface IneosIdentityProvider {

    LdapUser findByUsername(String name);

    boolean authenticate(String username, String password);

}
