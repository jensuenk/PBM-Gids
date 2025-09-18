package com.ineos.oxide.base.security.ui.panels;

import java.util.ArrayList;
import java.util.List;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.model.entities.DBUser;
import com.ineos.oxide.base.security.model.entities.User;
import com.ineos.oxide.base.security.services.ServiceUsers;
import com.ineos.oxide.base.services.HasResources;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.CrudListenerIOS;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.GridPanelCrudAndFilterdAndDownloadIOS;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.GridPanelCrudAndFilterdAndDownloadIOS.CrudMode;
import com.ineos.oxide.base.ui.services.ServiceCookies;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

public class UserPanel extends VerticalLayout implements HasResources {
    private static final long serialVersionUID = 1L;
    private User user;
    private ServiceCookies serviceCookies;
    private ServiceUsers serviceUsers;
    private List<ApplicationRole> rolesToappend;
    private Binder<User> binder;
    private TextField displayNameField;
    private TextField emailField;
    private TextField usernameField;
    private List<ApplicationRole> currentRoles; // Track current roles state
    private GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> rolesGrid;

    public enum UserPanelMode {
        VIEW, EDIT, ADD
    }

    private UserPanelMode mode = UserPanelMode.VIEW;

    public UserPanel(UserPanelMode mode, User user, ServiceCookies serviceCookies,
            List<ApplicationRole> rolesToappend, ServiceUsers serviceUsers) {
        this.mode = mode;
        this.rolesToappend = rolesToappend;
        this.user = user;
        this.serviceCookies = serviceCookies;
        this.serviceUsers = serviceUsers;

        // Initialize currentRoles with a mutable copy of user's roles
        if (user != null && user.getRoles() != null) {
            this.currentRoles = new ArrayList<>(user.getRoles());
        } else {
            this.currentRoles = new ArrayList<>();
        }

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addUserForm();
    }

    public UserPanel(UserPanelMode view, User user2, ServiceCookies serviceCookies2) {
        this(view, user2, serviceCookies2, null, null);
        // This constructor is for cases where rolesToappend is not needed
        // You can call the other constructor with null if rolesToappend is not required
    }

    public void addUserForm() {
        binder = new Binder<>(User.class);

        usernameField = new TextField(getProperty("label.username"));
        usernameField.setWidthFull();
        usernameField.setReadOnly(mode != UserPanelMode.ADD);
        binder.forField(usernameField)
                .asRequired(getProperty("error.username.required"))
                .withValidator(
                        username -> validateUsername(username),
                        getProperty("error.username.not.unique"))
                .bind(User::getUsername, (user, value) -> {
                    if (user instanceof DBUser dbUser) {
                        dbUser.setUsername(value);
                    }
                });
        add(usernameField);

        displayNameField = new TextField(getProperty("label.displayName"));
        displayNameField.setWidthFull();
        displayNameField.setReadOnly(mode != UserPanelMode.ADD);
        binder.forField(displayNameField)
                .bind(User::getDisplayName, (user, value) -> {
                    if (user instanceof DBUser dbUser) {
                        dbUser.setDisplayName(value);
                    }
                });
        add(displayNameField);

        emailField = new TextField(getProperty("label.email"));
        emailField.setWidthFull();
        emailField.setReadOnly(mode != UserPanelMode.ADD);
        binder.forField(emailField)
                .bind(User::getEmail, (user, value) -> {
                    if (user instanceof DBUser dbUser) {
                        dbUser.setEmail(value);
                    }
                });
        add(emailField);

        if (mode != UserPanelMode.ADD) {
            DateTimePicker lastLoginField = new DateTimePicker(getProperty("label.lastLogin"));
            lastLoginField.setValue(user == null || user.getLastLoginDate() == null ? null : user.getLastLoginDate());
            lastLoginField.setReadOnly(true);
            add(lastLoginField);
        }

        if (user != null) {
            binder.readBean(user);
        }

        add(new Span(getProperty("label.roles")));
        rolesGrid = new GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole>(
                "a573149f-386c-4dce-9074-760c4c3f83d5",
                serviceCookies,
                isPanelReadOnly() ? CrudMode.NONE : CrudMode.CD,
                currentRoles,
                ApplicationRole.COLUMNS) {

            @Override
            protected Class<ApplicationRole> getObjectClass() {
                return ApplicationRole.class;
            }

        };
        if (!isPanelReadOnly()) {
            rolesGrid.addCrudListener(getCrudListenerImplementation());
        }

        add(rolesGrid);
    }

    private CrudListenerIOS<ApplicationRole> getCrudListenerImplementation() {
        return new CrudListenerIOS<ApplicationRole>() {
            @Override
            public void deleteObject(GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel) {
                ApplicationRole role = gridPanel.getGrid().asSingleSelect().getValue();
                if (role != null) {
                    currentRoles.remove(role);
                    gridPanel.getGrid().asSingleSelect().clear();
                    gridPanel.getDataProvider().refreshAll();
                }
            }

            @Override
            public void addObject(GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel) {
                ComboBox<ApplicationRole> rolesComboBox = new ComboBox<>(getProperty("label.add.role"));
                rolesComboBox.setItems(showOnlyRolesNotAddedToUser(rolesToappend));
                Dialog confirmationDialog = new Dialog(getProperty("dialog.title.add.role"));
                confirmationDialog.add(rolesComboBox);
                Button confirmButton = new Button(getProperty("button.confirm"), e -> {
                    ApplicationRole selectedRole = rolesComboBox.getValue();
                    if (selectedRole != null && !currentRoles.contains(selectedRole)) {
                        currentRoles.add(selectedRole);
                        gridPanel.getGrid().asSingleSelect().setValue(selectedRole);
                        gridPanel.getDataProvider().refreshAll();
                    }
                    confirmationDialog.close();
                });
                Button cancelButton = new Button(getProperty("button.cancel"), e -> confirmationDialog.close());
                confirmationDialog.getFooter().add(confirmButton, cancelButton);
                confirmationDialog.open();
            }

            private List<ApplicationRole> showOnlyRolesNotAddedToUser(List<ApplicationRole> rolesToappend) {
                if (rolesToappend == null || rolesToappend.isEmpty()) {
                    return List.of();
                }
                return rolesToappend.stream()
                        .filter(role -> !currentRoles.contains(role))
                        .toList();
            }
        };

    }

    public boolean isPanelReadOnly() {
        return mode == UserPanelMode.VIEW;
    }

    public User getUser() {
        if (mode == UserPanelMode.ADD || mode == UserPanelMode.EDIT) {
            return getChangedItem();
        }
        return user;
    }

    private boolean validateUsername(String username) {
        // For new users, check if username is unique (case-insensitive)
        if (serviceUsers == null || mode != UserPanelMode.ADD) {
            return true;
        }
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return serviceUsers.isUsernameUnique(username.trim());
    }

    public User getChangedItem() throws IllegalStateException {
        if (binder.isValid()) {
            try {
                binder.writeBean(user);
                // Ensure the roles are updated with current roles
                if (user instanceof DBUser dbUser) {
                    dbUser.setRoles(new ArrayList<>(currentRoles));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Form validation failed", e);
            }
        } else {
            binder.validate();
            throw new IllegalStateException("Form validation failed");
        }
        return user;
    }

}
