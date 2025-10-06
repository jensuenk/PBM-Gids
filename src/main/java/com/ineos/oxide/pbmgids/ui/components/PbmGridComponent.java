package com.ineos.oxide.pbmgids.ui.components;

import java.util.function.Consumer;

import com.ineos.oxide.pbmgids.managers.ComparisonManager;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.renderer.ComponentRenderer;

/**
 * Grid component for displaying PBMs in table format.
 * Handles column setup and rendering logic.
 */
public class PbmGridComponent extends Grid<Pbm> {

    private static final String IMAGE_BASE_URL = "/images/";
    private static final String GRID_IMAGE_SIZE = "64px";

    private ComparisonManager comparisonManager;
    private Consumer<Pbm> onDetailsClick;

    public PbmGridComponent() {
        super(Pbm.class, false);
        setupColumns();
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void setupColumns() {
        // Checkbox column for comparison selection
        addColumn(new ComponentRenderer<>(this::createCompareCheckbox))
                .setHeader("Compare")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setWidth("90px");

        // Image column
        addColumn(new ComponentRenderer<>(this::createImageComponent))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setWidth("80px");

        // Name column
        addColumn(Pbm::getName)
                .setHeader("Name")
                .setAutoWidth(true);

        // Type column
        addColumn(Pbm::getTypeName)
                .setHeader("Type")
                .setAutoWidth(true);

        // Brand column
        addColumn(Pbm::getBrand)
                .setHeader("Brand")
                .setAutoWidth(true);

        // Details button column
        addColumn(new ComponentRenderer<>(this::createDetailsButton))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private Checkbox createCompareCheckbox(Pbm pbm) {
        Checkbox checkbox = new Checkbox();

        if (comparisonManager != null) {
            checkbox.setValue(comparisonManager.isSelected(pbm));
            checkbox.addValueChangeListener(event -> {
                boolean success = comparisonManager.setSelected(pbm, event.getValue());
                if (!success) {
                    // Revert checkbox if selection was rejected
                    checkbox.setValue(false);
                }
            });
        }

        return checkbox;
    }

    private Div createImageComponent(Pbm pbm) {
        Div container = new Div();

        if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
            String imageUrl = getImageUrl(pbm.getImage());
            Image img = new Image(imageUrl, pbm.getName());
            img.setAlt(pbm.getName());
            img.setWidth(GRID_IMAGE_SIZE);
            container.add(img);
        }

        return container;
    }

    private Button createDetailsButton(Pbm pbm) {
        Button btn = new Button("Details");
        btn.addClickListener(ev -> {
            if (onDetailsClick != null) {
                onDetailsClick.accept(pbm);
            }
        });
        return btn;
    }

    private String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // If the path starts with mag_doc, use /static/ prefix, otherwise use /images/
        if (cleanPath.startsWith("mag_doc/")) {
            return "/static/" + cleanPath;
        } else {
            return IMAGE_BASE_URL + cleanPath;
        }
    }

    // Public API

    public void setComparisonManager(ComparisonManager manager) {
        this.comparisonManager = manager;
        // Refresh grid when comparison manager changes
        getDataProvider().refreshAll();
    }

    public void setOnDetailsClick(Consumer<Pbm> callback) {
        this.onDetailsClick = callback;
    }

    public void refreshCheckboxes() {
        getDataProvider().refreshAll();
    }
}