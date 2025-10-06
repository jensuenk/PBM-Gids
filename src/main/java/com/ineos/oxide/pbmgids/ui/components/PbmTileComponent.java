package com.ineos.oxide.pbmgids.ui.components;

import java.util.function.Consumer;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Component for rendering a PBM as a tile card.
 * Handles the visual representation and user interactions for individual PBM
 * tiles.
 */
public class PbmTileComponent extends Div {

    private static final String IMAGE_BASE_URL = "/images/";
    private static final String TILE_WIDTH = "200px";
    private static final String IMAGE_SIZE = "120px";

    private final Pbm pbm;
    private final Checkbox selectCheckbox;
    private final Button detailsButton;

    // Callbacks
    private Consumer<Boolean> onSelectionChange;
    private Runnable onDetailsClick;

    public PbmTileComponent(Pbm pbm) {
        this.pbm = pbm;
        this.selectCheckbox = new Checkbox("Compare");
        this.detailsButton = new Button("Details", VaadinIcon.INFO_CIRCLE.create());

        setupCard();
        setupContent();
        setupActions();
    }

    private void setupCard() {
        addClassName("pbm-tile-card");
        getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "16px")
                .set("background", "var(--lumo-base-color)")
                .set("width", TILE_WIDTH)
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("text-align", "center");
    }

    private void setupContent() {
        add(createImageContainer(), createTitle(), createActionsLayout());
    }

    private Div createImageContainer() {
        Div container = new Div();
        container.getStyle()
                .set("width", IMAGE_SIZE)
                .set("height", IMAGE_SIZE)
                .set("margin-bottom", "12px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "hidden");

        if (hasValidImage()) {
            container.add(createImage());
        } else {
            container.add(createPlaceholder());
        }

        return container;
    }

    private boolean hasValidImage() {
        return pbm.getImage() != null && !pbm.getImage().isBlank();
    }

    private Image createImage() {
        String imageUrl = getImageUrl(pbm.getImage());
        Image img = new Image(imageUrl, pbm.getName());
        img.setAlt(pbm.getName());
        img.getStyle()
                .set("max-width", "100%")
                .set("max-height", "100%")
                .set("width", "auto")
                .set("height", "auto")
                .set("object-fit", "contain");
        return img;
    }

    private Div createPlaceholder() {
        Div placeholder = new Div();
        placeholder.getStyle()
                .set("font-size", "48px")
                .set("color", "var(--lumo-contrast-30pct)");
        placeholder.setText("📦");
        return placeholder;
    }

    private H3 createTitle() {
        H3 title = new H3(pbm.getName());
        title.getStyle()
                .set("margin", "0 0 16px 0")
                .set("font-size", "1.1em")
                .set("color", "var(--lumo-body-text-color)")
                .set("cursor", "pointer");
        title.addClickListener(e -> {
            if (onDetailsClick != null) {
                onDetailsClick.run();
            }
        });
        return title;
    }

    private HorizontalLayout createActionsLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(selectCheckbox, detailsButton);
        return layout;
    }

    private void setupActions() {
        // Setup checkbox - set value first to avoid triggering listener during
        // initialization
        selectCheckbox.addValueChangeListener(event -> {
            if (onSelectionChange != null) {
                onSelectionChange.accept(event.getValue());
            }
        });

        // Setup details button
        detailsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        detailsButton.addClickListener(e -> {
            if (onDetailsClick != null) {
                onDetailsClick.run();
            }
        });
    }

    // Public API methods

    public void setSelected(boolean selected) {
        selectCheckbox.setValue(selected);
    }

    public boolean isSelected() {
        return selectCheckbox.getValue();
    }

    public void setOnSelectionChange(Consumer<Boolean> callback) {
        this.onSelectionChange = callback;
    }

    public void setOnDetailsClick(Runnable callback) {
        this.onDetailsClick = callback;
    }

    public Pbm getPbm() {
        return pbm;
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
}