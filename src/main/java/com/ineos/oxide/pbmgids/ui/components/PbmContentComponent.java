package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;
import java.util.Set;

import com.ineos.oxide.pbmgids.model.entities.Category;
import com.ineos.oxide.pbmgids.model.entities.Document;
import com.ineos.oxide.pbmgids.model.entities.Norm;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.model.entities.WarehouseItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Utility component for creating content sections for PBM information.
 * This centralizes the content creation logic to avoid duplication between
 * single PBM details and comparison views.
 */
public class PbmContentComponent {

    /**
     * Content section identifiers
     */
    public enum ContentSection {
        DESCRIPTION("Description"),
        PROTECTS_AGAINST("Protects Against"),
        DOES_NOT_PROTECT("Does Not Protect"),
        NOTES("Notes"),
        USAGE_INSTRUCTIONS("Usage Instructions"),
        DISTRIBUTION("Distribution"),
        STANDARDS("Standards"),
        WAREHOUSE_ITEMS("Warehouse Items"),
        CATEGORIES("Categories");

        private final String displayName;

        ContentSection(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Checks if a PBM has content for the specified section
     */
    public static boolean hasContent(Pbm pbm, ContentSection section) {
        if (pbm == null)
            return false;

        return switch (section) {
            case DESCRIPTION -> pbm.getDescription() != null && !pbm.getDescription().isBlank();
            case PROTECTS_AGAINST -> pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank();
            case DOES_NOT_PROTECT ->
                pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank();
            case NOTES -> hasNotesContent(pbm);
            case USAGE_INSTRUCTIONS -> hasUsageInstructionsContent(pbm);
            case DISTRIBUTION -> pbm.getDistribution() != null && !pbm.getDistribution().isBlank();
            case STANDARDS -> hasStandardsContent(pbm);
            case WAREHOUSE_ITEMS -> pbm.getWarehouseItems() != null && !pbm.getWarehouseItems().isEmpty();
            case CATEGORIES -> pbm.getCategories() != null && !pbm.getCategories().isEmpty();
        };
    }

    /**
     * Checks if any PBM in the list has content for the specified section
     */
    public static boolean anyHasContent(List<Pbm> pbms, ContentSection section) {
        return pbms.stream().anyMatch(pbm -> hasContent(pbm, section));
    }

    /**
     * Creates content for a single PBM for the specified section
     */
    public static Div createContent(Pbm pbm, ContentSection section) {
        if (pbm == null || !hasContent(pbm, section)) {
            return new Div(new Span("No content available"));
        }

        return switch (section) {
            case DESCRIPTION -> createHtmlContent(pbm.getDescription());
            case PROTECTS_AGAINST -> createHtmlContent(pbm.getProtectsAgainst());
            case DOES_NOT_PROTECT -> createHtmlContent(pbm.getDoesNotProtectAgainst());
            case NOTES -> createNotesContent(pbm);
            case USAGE_INSTRUCTIONS -> createUsageInstructionsContent(pbm);
            case DISTRIBUTION -> createHtmlContent(pbm.getDistribution());
            case STANDARDS -> createStandardsContent(pbm);
            case WAREHOUSE_ITEMS -> createWarehouseContent(pbm);
            case CATEGORIES -> createCategoriesContent(pbm);
        };
    }

    /**
     * Creates a column for comparison view for the specified PBM and section
     */
    public static VerticalLayout createComparisonColumn(Pbm pbm, ContentSection section) {
        VerticalLayout column = new VerticalLayout();
        column.getStyle().set("flex", "1");
        column.setPadding(true);
        column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        column.setWidthFull(); // Ensure full width for proper grid display

        // Add content directly without PBM name header
        if (hasContent(pbm, section)) {
            Div content = createContent(pbm, section);
            content.setWidthFull(); // Ensure content div is also full width
            column.add(content);
        } else {
            Span noContent = new Span("-");
            noContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
            noContent.getStyle().set("font-style", "italic");
            column.add(noContent);
        }

        return column;
    }

    // Private helper methods

    private static boolean hasNotesContent(Pbm pbm) {
        return (pbm.getNotes() != null && !pbm.getNotes().isBlank()) ||
                (pbm.getDocuments() != null &&
                        pbm.getDocuments().stream().anyMatch(doc -> "2".equals(doc.getDocumentType())));
    }

    private static boolean hasUsageInstructionsContent(Pbm pbm) {
        return (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) ||
                (pbm.getDocuments() != null &&
                        pbm.getDocuments().stream().anyMatch(doc -> "1".equals(doc.getDocumentType())));
    }

    private static boolean hasStandardsContent(Pbm pbm) {
        return (pbm.getStandards() != null && !pbm.getStandards().isBlank()) ||
                (pbm.getNorms() != null && !pbm.getNorms().isEmpty());
    }

    private static Div createHtmlContent(String htmlContent) {
        Div content = new Div();
        content.getElement().setProperty("innerHTML", htmlContent);
        content.getStyle().set("padding", "10px");
        return content;
    }

    private static Div createNotesContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
            Div notesContent = createHtmlContent(pbm.getNotes());
            layout.add(notesContent);
        }

        // Create a spacer div to push downloads to bottom
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        layout.add(spacer);

        // Add documents with type 2 at the bottom
        if (pbm.getDocuments() != null) {
            var type2Documents = pbm.getDocuments().stream()
                    .filter(doc -> "2".equals(doc.getDocumentType()))
                    .toList();
            if (!type2Documents.isEmpty()) {
                if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
                    Hr divider = new Hr();
                    layout.add(divider);
                    H4 documentsHeader = new H4("Related Documents");
                    documentsHeader.getStyle().set("margin", "10px 0 5px 0");
                    layout.add(documentsHeader);
                }
                Div documentsDiv = createDownloadableDocuments(type2Documents);
                layout.add(documentsDiv);
            }
        }

        Div container = new Div(layout);
        container.setWidthFull();
        container.setHeightFull();
        container.getStyle().set("display", "flex");
        container.getStyle().set("flex-direction", "column");
        return container;
    }

    private static Div createUsageInstructionsContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
            Div usageContent = createHtmlContent(pbm.getUsageInstructions());
            layout.add(usageContent);
        }

        // Create a spacer div to push downloads to bottom
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        layout.add(spacer);

        // Add documents with type 1 at the bottom
        if (pbm.getDocuments() != null) {
            var type1Documents = pbm.getDocuments().stream()
                    .filter(doc -> "1".equals(doc.getDocumentType()))
                    .toList();
            if (!type1Documents.isEmpty()) {
                if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
                    Hr divider = new Hr();
                    layout.add(divider);
                    H4 documentsHeader = new H4("Related Documents");
                    documentsHeader.getStyle().set("margin", "10px 0 5px 0");
                    layout.add(documentsHeader);
                }
                Div documentsDiv = createDownloadableDocuments(type1Documents);
                layout.add(documentsDiv);
            }
        }

        Div container = new Div(layout);
        container.setWidthFull();
        container.setHeightFull();
        container.getStyle().set("display", "flex");
        container.getStyle().set("flex-direction", "column");
        return container;
    }

    private static Div createStandardsContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
            Div standardsContent = createHtmlContent(pbm.getStandards());
            layout.add(standardsContent);
        }

        // Create a spacer div to push norms to bottom
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        layout.add(spacer);

        if (pbm.getNorms() != null && !pbm.getNorms().isEmpty()) {
            if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
                Hr divider = new Hr();
                layout.add(divider);
                H4 normsHeader = new H4("Norms");
                normsHeader.getStyle().set("margin", "10px 0 5px 0");
                layout.add(normsHeader);
            }
            Div normsDiv = createDownloadableNorms(pbm.getNorms());
            layout.add(normsDiv);
        }

        Div container = new Div(layout);
        container.setWidthFull();
        container.setHeightFull();
        container.getStyle().set("display", "flex");
        container.getStyle().set("flex-direction", "column");
        return container;
    }

    private static Div createWarehouseContent(Pbm pbm) {
        Grid<WarehouseItem> grid = new Grid<>(WarehouseItem.class, false);
        grid.addColumn(WarehouseItem::getWarehouseNumber).setHeader("Warehouse Number").setFlexGrow(1);
        grid.addColumn(WarehouseItem::getVariantText).setHeader("Variant").setFlexGrow(1);
        grid.setItems(pbm.getWarehouseItems());
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        Div container = new Div(grid);
        container.setWidthFull();
        return container;
    }

    private static Div createCategoriesContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();

        for (Category category : pbm.getCategories()) {
            Span categorySpan = new Span(category.getName());
            categorySpan.getStyle().set("display", "inline-block");
            categorySpan.getStyle().set("background", "var(--lumo-contrast-10pct)");
            categorySpan.getStyle().set("padding", "4px 8px");
            categorySpan.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            categorySpan.getStyle().set("margin", "2px");
            layout.add(categorySpan);
        }

        Div container = new Div(layout);
        container.setWidthFull();
        return container;
    }

    private static Div createDownloadableDocuments(List<Document> documents) {
        FlexLayout flexLayout = new FlexLayout();
        flexLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        flexLayout.getStyle().set("gap", "10px");
        flexLayout.setWidthFull();

        // Filter only documents with valid file paths
        List<Document> validDocs = documents.stream()
                .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().isBlank())
                .toList();

        for (Document doc : validDocs) {
            String fileName = doc.getFilePath();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName == null)
                fileName = "Unknown";

            Button downloadButton = new Button();
            downloadButton.setIcon(VaadinIcon.DOWNLOAD.create());
            downloadButton.setText(fileName);
            downloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            downloadButton.getStyle().set("cursor", "pointer");

            if (doc.getDescription() != null && !doc.getDescription().isBlank()) {
                downloadButton.setTooltipText(doc.getDescription());
            }

            downloadButton.addClickListener(event -> {
                String downloadUrl = "/static/" + doc.getFilePath();
                downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
            });

            flexLayout.add(downloadButton);
        }

        Div container = new Div(flexLayout);
        container.setWidthFull();
        return container;
    }

    private static Div createDownloadableNorms(Set<Norm> norms) {
        FlexLayout flexLayout = new FlexLayout();
        flexLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        flexLayout.getStyle().set("gap", "10px");
        flexLayout.setWidthFull();

        for (Norm norm : norms) {
            String buttonText = norm.getName();
            if (norm.getFilePath() != null && !norm.getFilePath().isBlank()) {
                String fileName = norm.getFilePath();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                if (buttonText == null || buttonText.isBlank()) {
                    buttonText = fileName;
                }
            }
            if (buttonText == null || buttonText.isBlank()) {
                buttonText = "Unknown";
            }

            Button downloadButton = new Button();
            downloadButton.setIcon(VaadinIcon.DOWNLOAD.create());
            downloadButton.setText(buttonText);
            downloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            downloadButton.getStyle().set("cursor", "pointer");

            if (norm.getDescription() != null && !norm.getDescription().isBlank()) {
                downloadButton.setTooltipText(norm.getDescription());
            }

            // Only add click listener if there's a valid file path
            if (norm.getFilePath() != null && !norm.getFilePath().isBlank()) {
                downloadButton.addClickListener(event -> {
                    String downloadUrl = "/static/" + norm.getFilePath();
                    downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
                });
            } else {
                downloadButton.setEnabled(false);
                downloadButton.getStyle().set("opacity", "0.5");
            }

            flexLayout.add(downloadButton);
        }

        Div container = new Div(flexLayout);
        container.setWidthFull();
        return container;
    }

}