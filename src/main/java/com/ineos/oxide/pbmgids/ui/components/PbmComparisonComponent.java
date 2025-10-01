package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.ui.components.PbmContentComponent.ContentSection;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Component for comparing multiple PBMs side by side.
 * This component creates accordion sections with content comparison.
 */
public class PbmComparisonComponent extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    public PbmComparisonComponent() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
    }

    /**
     * Shows comparison for the specified PBMs
     */
    public void showComparison(List<Pbm> pbms) {
        removeAll();

        if (pbms == null || pbms.isEmpty()) {
            return;
        }

        // Add images header
        add(createImagesLayout(pbms));

        // Add content sections
        for (ContentSection section : ContentSection.values()) {
            if (PbmContentComponent.anyHasContent(pbms, section)) {
                Details accordion = createAccordion(pbms, section);
                add(accordion);
            }
        }
    }

    private HorizontalLayout createImagesLayout(List<Pbm> pbms) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout pbmLayout = new VerticalLayout();
            pbmLayout.getStyle().set("flex", "1");
            pbmLayout.setAlignItems(Alignment.CENTER);
            pbmLayout.setPadding(true);
            pbmLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            pbmLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

            // PBM name
            H4 nameHeader = new H4(pbm.getName());
            nameHeader.getStyle().set("margin", "0 0 10px 0");
            nameHeader.getStyle().set("text-align", "center");
            pbmLayout.add(nameHeader);

            // PBM image
            if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
                String imageUrl = getImageUrl(pbm.getImage());
                Image img = new Image(imageUrl, pbm.getName());
                img.setAlt(pbm.getName());
                img.setWidth("120px");
                img.setHeight("120px");
                img.getStyle().set("object-fit", "contain");
                pbmLayout.add(img);
            } else {
                Div placeholder = new Div();
                placeholder.setText("No Image");
                placeholder.setWidth("120px");
                placeholder.setHeight("120px");
                placeholder.getStyle().set("display", "flex");
                placeholder.getStyle().set("align-items", "center");
                placeholder.getStyle().set("justify-content", "center");
                placeholder.getStyle().set("background", "var(--lumo-contrast-10pct)");
                placeholder.getStyle().set("color", "var(--lumo-secondary-text-color)");
                placeholder.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
                pbmLayout.add(placeholder);
            }

            // Additional info
            if (pbm.getBrand() != null && !pbm.getBrand().isBlank()) {
                Div brandDiv = new Div();
                brandDiv.setText("Brand: " + pbm.getBrand());
                brandDiv.getStyle().set("font-size", "var(--lumo-font-size-s)");
                brandDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                brandDiv.getStyle().set("text-align", "center");
                pbmLayout.add(brandDiv);
            }

            if (pbm.getTypeName() != null && !pbm.getTypeName().isBlank()) {
                Div typeDiv = new Div();
                typeDiv.setText("Type: " + pbm.getTypeName());
                typeDiv.getStyle().set("font-size", "var(--lumo-font-size-s)");
                typeDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                typeDiv.getStyle().set("text-align", "center");
                pbmLayout.add(typeDiv);
            }

            layout.add(pbmLayout);
        }

        return layout;
    }

    private Details createAccordion(List<Pbm> pbms, ContentSection section) {
        Details accordion = new Details(section.getDisplayName());
        accordion.setWidthFull();

        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = PbmContentComponent.createComparisonColumn(pbm, section);
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "/images/placeholder.png";
        }

        // Remove leading slash if present
        String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // Construct the static resource URL
        return "/images/" + cleanPath;
    }
}