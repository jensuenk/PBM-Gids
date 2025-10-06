package com.ineos.oxide.pbmgids.ui.dialogs;

import java.util.List;

import com.ineos.oxide.pbmgids.model.entities.Category;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.ui.components.PbmContentComponent;
import com.ineos.oxide.pbmgids.ui.components.PbmContentComponent.ContentSection;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;

/**
 * Modal dialog that displays detailed PBM information using tabs.
 * Uses PbmContentComponent for consistent content rendering.
 */
public class PbmDetailsDialog extends Dialog {
    private static final long serialVersionUID = 1L;

    private final CatalogService catalogService;

    public PbmDetailsDialog(CatalogService catalogService) {
        this.catalogService = catalogService;
        setupDialog();
    }

    private void setupDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("900px");
        setHeight("700px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
    }

    /**
     * Opens the dialog displaying details for the specified PBM
     */
    public void showPbm(Pbm pbm) {
        if (pbm == null) {
            return;
        }

        removeAll();

        // Create dialog layout
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();

        // Header section with image and basic info
        HorizontalLayout headerLayout = createHeaderLayout(pbm);

        // Tabbed details section
        TabSheet tabSheet = createTabSheet(pbm);

        // Footer with close button
        HorizontalLayout footerLayout = createFooterLayout();

        dialogLayout.add(headerLayout);
        dialogLayout.addAndExpand(tabSheet);
        dialogLayout.add(footerLayout);

        add(dialogLayout);
        open();
    }

    private HorizontalLayout createHeaderLayout(Pbm pbm) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();

        // Left side - Image
        VerticalLayout imageLayout = new VerticalLayout();
        imageLayout.setWidth("300px");
        imageLayout.setFlexGrow(0);

        if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
            String imageUrl = getImageUrl(pbm.getImage());
            Image img = new Image(imageUrl, pbm.getName());
            img.setAlt(pbm.getName());
            img.setMaxWidth("280px");
            img.setMaxHeight("280px");
            img.getStyle().set("object-fit", "contain");
            imageLayout.add(img);
        } else {
            Div placeholder = new Div();
            placeholder.setText("No Image Available");
            placeholder.setWidth("280px");
            placeholder.setHeight("280px");
            placeholder.getStyle().set("display", "flex");
            placeholder.getStyle().set("align-items", "center");
            placeholder.getStyle().set("justify-content", "center");
            placeholder.getStyle().set("background", "var(--lumo-contrast-10pct)");
            placeholder.getStyle().set("color", "var(--lumo-secondary-text-color)");
            placeholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            imageLayout.add(placeholder);
        }

        // Right side - Basic info
        VerticalLayout basicInfoLayout = new VerticalLayout();
        basicInfoLayout.setSpacing(false);
        basicInfoLayout.setPadding(false);
        basicInfoLayout.setFlexGrow(1);

        // Title
        H2 title = new H2(pbm.getName());
        title.getStyle().set("margin", "0 0 20px 0");
        basicInfoLayout.add(title);

        if (pbm.getBrand() != null && !pbm.getBrand().isBlank()) {
            H4 brandHeader = new H4("Brand");
            brandHeader.getStyle().set("margin", "0 0 5px 0");
            Div brandContent = new Div();
            brandContent.setText(pbm.getBrand());
            brandContent.getStyle().set("margin-bottom", "15px");
            basicInfoLayout.add(brandHeader, brandContent);
        }

        if (pbm.getTypeName() != null && !pbm.getTypeName().isBlank()) {
            H4 typeHeader = new H4("Type");
            typeHeader.getStyle().set("margin", "0 0 5px 0");
            Div typeContent = new Div();
            typeContent.setText(pbm.getTypeName());
            typeContent.getStyle().set("margin-bottom", "15px");
            basicInfoLayout.add(typeHeader, typeContent);
        }

        if (pbm.getCategories() != null && !pbm.getCategories().isEmpty()) {
            H4 categoriesHeader = new H4("Categories");
            categoriesHeader.getStyle().set("margin", "0 0 5px 0");

            FlexLayout categoriesLayout = createCategoryBadges(pbm);
            if (categoriesLayout != null) {
                basicInfoLayout.add(categoriesHeader, categoriesLayout);
            }
        }

        headerLayout.add(imageLayout, basicInfoLayout);
        return headerLayout;
    }

    private TabSheet createTabSheet(Pbm pbm) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.getStyle().set("overflow-y", "auto");

        // Add tabs for each content section that has data
        for (ContentSection section : ContentSection.values()) {
            if (PbmContentComponent.hasContent(pbm, section)) {
                Div content = PbmContentComponent.createContent(pbm, section);
                tabSheet.add(section.getDisplayName(), content);
            }
        }

        return tabSheet;
    }

    private HorizontalLayout createFooterLayout() {
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setPadding(true);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button("Close", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        footerLayout.add(closeButton);

        return footerLayout;
    }

    private String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "/images/placeholder.png";
        }

        // Remove leading slash if present
        String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // If the path starts with mag_doc, use /static/ prefix, otherwise use /images/
        if (cleanPath.startsWith("mag_doc/")) {
            return "/static/" + cleanPath;
        } else {
            return "/images/" + cleanPath;
        }
    }

    /**
     * Clears the dialog content and closes it
     */
    public void clearAndClose() {
        removeAll();
        close();
    }

    private FlexLayout createCategoryBadges(Pbm pbm) {
        FlexLayout layout = new FlexLayout();
        layout.getStyle().set("gap", "5px");
        layout.getStyle().set("flex-wrap", "wrap");

        if (!catalogService.hasCategories(pbm)) {
            return layout;
        }

        List<Category> allCategories = catalogService.getAllCategoriesWithParents(pbm);

        // Deduplicate by ID and name
        java.util.Map<String, Category> uniqueCategories = new java.util.LinkedHashMap<>();
        for (Category category : allCategories) {
            if (category.getId() != null && !uniqueCategories.containsKey(category.getName())) {
                uniqueCategories.put(category.getName(), category);
            }
        }

        List<Category> categories = new java.util.ArrayList<>(uniqueCategories.values());

        // Find root categories
        java.util.Set<Integer> parentIds = new java.util.HashSet<>();
        for (Category category : categories) {
            if (category.getParent() != null) {
                parentIds.add(category.getParent().getId());
            }
        }

        // Create badges
        for (Category category : categories) {
            Span badge = new Span(category.getName());
            if (!parentIds.contains(category.getId())) {
                badge.getElement().getThemeList().add("badge success primary pill");
            } else {
                badge.getElement().getThemeList().add("badge primary pill");
            }
            layout.add(badge);
        }

        return layout;
    }
}