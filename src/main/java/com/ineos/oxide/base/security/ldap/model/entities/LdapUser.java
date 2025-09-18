package com.ineos.oxide.base.security.ldap.model.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class LdapUser {

    public LdapUser(String givenName, String cn, String mail, String surname, String displayName) {
        this.username = cn;
        this.givenName = givenName;
        this.email = mail;
        this.sureName = surname;
        this.displayName = displayName;
    }

    private String username;
    private String givenName;
    private String sureName;
    private String displayName;
    private String email;

}
