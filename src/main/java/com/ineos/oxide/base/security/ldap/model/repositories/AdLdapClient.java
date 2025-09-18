package com.ineos.oxide.base.security.ldap.model.repositories;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.ineos.oxide.base.security.ldap.model.entities.LdapUser;
import com.ineos.oxide.base.services.HasLogger;

@Service
@PropertySource("classpath:application.yaml")
public class AdLdapClient implements HasLogger {
    @Value("${spring.ldap.urls}")
    private String ldapUrl;
    @Value("${spring.ldap.username}")
    private String ldapUser;
    @Value("${spring.ldap.password}")
    private String ldapPassword;
    @Value("${spring.ldap.base}")
    private String ldapBase;
    @Value("${spring.ldap.filter}")
    private String ldapFilter;

    public boolean userExists(String username) {
        Hashtable<String, String> env = getEnvInfoTable();

        try {
            DirContext ctx = new InitialDirContext(env);
            String filter = String.format(ldapFilter, username);
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = ctx.search(ldapBase, filter, controls);
            boolean found = results.hasMore();
            ctx.close();
            return found;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Authenticate a user with username and password
    public boolean authenticate(String username, String password) {
        // First, search for the user's DN
        String userDn = null;
        Hashtable<String, String> env = getEnvInfoTable();
        String filter = String.format(ldapFilter, username);
        getLogger().debug("LDAP search base: {}, filter: {}", ldapBase, filter);
        try {
            DirContext ctx = new InitialDirContext(env);
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = ctx.search(ldapBase, filter, controls);
            if (results.hasMore()) {
                SearchResult result = results.next();
                userDn = result.getNameInNamespace();
            }
            ctx.close();
        } catch (Exception e) {
            getLogger().error("Error finding user DN for: " + filter, e);
            return false;
        }

        if (userDn == null) {
            return false;
        }

        // User has been fout now try to see is his password is correct.
        Hashtable<String, String> authEnv = new Hashtable<>();
        authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        authEnv.put(Context.PROVIDER_URL, ldapUrl);
        authEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        authEnv.put(Context.SECURITY_PRINCIPAL, userDn);
        authEnv.put(Context.SECURITY_CREDENTIALS, password);

        try {
            DirContext userCtx = new InitialDirContext(authEnv);
            userCtx.close();
            return true;
        } catch (Exception e) {
            // Authentication failed
            getLogger().error("Error finding user DN for: " + userDn, e);
            return false;
        }
    }

    public LdapUser getUserInfo(String username) {
        Hashtable<String, String> env = getEnvInfoTable();

        try {
            DirContext ctx = new InitialDirContext(env);
            String filter = String.format(ldapFilter, username);
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[] { "givenName", "cn", "mail", "sn", "displayName" });

            NamingEnumeration<SearchResult> results = ctx.search(ldapBase, filter, controls);
            if (results.hasMore()) {
                SearchResult result = results.next();
                String givenName = null;
                String cn = null;
                String mail = null;
                String surname = null;
                String displayName = null;

                if (result.getAttributes().get("givenName") != null) {
                    givenName = (String) result.getAttributes().get("givenName").get();
                }
                if (result.getAttributes().get("displayName") != null) {
                    displayName = (String) result.getAttributes().get("displayName").get();
                }
                if (result.getAttributes().get("sn") != null) {
                    surname = (String) result.getAttributes().get("sn").get();
                }
                if (result.getAttributes().get("cn") != null) {
                    cn = (String) result.getAttributes().get("cn").get();
                }
                if (result.getAttributes().get("mail") != null) {
                    mail = (String) result.getAttributes().get("mail").get();
                }
                ctx.close();
                return new LdapUser(givenName, cn, mail, surname, displayName);
            }
            ctx.close();
        } catch (Exception e) {
            getLogger().error(username, e);
        }
        return null;
    }

    private Hashtable<String, String> getEnvInfoTable() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapUser);
        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
        logEnvInfoTable(env);
        return env;
    }

    public void printLdapTree() {
        Hashtable<String, String> env = getEnvInfoTable();
        try {
            DirContext ctx = new InitialDirContext(env);
            printSubtree(ctx, ldapBase, 0);
            ctx.close();
        } catch (Exception e) {
            getLogger().error("Error printing LDAP tree", e);
        }
    }

    private void printSubtree(DirContext ctx, String base, int level) throws Exception {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        NamingEnumeration<SearchResult> results = ctx.search(base, "(objectClass=*)", controls);

        while (results.hasMore()) {
            SearchResult result = results.next();
            String dn = result.getNameInNamespace();
            // Indent according to level
            String indent = "  ".repeat(level);
            System.out.println(indent + dn);
            // Recursively print children
            printSubtree(ctx, dn, level + 1);
        }
    }

    private void logEnvInfoTable(Hashtable<String, String> env) {
        StringBuilder sb = new StringBuilder("LDAP Environment:\n");
        env.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
        getLogger().debug(sb.toString());
    }
}