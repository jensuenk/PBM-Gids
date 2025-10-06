package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;
import java.util.Set;

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
        WAREHOUSE_ITEMS("Warehouse Items");

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
        column.getStyle().set("overflow", "auto");
        column.setWidthFull();

        // Add content directly without PBM name header
        if (hasContent(pbm, section)) {
            Div content = createContent(pbm, section);
            content.setWidthFull();
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
        return content;
    }

    private static Div createNotesContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
            layout.add(createHtmlContent(pbm.getNotes()));
        }

        // Add spacer to push documents to bottom
        layout.add(createFlexSpacer());

        // Add type 2 documents
        addDocumentsIfPresent(layout, pbm.getDocuments(), "2", "Related Documents",
                pbm.getNotes() != null && !pbm.getNotes().isBlank());

        return createFlexContainer(layout);
    }

    private static Div createUsageInstructionsContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
            layout.add(createHtmlContent(pbm.getUsageInstructions()));
        }

        layout.add(createFlexSpacer());

        addDocumentsIfPresent(layout, pbm.getDocuments(), "1", "Related Documents",
                pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank());

        return createFlexContainer(layout);
    }

    private static Div createStandardsContent(Pbm pbm) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setHeightFull();

        if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
            layout.add(createHtmlContent(pbm.getStandards()));
        }

        layout.add(createFlexSpacer());

        addNormsIfPresent(layout, pbm.getNorms(),
                pbm.getStandards() != null && !pbm.getStandards().isBlank());

        return createFlexContainer(layout);
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
                try {
                    // Encode each path segment separately to avoid encoding forward slashes
                    String[] pathParts = doc.getFilePath().split("/");
                    StringBuilder encodedPath = new StringBuilder();
                    for (int i = 0; i < pathParts.length; i++) {
                        if (i > 0)
                            encodedPath.append("/");
                        encodedPath.append(java.net.URLEncoder.encode(pathParts[i], "UTF-8").replace("+", "%20"));
                    }
                    String downloadUrl = "/static/" + encodedPath.toString();
                    downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
                } catch (java.io.UnsupportedEncodingException e) {
                    // Fallback to original path if encoding fails
                    String downloadUrl = "/static/" + doc.getFilePath();
                    downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
                }
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
                    try {
                        // Encode each path segment separately to avoid encoding forward slashes
                        String[] pathParts = norm.getFilePath().split("/");
                        StringBuilder encodedPath = new StringBuilder();
                        for (int i = 0; i < pathParts.length; i++) {
                            if (i > 0)
                                encodedPath.append("/");
                            encodedPath.append(java.net.URLEncoder.encode(pathParts[i], "UTF-8").replace("+", "%20"));
                        }
                        String downloadUrl = "/static/" + encodedPath.toString();
                        downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        // Fallback to original path if encoding fails
                        String downloadUrl = "/static/" + norm.getFilePath();
                        downloadButton.getUI().ifPresent(ui -> ui.getPage().open(downloadUrl, "_blank"));
                    }
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

    // Helper methods for reducing code duplication

    private static Div createFlexSpacer() {
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        return spacer;
    }

    private static Div createFlexContainer(VerticalLayout layout) {
        Div container = new Div(layout);
        container.setWidthFull();
        container.setHeightFull();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column");
        return container;
    }

    private static void addDocumentsIfPresent(VerticalLayout layout, Set<Document> documents,
            String documentType, String sectionTitle, boolean addDivider) {
        if (documents == null)
            return;

        var filteredDocs = documents.stream()
                .filter(doc -> documentType.equals(doc.getDocumentType()))
                .toList();

        if (!filteredDocs.isEmpty()) {
            if (addDivider) {
                layout.add(new Hr());
                H4 header = new H4(sectionTitle);
                header.getStyle().set("margin", "10px 0 5px 0");
                layout.add(header);
            }
            layout.add(createDownloadableDocuments(filteredDocs));
        }
    }

    private static void addNormsIfPresent(VerticalLayout layout, Set<Norm> norms, boolean addDivider) {
        if (norms == null || norms.isEmpty())
            return;

        if (addDivider) {
            layout.add(new Hr());
            H4 header = new H4("Norms");
            header.getStyle().set("margin", "10px 0 5px 0");
            layout.add(header);
        }
        layout.add(createDownloadableNorms(norms));
    }

}