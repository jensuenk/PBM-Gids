package com.ineos.oxide.base.security.ui.panels;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;
import com.ineos.oxide.base.security.services.ServiceApplicationRole;
import com.ineos.oxide.base.services.HasResources;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

public class RolePanel extends VerticalLayout implements HasResources {
    private ApplicationRole item;
    private ServiceApplicationRole serviceApplicationRole;
    private Binder<ApplicationRole> binder;

    public RolePanel(ApplicationRole item, ServiceApplicationRole serviceApplicationRole) {
        this.item = item;
        this.serviceApplicationRole = serviceApplicationRole;
        setSizeFull();
        buidUi();
    }

    private void buidUi() {
        binder = new Binder<>(ApplicationRole.class);
        TextField nameField = new TextField(getProperty("label.name"));
        binder.forField(nameField)
                .asRequired(getProperty("error.name.required"))
                .withValidator(
                        name -> validateName(name),
                        getProperty("error.name.not.unique"))
                .bind(ApplicationRole::getName, ApplicationRole::setName);
        TextField descriptionField = new TextField(getProperty("label.description"));
        descriptionField.setWidthFull();
        binder.forField(descriptionField)
                .bind(ApplicationRole::getDescription, ApplicationRole::setDescription);
        binder.readBean(item);
        this.add(nameField, descriptionField);
    }

    private boolean validateName(String name) {
        if (item.getName() == null || !item.getName().equals(name)) {
            return serviceApplicationRole.isNameUnique(name);
        }
        return true;
    }

    public ApplicationRole getChangedItem() throws IllegalStateException {
        if (binder.isValid()) {
            try {
                binder.writeBean(item);
            } catch (Exception e) {
                throw new IllegalStateException(getProperty("error.form.invalid"), e);
            }
        } else {
            binder.validate();
            throw new IllegalStateException(getProperty("error.form.invalid"));
        }
        return item;
    }
}
