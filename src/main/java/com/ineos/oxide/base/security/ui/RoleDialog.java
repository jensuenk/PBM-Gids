package com.ineos.oxide.base.security.ui;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.services.ServiceApplicationRole;
import com.ineos.oxide.base.security.ui.panels.RolePanel;
import com.ineos.oxide.base.services.HasResources;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;

public class RoleDialog implements HasResources {
    private ServiceApplicationRole serviceApplicationRole;
    private RoleDialogListener listener;
    private Mode mode;

    private ApplicationRole item;

    public enum Mode {
        ADD,
        EDIT,
        DELETE
    }

    public RoleDialog(Mode add, ApplicationRole item, RoleDialogListener listener,
            ServiceApplicationRole serviceApplicationRole) {
        this.mode = add;
        setItem(item);
        this.serviceApplicationRole = serviceApplicationRole;
        this.listener = listener;
    }

    public void open() {
        Dialog roleDialog = new Dialog(getTitle());
        RolePanel panel = new RolePanel(this.getItem(), getServiceApplicationRole());
        if (mode == Mode.DELETE) {
            Span confirmationMessage = new Span(
                    getProperty("message.delete.role.confirmation", getItem().getName()));
            confirmationMessage.getStyle().set("color", "red");
            confirmationMessage.getStyle().set("font-weight", "bold");
            confirmationMessage.getStyle().set("font-size", "1.2em");

            roleDialog.add(confirmationMessage);
        } else {
            roleDialog.add(panel);
        }

        Button saveButton = new Button(getComfirmationLabel(), e -> {
            try {
                if (getMode() == Mode.ADD) {
                    setItem(getServiceApplicationRole().save(panel.getChangedItem()));
                } else if (getMode() == Mode.EDIT) {
                    setItem(getServiceApplicationRole().save(panel.getChangedItem()));
                } else if (getMode() == Mode.DELETE) {
                    getServiceApplicationRole().delete(getItem());
                }
                getListener().onRoleDialogClosed(getItem(), getMode(), true);
                roleDialog.close();
            } catch (IllegalStateException ex) {
                // Dialog needs to remain open if there is an error
            }
        });
        saveButton.setAutofocus(true);
        Button cancelButton = new Button("Cancel", e -> roleDialog.close());
        roleDialog.getFooter().add(saveButton, cancelButton);
        roleDialog.setWidth("600px");
        roleDialog.setHeight("400px");
        roleDialog.open();
    }

    private String getComfirmationLabel() {
        return switch (getMode()) {
            case ADD -> getProperty("button.add.role");
            case EDIT -> getProperty("button.edit.role");
            case DELETE -> getProperty("button.delete.role");
        };
    }

    private String getTitle() {
        return switch (getMode()) {
            case ADD -> getProperty("title.add.role");
            case EDIT -> getProperty("title.edit.role");
            case DELETE -> getProperty("title.delete.role");
        };
    }

    private ServiceApplicationRole getServiceApplicationRole() {
        return serviceApplicationRole;
    }

    private RoleDialogListener getListener() {
        return listener;
    }

    private ApplicationRole getItem() {
        return item;
    }

    private void setItem(ApplicationRole item) {
        this.item = item;
    }

    private Mode getMode() {
        return mode;
    }

    private void setMode(Mode mode) {
        this.mode = mode;
    }
}
