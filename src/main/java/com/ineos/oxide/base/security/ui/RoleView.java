package com.ineos.oxide.base.security.ui;

import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.services.ServiceApplicationRole;
import com.ineos.oxide.base.security.ui.RoleDialog.Mode;
import com.ineos.oxide.base.ui.IneosAppLayout;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.CrudListenerIOS;
import com.ineos.oxide.base.ui.helpers.vaadin.grids.GridPanelCrudAndFilterdAndDownloadIOS;
import com.ineos.oxide.base.ui.services.ServiceCookies;
import com.ineos.oxide.pbmgids.ui.MainView;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import jakarta.annotation.security.RolesAllowed;

@Route(value = RoleView.ROUTE, layout = MainView.class)
@UIScope
@RolesAllowed(value = { "ROLE_ADMIN" })
@PropertySource("classpath:application.yaml")
public class RoleView extends IneosAppLayout implements HasDynamicTitle {
    protected static final String ROUTE = "specific_role_view";
    private transient ServiceApplicationRole serviceApplicationRole;
    private transient ServiceCookies serviceCookies;

    public RoleView(ServiceApplicationRole serviceApplicationRole,
            ServiceCookies serviceCookies) {
        this.serviceApplicationRole = serviceApplicationRole;
        this.serviceCookies = serviceCookies;
        buildUi();
    }

    private void buildUi() {
        GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel = new GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole>(
                "a1da5f87-202e-44e1-b84b-aca42482fb58",
                serviceCookies,
                GridPanelCrudAndFilterdAndDownloadIOS.CrudMode.CUD,
                serviceApplicationRole.findAll(),
                "name", "description", "enabled") {

            @Override
            protected Class<ApplicationRole> getObjectClass() {
                return ApplicationRole.class;
            }

            @Override
            protected void setCustomKolomProperties(Column<ApplicationRole> kolom) {
                if (kolom.getKey().equals("enabled")) {
                    kolom.setRenderer(new ComponentRenderer<>(item -> {
                        Checkbox checkbox = new Checkbox();
                        checkbox.setValue(item.isEnabled());
                        checkbox.setReadOnly(true);
                        return checkbox;
                    }));
                }
            }
        };
        gridPanel.addCrudListener(getCrudImplmentation());
        gridPanel.getThemeList().clear();
        gridPanel.getThemeList().add("spacing-s");
        this.setContent(gridPanel);
    }

    private CrudListenerIOS<ApplicationRole> getCrudImplmentation() {
        return new CrudListenerIOS<ApplicationRole>() {

            @Override
            public void addObject(GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel) {
                // Add new item to the grid
                ApplicationRole newItem = serviceApplicationRole.createNewObject();
                new RoleDialog(
                        RoleDialog.Mode.ADD,
                        newItem,
                        getRoleDialogListnerImplementation(gridPanel, newItem), serviceApplicationRole).open();

            }

            private RoleDialogListener getRoleDialogListnerImplementation(
                    GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel, ApplicationRole newItem) {
                return (item, mode, success) -> {
                    if (mode == Mode.ADD && success) {
                        gridPanel.getDataProvider().getItems().add(item);
                        gridPanel.getDataProvider().refreshAll();
                        gridPanel.selectItem(item);
                    } else if (mode == Mode.EDIT && success) {
                        gridPanel.getDataProvider().refreshItem(item);
                        gridPanel.getDataProvider().refreshAll();
                        gridPanel.selectItem(item);
                    } else if (mode == Mode.DELETE && success) {
                        gridPanel.getDataProvider().getItems().remove(item);
                        gridPanel.getDataProvider().refreshAll();
                    }
                };
            }

            @Override
            public void deleteObject(GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel) {
                // Delete selected item but ask for comfirmation
                ApplicationRole selectedItem = gridPanel.getGrid().asSingleSelect().getValue();
                if (selectedItem != null) {
                    try {
                        serviceApplicationRole.delete(selectedItem);
                        gridPanel.getDataProvider().getItems().remove(selectedItem);
                        gridPanel.getDataProvider().refreshAll();
                        gridPanel.selectLastItem();
                    } catch (Exception e) {
                        // Show error message to user
                        ConfirmDialog confirmDialog = new ConfirmDialog(getProperty("error.title"),
                                getProperty("error.delete.role.in.use"), getProperty("button.ok"),
                                event -> {
                                    // Do nothing on cancel
                                });
                        confirmDialog.setCancelable(true);
                        confirmDialog.setCloseOnEsc(true);
                        confirmDialog.open();
                    }
                }
            }

            @Override
            public void editObject(GridPanelCrudAndFilterdAndDownloadIOS<ApplicationRole> gridPanel) {
                // Set selected toggle item to the editor
                ApplicationRole selectedItem = gridPanel.getGrid().asSingleSelect().getValue();
                new RoleDialog(
                        RoleDialog.Mode.EDIT,
                        selectedItem,
                        getRoleDialogListnerImplementation(gridPanel, selectedItem), serviceApplicationRole).open();
            }

        };
    }

    @Override
    protected String getRoute() {
        return ROUTE;
    }

    @Override
    public String getPageTitle() {
        return getProperty("menu.title");
    }

}
