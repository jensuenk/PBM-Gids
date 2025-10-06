package com.ineos.oxide.pbmgids.ui.dialogs;

import java.util.List;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.ui.components.PbmComparisonComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Modal dialog for comparing multiple PBMs side by side.
 */
public class PbmComparisonDialog extends Dialog {
    private static final long serialVersionUID = 1L;

    private final PbmComparisonComponent comparisonComponent;
    private final H2 titleLabel;

    public PbmComparisonDialog(CatalogService catalogService) {
        setupDialog();

        this.comparisonComponent = new PbmComparisonComponent(catalogService);
        this.titleLabel = new H2("PBM Comparison");

        createLayout();
    }

    private void setupDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("1200px");
        setHeight("800px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
    }

    private void createLayout() {
        // Main container with fixed header/footer
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.setSizeFull();

        // Header section (fixed)
        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setPadding(true);
        headerLayout.setSpacing(false);
        headerLayout.add(titleLabel);

        // Scrollable content area
        Div scrollWrapper = new Div(comparisonComponent);
        scrollWrapper.getStyle().set("overflow-y", "auto");
        scrollWrapper.getStyle().set("flex", "1");
        scrollWrapper.setWidthFull();

        // Footer with close button (fixed)
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setPadding(true);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        footerLayout.add(closeButton);

        mainLayout.add(headerLayout, scrollWrapper, footerLayout);
        mainLayout.setFlexGrow(0, headerLayout);
        mainLayout.setFlexGrow(1, scrollWrapper);
        mainLayout.setFlexGrow(0, footerLayout);

        add(mainLayout);
    }

    /**
     * Shows comparison for the specified PBMs
     */
    public void showComparison(List<Pbm> pbms) {
        if (pbms == null || pbms.isEmpty()) {
            return;
        }

        titleLabel.setText("PBM Comparison (" + pbms.size() + " items)");
        comparisonComponent.showComparison(pbms);
        open();
    }

    /**
     * Clears the comparison and closes the dialog
     */
    public void clearAndClose() {
        comparisonComponent.removeAll();
        close();
    }
}