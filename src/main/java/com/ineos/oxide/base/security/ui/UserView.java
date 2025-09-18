package com.ineos.oxide.base.security.ui;

import com.ineos.oxide.base.security.model.entities.User;
import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.security.services.ServiceUsers;
import com.ineos.oxide.base.security.ui.panels.UserPanel;
import com.ineos.oxide.base.ui.IneosAppLayout;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.CrudListenerIOS;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.GridPanelCrudAndFilterdAndDownloadIOS;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.GridPanelCrudAndFilterdAndDownloadIOS.CrudMode;
import com.ineos.oxide.base.ui.services.ServiceCookies;
import com.ineos.oxide.pbmgids.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import jakarta.annotation.security.RolesAllowed;

@Route(value = UserView.ROUTE, layout = MainView.class)
@UIScope
@PageTitle(value = "Users")
@RolesAllowed(value = { "ROLE_ADMIN" })
public class UserView extends IneosAppLayout {
    private transient ServiceUsers serviceUsers;
    private transient ServiceCookies serviceCookies;

    public static final String ROUTE = "users";

    public UserView(ServiceUsers serviceUsers, ServiceCookies serviceCookies) {
        setServiceUsers(serviceUsers);
        setServiceCookies(serviceCookies);
        buildUi();
    }

    private void buildUi() {
        GridPanelCrudAndFilterdAndDownloadIOS<User> gridPanelCrudAndFilterdAndDownload = new GridPanelCrudAndFilterdAndDownloadIOS<User>(
                "0194d090-fad4-7186-bbcf-3eaf54026db9",
                getServiceCookies(),
                CrudMode.CRUD,
                this.getServiceUsers().findAll(),
                User.COLUMNS) {

            @Override
            protected void setCustomKolomProperties(Column<User> column) {
                /** Here you can set the properties of the columns */
                // Set checkbox for enable
                if (column.getKey().equals("enabled")) {
                    column.setRenderer(new ComponentRenderer<>(user -> {
                        Checkbox checkbox = new Checkbox();
                        checkbox.setValue(user.isEnabled());
                        checkbox.addValueChangeListener(event -> {
                            user.setEnabled(event.getValue());
                            this.getDataProvider().refreshItem(getServiceUsers().save(user));
                        });
                        return checkbox;
                    }));
                }
                column.setHeader(getProperty(UserView.class,
                        String.format("user.column.label.%s", column.getKey())));
            }

            @Override
            protected Class<User> getObjectClass() {
                return User.class;
            }
        };

        gridPanelCrudAndFilterdAndDownload.addCrudListener(getCrudListenerImplementation());

        // ContextMenu contextMenu = new
        // ContextMenu(gridPanelCrudAndFilterdAndDownload.getGrid());
        // contextMenu.addItem("Reset password", e -> {
        // User user =
        // gridPanelCrudAndFilterdAndDownload.getGrid().asSingleSelect().getValue();
        // if (user != null) {
        // PasswordResetModule passwordResetView = new
        // PasswordResetModule(this.serviceUser, user);

        // passwordResetView.addListener((PasswordResetModuleListener) updatedUser -> {
        // gridPanelCrudAndFilterdAndDownload
        // .setDataProvider(new ListDataProvider<>(this.getServiceUser().findAll()));
        // gridPanelCrudAndFilterdAndDownload.getDataProvider().refreshAll();
        // gridPanelCrudAndFilterdAndDownload.selectItem(user);
        // });

        // passwordResetView.showPasswordResetView();
        // }
        // });

        this.setSizeFull();
        this.setContent(gridPanelCrudAndFilterdAndDownload);
    }

    private CrudListenerIOS<User> getCrudListenerImplementation() {
        return new CrudListenerIOS<User>() {
            @Override
            public void deleteObject(GridPanelCrudAndFilterdAndDownloadIOS<User> gridPanel) {
                User user = gridPanel.getGrid().asSingleSelect().getValue();
                if (SecurityUtils.isCurrentUser(user)) {
                    ConfirmDialog confirmationDialog = new ConfirmDialog(getProperty("dialog.title.delete"),
                            getProperty("dialog.message.delete.current.user"),
                            getProperty("dialog.confirm.ok"),
                            e -> {
                            });
                    confirmationDialog.addConfirmListener(e -> confirmationDialog.close());
                    confirmationDialog.open();
                    return;
                }
                if (user != null) {
                    ConfirmDialog confirmationDialog = new ConfirmDialog(getProperty("dialog.title.delete"),
                            String.format(getProperty("dialog.message.delete"), user.getUsername(),
                                    user.getDisplayName()),
                            getProperty("dialog.confirm.delete"),
                            e -> {
                                getServiceUsers().delete(user);
                                gridPanel.getDataProvider().getItems().remove(user);
                                gridPanel.getDataProvider().refreshAll();
                                gridPanel.getGrid().asSingleSelect().clear();
                            });

                    confirmationDialog.open();
                }
            }

            @Override
            public void editObject(GridPanelCrudAndFilterdAndDownloadIOS<User> gridPanel) {
                User user = gridPanel.getGrid().asSingleSelect().getValue();
                if (user != null) {
                    UserPanel userForm = new UserPanel(
                            UserPanel.UserPanelMode.EDIT,
                            user,
                            getServiceCookies(),
                            getServiceUsers().getAllAsignableRoles(),
                            getServiceUsers());
                    Dialog dialog = new Dialog(getProperty("dialog.title.edit"));
                    Span message = new Span(getProperty("dialog.message.edit"));
                    message.getStyle().set("color", "red");
                    message.getStyle().set("font-weight", "bold");
                    dialog.add(message, userForm);
                    Button saveButton = new Button(getProperty("button.save"), e -> {
                        User updatedUser = userForm.getUser();
                        updatedUser = getServiceUsers().save(updatedUser);
                        gridPanel.getDataProvider().refreshAll();
                        dialog.close();
                    });
                    Button cancelButton = new Button(getProperty("button.cancel"), e -> dialog.close());
                    dialog.getFooter().add(saveButton, cancelButton);
                    dialog.open();
                }
            }

            @Override
            public void addObject(GridPanelCrudAndFilterdAndDownloadIOS<User> gridPanel) {
                User newUser = getServiceUsers().createNewObject();
                UserPanel userForm = new UserPanel(
                        UserPanel.UserPanelMode.ADD,
                        newUser,
                        getServiceCookies(),
                        getServiceUsers().getAllAsignableRoles(),
                        getServiceUsers());
                Dialog dialog = new Dialog(getProperty("dialog.title.add"));
                dialog.add(userForm);
                Button saveButton = new Button(getProperty("button.save"), e -> {
                    User savedUser = userForm.getUser();
                    savedUser = getServiceUsers().save(savedUser);
                    gridPanel.getDataProvider().getItems().add(savedUser);
                    gridPanel.getDataProvider().refreshAll();
                    dialog.close();
                    gridPanel.getGrid().asSingleSelect().setValue(savedUser);
                });
                Button cancelButton = new Button(getProperty("button.cancel"), e -> dialog.close());
                dialog.getFooter().add(saveButton, cancelButton);
                dialog.open();
            }

            @Override
            public void viewObject(GridPanelCrudAndFilterdAndDownloadIOS<User> gridPanelCrudAndFilterdAndDownloadIOS) {
            }
        };
    }

    @Override
    protected String getRoute() {
        return ROUTE;
    }

    private ServiceUsers getServiceUsers() {
        return serviceUsers;
    }

    private void setServiceUsers(ServiceUsers serviceUsers) {
        this.serviceUsers = serviceUsers;
    }

    private ServiceCookies getServiceCookies() {
        return serviceCookies;
    }

    private void setServiceCookies(ServiceCookies serviceCookies) {
        this.serviceCookies = serviceCookies;
    }
}
