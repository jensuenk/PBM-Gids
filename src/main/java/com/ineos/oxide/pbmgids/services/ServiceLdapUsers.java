package com.ineos.oxide.pbmgids.services;

import org.springframework.stereotype.Service;

import com.ineos.oxide.base.security.ldap.services.ServiceLdapUser;
import com.ineos.oxide.base.security.services.ServiceDBUser;
import com.ineos.oxide.base.security.services.ServiceUsers;

@Service
public final class ServiceLdapUsers extends ServiceUsers {

    public ServiceLdapUsers(ServiceDBUser serviceDBUser, ServiceLdapUser serviceIneosIdentityProvider) {
        super(serviceDBUser, serviceIneosIdentityProvider);

    }

}
