package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.model.entities.Category;
import com.ineos.oxide.pbmgids.model.entities.Document;
import com.ineos.oxide.pbmgids.model.entities.Norm;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.model.entities.WarehouseItem;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.services.CategoryDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Main catalog view that displays PBMs for a specific category.
 * This view is accessed via URL: /catalog/{categoryId}
 */
@Route(value = "catalog/:categoryId", layout = MainView.class)
@PageTitle("")
@AnonymousAllowed
public class CatalogView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger logger = Logger.getLogger(CatalogView.class.getName());

    // Components
    private final H2 categoryTitle;
    private final ComboBox<String> searchField;
    private final Grid<Pbm> pbmGrid;
    private final Button compareButton;
    private final HorizontalLayout toolbarLayout;

    // Services
    private final CategoryDataService categoryDataService;

    // State
    private Integer currentCategoryId;
    private List<Pbm> allPbms = new ArrayList<>();
    private String currentSearchFilter = "";
    private List<Pbm> selectedPbmsForComparison = new ArrayList<>();

    public CatalogView(CatalogService catalogService) {
        this.categoryDataService = new CategoryDataService(catalogService);
        this.categoryTitle = new H2("PBM Catalog");
        this.searchField = new ComboBox<>();
        this.pbmGrid = new Grid<>(Pbm.class, false);
        this.compareButton = new Button("Compare Selected", VaadinIcon.SCALE.create());
        this.toolbarLayout = new HorizontalLayout();

        initializeView();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String categoryIdParam = event.getRouteParameters().get("categoryId").orElse(null);
        Integer categoryId = categoryDataService.validateAndParseCategoryId(categoryIdParam);

        if (categoryId != null) {
            this.currentCategoryId = categoryId;
            loadPbmsForCategory(categoryId);
            loadCategoryTitle(categoryId);
        } else {
            logger.warning("Invalid or missing category ID, showing empty grid");
            this.currentCategoryId = null;
            categoryTitle.setText("PBM Catalog - Invalid Category");
            allPbms.clear();
            pbmGrid.setItems();
        }
    }

    private void initializeView() {
        setPadding(true);

        // Setup search field
        setupSearchField();

        // Setup toolbar with compare functionality
        setupToolbar();

        // Setup PBM grid columns
        setupGridColumns();

        // Add components to view
        add(searchField, categoryTitle, toolbarLayout, pbmGrid);
        setSizeFull();
    }

    private void setupSearchField() {
        searchField.setPlaceholder("Search PBMs...");
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();

        // Enable custom values (allows typing anything, not just suggestions)
        searchField.setAllowCustomValue(true);

        // Configure autocomplete behavior
        searchField.addCustomValueSetListener(event -> {
            String customValue = event.getDetail();
            searchField.setValue(customValue);
            performSearch(customValue);
        });

        // Handle selection from suggestions
        searchField.addValueChangeListener(event -> {
            String value = event.getValue();
            performSearch(value);
        });

        // Set up lazy data provider for dynamic suggestions with proper pagination
        searchField.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit();

            // Store current filter for highlighting
            currentSearchFilter = filter;

            // Only show suggestions when at least 2 characters are entered
            if (filter.length() < 2) {
                return java.util.stream.Stream.empty();
            }

            List<String> allSuggestions = categoryDataService.getPbmNameSuggestions(filter);

            // Apply pagination
            return allSuggestions.stream()
                    .skip(offset)
                    .limit(limit);
        });

        // Set up custom renderer to highlight matching text
        searchField.setRenderer(new ComponentRenderer<>(this::createHighlightedSuggestion));
    }

    private void setupToolbar() {
        // Configure compare button
        compareButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        compareButton.setEnabled(false);
        compareButton.addClickListener(e -> showComparisonDialog());

        // Add info text
        Span infoText = new Span("Select up to 3 PBMs to compare");
        infoText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        infoText.getStyle().set("font-size", "var(--lumo-font-size-s)");

        toolbarLayout.add(compareButton, infoText);
        toolbarLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbarLayout.setSpacing(true);
    }

    private void setupGridColumns() {
        // Checkbox column for comparison selection
        pbmGrid.addColumn(new ComponentRenderer<>(this::createCompareCheckbox))
                .setHeader("Compare")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setWidth("90px");
        // Image column
        pbmGrid.addColumn(new ComponentRenderer<>(this::createImageComponent))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setWidth("80px");

        // Name column
        pbmGrid.addColumn(Pbm::getName)
                .setHeader("Name")
                .setAutoWidth(true);

        // Type column
        pbmGrid.addColumn(Pbm::getTypeName)
                .setHeader("Type")
                .setAutoWidth(true);

        // Brand column
        pbmGrid.addColumn(Pbm::getBrand)
                .setHeader("Brand")
                .setAutoWidth(true);

        // Details button column
        pbmGrid.addColumn(new ComponentRenderer<>(this::createDetailsButton))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0);

        pbmGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private Div createImageComponent(Pbm pbm) {
        if (pbm.getImage() == null || pbm.getImage().isBlank()) {
            return new Div();
        }

        String imageUrl = getImageUrl(pbm.getImage());
        Image img = new Image(imageUrl, pbm.getName());
        img.setAlt(pbm.getName());
        img.setWidth("64px");

        return new Div(img);
    }

    private String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }

        // Remove leading slash if present
        String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // Construct the static resource URL
        // Since images are in src/main/resources/static/images/mag_doc
        // and database has paths like mag_doc/PBM/Foto_afbeeldingen/...
        // we need to prefix with /images/
        return "/images/" + cleanPath;
    }

    private Checkbox createCompareCheckbox(Pbm pbm) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(selectedPbmsForComparison.contains(pbm));
        checkbox.addValueChangeListener(event -> {
            if (event.getValue()) {
                // Add to comparison if not already there and under limit
                if (!selectedPbmsForComparison.contains(pbm)) {
                    if (selectedPbmsForComparison.size() < 3) {
                        selectedPbmsForComparison.add(pbm);
                    } else {
                        // Show notification and uncheck
                        checkbox.setValue(false);
                        Notification notification = Notification.show("You can only compare up to 3 PBMs");
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                }
            } else {
                // Remove from comparison
                selectedPbmsForComparison.remove(pbm);
            }
            updateCompareButtonState();
        });
        return checkbox;
    }

    private Button createDetailsButton(Pbm pbm) {
        Button btn = new Button("Details");
        btn.addClickListener(ev -> showPbmDetails(pbm));
        return btn;
    }

    private void updateCompareButtonState() {
        compareButton.setEnabled(selectedPbmsForComparison.size() >= 2);
        compareButton.setText("Compare Selected (" + selectedPbmsForComparison.size() + ")");
    }

    private Span createHighlightedSuggestion(String suggestion) {
        if (currentSearchFilter == null || currentSearchFilter.isEmpty() || suggestion == null) {
            return new Span(suggestion);
        }

        Span container = new Span();
        String lowerSuggestion = suggestion.toLowerCase();
        String lowerFilter = currentSearchFilter.toLowerCase();

        int startIndex = lowerSuggestion.indexOf(lowerFilter);

        if (startIndex == -1) {
            // No match found, return plain text
            container.setText(suggestion);
        } else {
            // Create highlighted text
            if (startIndex > 0) {
                // Add text before match
                container.add(new Span(suggestion.substring(0, startIndex)));
            }

            // Add bold matched text
            Span boldMatch = new Span(suggestion.substring(startIndex, startIndex + currentSearchFilter.length()));
            boldMatch.getStyle().set("font-weight", "bold");
            container.add(boldMatch);

            if (startIndex + currentSearchFilter.length() < suggestion.length()) {
                // Add text after match
                container.add(new Span(suggestion.substring(startIndex + currentSearchFilter.length())));
            }
        }

        return container;
    }

    private void loadPbmsForCategory(Integer categoryId) {
        allPbms = categoryDataService.loadPbmsByCategory(categoryId);
        pbmGrid.setItems(allPbms);

        // Clear search field when loading new category
        searchField.clear();

        // Clear selected items for comparison when switching categories
        selectedPbmsForComparison.clear();
        updateCompareButtonState();
    }

    private void loadCategoryTitle(Integer categoryId) {
        String categoryName = categoryDataService.getCategoryName(categoryId);
        if (categoryName != null && !categoryName.isBlank()) {
            categoryTitle.setText("PBM Catalog - " + categoryName);
        } else {
            categoryTitle.setText("PBM Catalog - Category " + categoryId);
        }
    }

    private void performSearch(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Show all PBMs for current category if search is empty
            pbmGrid.setItems(allPbms);
            updateSearchStatus(false, allPbms.size(), 0);
        } else {
            // Search across all PBMs in database when there's a search term
            List<Pbm> searchResults = categoryDataService.searchAllPbms(searchTerm.trim());
            pbmGrid.setItems(searchResults);
            updateSearchStatus(true, searchResults.size(), allPbms.size());
        }
    }

    private void updateSearchStatus(boolean isSearching, int resultCount, int totalCount) {
        if (isSearching) {
            categoryTitle.setText("PBM Search Results for \"" + currentSearchFilter);
        } else {
            // Restore original category title when not searching
            if (currentCategoryId != null) {
                loadCategoryTitle(currentCategoryId);
            }
        }
    }

    private void showPbmDetails(Pbm pbm) {
        if (pbm != null) {
            Dialog dialog = createDetailsDialog(pbm);
            dialog.open();
        }
    }

    private Dialog createDetailsDialog(Pbm pbm) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("900px");
        dialog.setHeight("700px");

        // Header section with image and basic info
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
            img.setMaxWidth("280px");
            img.setMaxHeight("280px");
            imageLayout.add(img);
        } else {
            Div placeholder = new Div();
            placeholder.setText("No image available");
            placeholder.setWidth("280px");
            placeholder.setHeight("280px");
            placeholder.getStyle().set("border", "1px dashed #ccc");
            placeholder.getStyle().set("display", "flex");
            placeholder.getStyle().set("align-items", "center");
            placeholder.getStyle().set("justify-content", "center");
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
            H3 brandHeader = new H3("Brand");
            brandHeader.getStyle().set("margin", "0 0 5px 0");
            Paragraph brandText = new Paragraph(pbm.getBrand());
            brandText.getStyle().set("margin", "0 0 15px 0");
            basicInfoLayout.add(brandHeader, brandText);
        }

        if (pbm.getTypeName() != null && !pbm.getTypeName().isBlank()) {
            H3 typeHeader = new H3("Type");
            typeHeader.getStyle().set("margin", "0 0 5px 0");
            Paragraph typeText = new Paragraph(pbm.getTypeName());
            typeText.getStyle().set("margin", "0 0 15px 0");
            basicInfoLayout.add(typeHeader, typeText);
        }

        headerLayout.add(imageLayout, basicInfoLayout);

        // Tabbed details section - below the header
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.getStyle().set("overflow-y", "auto");

        // Description tab
        if (pbm.getDescription() != null && !pbm.getDescription().isBlank()) {
            Div descriptionContent = createHtmlContent(pbm.getDescription());
            tabSheet.add("Description", descriptionContent);
        }

        // Protects Against tab
        if (pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank()) {
            Div protectsContent = createHtmlContent(pbm.getProtectsAgainst());
            tabSheet.add("Protects Against", protectsContent);
        }

        // Does Not Protect Against tab
        if (pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank()) {
            Div doesNotProtectContent = createHtmlContent(pbm.getDoesNotProtectAgainst());
            tabSheet.add("Does Not Protect", doesNotProtectContent);
        }

        // Notes tab (includes documents with type 2)
        if ((pbm.getNotes() != null && !pbm.getNotes().isBlank()) ||
                (pbm.getDocuments() != null
                        && pbm.getDocuments().stream().anyMatch(doc -> "2".equals(doc.getDocumentType())))) {
            VerticalLayout notesLayout = new VerticalLayout();
            notesLayout.setPadding(false);
            notesLayout.setSpacing(true);

            if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
                Div notesContent = createHtmlContent(pbm.getNotes());
                notesLayout.add(notesContent);
            }

            // Add documents with type 2
            if (pbm.getDocuments() != null) {
                var type2Documents = pbm.getDocuments().stream()
                        .filter(doc -> "2".equals(doc.getDocumentType()))
                        .toList();
                if (!type2Documents.isEmpty()) {
                    if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
                        // Add separator if we have both content and documents
                        notesLayout.add(new com.vaadin.flow.component.html.Hr());
                        H4 documentsHeader = new H4("Related Documents");
                        documentsHeader.getStyle().set("margin", "10px 0 5px 0");
                        notesLayout.add(documentsHeader);
                    }
                    Div documentsDiv = createDownloadableDocuments(type2Documents);
                    notesLayout.add(documentsDiv);
                }
            }

            tabSheet.add("Notes", notesLayout);
        }

        // Usage Instructions tab (includes documents with type 1)
        if ((pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) ||
                (pbm.getDocuments() != null
                        && pbm.getDocuments().stream().anyMatch(doc -> "1".equals(doc.getDocumentType())))) {
            VerticalLayout usageLayout = new VerticalLayout();
            usageLayout.setPadding(false);
            usageLayout.setSpacing(true);

            if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
                Div usageContent = createHtmlContent(pbm.getUsageInstructions());
                usageLayout.add(usageContent);
            }

            // Add documents with type 1
            if (pbm.getDocuments() != null) {
                var type1Documents = pbm.getDocuments().stream()
                        .filter(doc -> "1".equals(doc.getDocumentType()))
                        .toList();
                if (!type1Documents.isEmpty()) {
                    if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
                        // Add separator if we have both content and documents
                        usageLayout.add(new com.vaadin.flow.component.html.Hr());
                        H4 documentsHeader = new H4("Related Documents");
                        documentsHeader.getStyle().set("margin", "10px 0 5px 0");
                        usageLayout.add(documentsHeader);
                    }
                    Div documentsDiv = createDownloadableDocuments(type1Documents);
                    usageLayout.add(documentsDiv);
                }
            }

            tabSheet.add("Usage Instructions", usageLayout);
        }

        // Distribution tab
        if (pbm.getDistribution() != null && !pbm.getDistribution().isBlank()) {
            Div distributionContent = createHtmlContent(pbm.getDistribution());
            tabSheet.add("Distribution", distributionContent);
        }

        // Standards tab (includes norms)
        if ((pbm.getStandards() != null && !pbm.getStandards().isBlank()) ||
                (pbm.getNorms() != null && !pbm.getNorms().isEmpty())) {
            VerticalLayout standardsLayout = new VerticalLayout();
            standardsLayout.setPadding(false);
            standardsLayout.setSpacing(true);

            if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
                Div standardsContent = createHtmlContent(pbm.getStandards());
                standardsLayout.add(standardsContent);
            }

            // Add norms
            if (pbm.getNorms() != null && !pbm.getNorms().isEmpty()) {
                if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
                    // Add separator if we have both content and norms
                    standardsLayout.add(new com.vaadin.flow.component.html.Hr());
                    H4 normsHeader = new H4("Standards Documents");
                    normsHeader.getStyle().set("margin", "10px 0 5px 0");
                    standardsLayout.add(normsHeader);
                }
                Div normsDiv = createDownloadableNorms(pbm.getNorms());
                standardsLayout.add(normsDiv);
            }

            tabSheet.add("Standards", standardsLayout);
        }

        // Warehouse Items tab
        if (pbm.getWarehouseItems() != null && !pbm.getWarehouseItems().isEmpty()) {
            Div warehouseContent = createWarehouseTable(pbm.getWarehouseItems());
            tabSheet.add("Warehouse", warehouseContent);
        }

        // Footer with close button (fixed)
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setPadding(true);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        footerLayout.add(closeButton);

        // Dialog layout with fixed footer
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();

        dialogLayout.add(headerLayout); // Header with image and basic info
        dialogLayout.addAndExpand(tabSheet); // Tabs take all available remaining space
        dialogLayout.add(footerLayout); // This stays at the bottom

        dialog.add(dialogLayout);

        return dialog;
    }

    private Div createHtmlContent(String htmlContent) {
        Div content = new Div();
        content.getElement().setProperty("innerHTML", htmlContent);
        content.getStyle().set("padding", "10px");
        return content;
    }

    private Div createWarehouseTable(java.util.Set<WarehouseItem> warehouseItems) {
        Grid<WarehouseItem> grid = new Grid<>(WarehouseItem.class, false);
        grid.addColumn(WarehouseItem::getWarehouseNumber).setHeader("Warehouse Number").setFlexGrow(1);
        grid.addColumn(WarehouseItem::getVariantText).setHeader("Variant").setFlexGrow(1);
        grid.setItems(warehouseItems);
        grid.setWidthFull();

        Div container = new Div(grid);
        return container;
    }

    private Div createDownloadableDocuments(List<Document> documents) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        for (Document doc : documents) {
            HorizontalLayout docLayout = new HorizontalLayout();
            docLayout.setSpacing(true);
            docLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            docLayout.setPadding(true);
            docLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            docLayout.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            docLayout.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
            docLayout.setWidthFull();

            // File icon
            Span icon = new Span();
            icon.getElement().setAttribute("class", "las la-file-download");
            icon.getStyle().set("font-size", "1.5em");
            icon.getStyle().set("color", "var(--lumo-primary-color)");

            // File link
            String fileName = doc.getFilePath();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            Anchor fileLink = new Anchor("/static/" + doc.getFilePath(), fileName != null ? fileName : "Download");
            fileLink.getElement().setAttribute("download", true);
            fileLink.getElement().setAttribute("target", "_blank");
            fileLink.getStyle().set("text-decoration", "none");
            fileLink.getStyle().set("color", "var(--lumo-primary-color)");
            fileLink.getStyle().set("font-weight", "500");

            // File info
            VerticalLayout infoLayout = new VerticalLayout();
            infoLayout.setPadding(false);
            infoLayout.setSpacing(false);
            infoLayout.setFlexGrow(1);

            infoLayout.add(fileLink);

            if (doc.getDescription() != null && !doc.getDescription().isBlank()) {
                Span description = new Span(doc.getDescription());
                description.getStyle().set("font-size", "var(--lumo-font-size-s)");
                description.getStyle().set("color", "var(--lumo-secondary-text-color)");
                infoLayout.add(description);
            }

            if (doc.getMimeType() != null && !doc.getMimeType().isBlank()) {
                Span mimeType = new Span(doc.getMimeType());
                mimeType.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                mimeType.getStyle().set("color", "var(--lumo-tertiary-text-color)");
                infoLayout.add(mimeType);
            }

            docLayout.add(icon, infoLayout);
            layout.add(docLayout);
        }

        Div container = new Div(layout);
        return container;
    }

    private Div createDownloadableNorms(java.util.Set<Norm> norms) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        for (Norm norm : norms) {
            HorizontalLayout normLayout = new HorizontalLayout();
            normLayout.setSpacing(true);
            normLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            normLayout.setPadding(true);
            normLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            normLayout.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            normLayout.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
            normLayout.setWidthFull();

            // File icon
            Span icon = new Span();
            icon.getElement().setAttribute("class", "las la-file-download");
            icon.getStyle().set("font-size", "1.5em");
            icon.getStyle().set("color", "var(--lumo-primary-color)");

            // Norm info layout
            VerticalLayout infoLayout = new VerticalLayout();
            infoLayout.setPadding(false);
            infoLayout.setSpacing(false);
            infoLayout.setFlexGrow(1);

            // Norm name
            if (norm.getName() != null && !norm.getName().isBlank()) {
                H4 nameHeader = new H4(norm.getName());
                nameHeader.getStyle().set("margin", "0 0 5px 0");
                nameHeader.getStyle().set("font-size", "var(--lumo-font-size-m)");
                infoLayout.add(nameHeader);
            }

            // File link (if available)
            if (norm.getFilePath() != null && !norm.getFilePath().isBlank()) {
                String fileName = norm.getFilePath();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }

                Anchor fileLink = new Anchor("/static/" + norm.getFilePath(), "Download: " + fileName);
                fileLink.getElement().setAttribute("download", true);
                fileLink.getElement().setAttribute("target", "_blank");
                fileLink.getStyle().set("text-decoration", "none");
                fileLink.getStyle().set("color", "var(--lumo-primary-color)");
                fileLink.getStyle().set("font-weight", "500");
                infoLayout.add(fileLink);
            }

            if (norm.getDescription() != null && !norm.getDescription().isBlank()) {
                Span description = new Span(norm.getDescription());
                description.getStyle().set("font-size", "var(--lumo-font-size-s)");
                description.getStyle().set("color", "var(--lumo-secondary-text-color)");
                infoLayout.add(description);
            }

            if (norm.getMimeType() != null && !norm.getMimeType().isBlank()) {
                Span mimeType = new Span(norm.getMimeType());
                mimeType.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                mimeType.getStyle().set("color", "var(--lumo-tertiary-text-color)");
                infoLayout.add(mimeType);
            }

            normLayout.add(icon, infoLayout);
            layout.add(normLayout);
        }

        Div container = new Div(layout);
        return container;
    }

    private void showComparisonDialog() {
        if (selectedPbmsForComparison.size() < 2) {
            Notification.show("Please select at least 2 PBMs to compare");
            return;
        }

        Dialog dialog = createComparisonDialog(selectedPbmsForComparison);
        dialog.open();
    }

    private Dialog createComparisonDialog(List<Pbm> pbmsToCompare) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1200px");
        dialog.setHeight("800px");

        // Main container with fixed header/footer
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.setSizeFull();

        // Header section (fixed)
        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setPadding(true);
        headerLayout.setSpacing(true);

        H2 title = new H2("PBM Comparison (" + pbmsToCompare.size() + " items)");
        HorizontalLayout imagesLayout = createComparisonImagesLayout(pbmsToCompare);

        headerLayout.add(title, imagesLayout);

        // Scrollable content area
        VerticalLayout scrollableContent = new VerticalLayout();
        scrollableContent.setPadding(true);
        scrollableContent.setSpacing(true);
        scrollableContent.setWidthFull();

        // Add new accordions with data checks
        addAccordionIfHasData(scrollableContent, createDescriptionAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createCategoriesAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createProtectsAgainstAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createDoesNotProtectAgainstAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createNotesAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createUsageInstructionsAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createDistributionAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createStandardsAccordion(pbmsToCompare));
        addAccordionIfHasData(scrollableContent, createWarehouseNumbersAccordion(pbmsToCompare));

        // Make scrollable content area scrollable
        Div scrollWrapper = new Div(scrollableContent);
        scrollWrapper.getStyle().set("overflow-y", "auto");
        scrollWrapper.getStyle().set("flex", "1");
        scrollWrapper.setWidthFull();

        // Footer with close button (fixed)
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setPadding(true);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        footerLayout.add(closeButton);

        mainLayout.add(headerLayout, scrollWrapper, footerLayout);
        mainLayout.setFlexGrow(0, headerLayout);
        mainLayout.setFlexGrow(1, scrollWrapper);
        mainLayout.setFlexGrow(0, footerLayout);

        dialog.add(mainLayout);
        return dialog;
    }

    private HorizontalLayout createComparisonImagesLayout(List<Pbm> pbms) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout pbmLayout = new VerticalLayout();
            pbmLayout.setSpacing(true);
            pbmLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            pbmLayout.getStyle().set("flex", "1");
            pbmLayout.getStyle().set("padding", "var(--lumo-space-m)");
            pbmLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            pbmLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

            // PBM name
            H4 name = new H4(pbm.getName());
            name.getStyle().set("margin", "0");
            name.getStyle().set("text-align", "center");
            pbmLayout.add(name);

            // Image
            if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
                String imageUrl = getImageUrl(pbm.getImage());
                Image img = new Image(imageUrl, pbm.getName());
                img.setMaxWidth("200px");
                img.setMaxHeight("200px");
                pbmLayout.add(img);
            } else {
                Div placeholder = new Div();
                placeholder.setText("No image available");
                placeholder.setWidth("200px");
                placeholder.setHeight("200px");
                placeholder.getStyle().set("border", "1px dashed var(--lumo-contrast-30pct)");
                placeholder.getStyle().set("display", "flex");
                placeholder.getStyle().set("align-items", "center");
                placeholder.getStyle().set("justify-content", "center");
                placeholder.getStyle().set("color", "var(--lumo-secondary-text-color)");
                pbmLayout.add(placeholder);
            }

            // Basic info under image
            VerticalLayout basicInfo = new VerticalLayout();
            basicInfo.setSpacing(false);
            basicInfo.setPadding(false);
            basicInfo.getStyle().set("width", "100%");

            if (pbm.getTypeName() != null && !pbm.getTypeName().isBlank()) {
                Div typeDiv = new Div();
                typeDiv.setText("Type: " + pbm.getTypeName());
                typeDiv.getStyle().set("font-weight", "bold");
                typeDiv.getStyle().set("text-align", "center");
                basicInfo.add(typeDiv);
            }

            if (pbm.getBrand() != null && !pbm.getBrand().isBlank()) {
                Div brandDiv = new Div();
                brandDiv.setText("Brand: " + pbm.getBrand());
                brandDiv.getStyle().set("text-align", "center");
                brandDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                basicInfo.add(brandDiv);
            }

            pbmLayout.add(basicInfo);
            layout.add(pbmLayout);
        }

        return layout;
    }

    @SuppressWarnings("unused")
    private Div createDocumentsComparisonTable(List<Pbm> pbms) {
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(true);
        container.setPadding(false);

        // Create header row with PBM names
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();

        // Empty cell for property column
        Div emptyHeader = new Div();
        emptyHeader.setText("Document Properties");
        emptyHeader.getStyle().set("font-weight", "bold");
        emptyHeader.getStyle().set("width", "200px");
        emptyHeader.getStyle().set("flex-shrink", "0");
        headerLayout.add(emptyHeader);

        // Add PBM name headers
        for (Pbm pbm : pbms) {
            Div pbmHeader = new Div();
            pbmHeader.setText(pbm.getName());
            pbmHeader.getStyle().set("font-weight", "bold");
            pbmHeader.getStyle().set("flex", "1");
            pbmHeader.getStyle().set("text-align", "center");
            headerLayout.add(pbmHeader);
        }

        container.add(headerLayout);

        // Find all unique document properties to compare
        int maxDocs = pbms.stream().mapToInt(pbm -> pbm.getDocuments() != null ? pbm.getDocuments().size() : 0).max()
                .orElse(0);

        if (maxDocs > 0) {
            for (int i = 0; i < maxDocs; i++) {
                final int docIndex = i;

                // File Path row
                HorizontalLayout filePathRow = createDocumentPropertyRow("File Path " + (i + 1), pbms, docIndex,
                        doc -> doc.getFilePath() != null ? doc.getFilePath() : "");
                container.add(filePathRow);

                // Document Type row
                HorizontalLayout typeRow = createDocumentPropertyRow("Type " + (i + 1), pbms, docIndex,
                        doc -> doc.getDocumentType() != null ? doc.getDocumentType() : "");
                container.add(typeRow);

                // MIME Type row
                HorizontalLayout mimeRow = createDocumentPropertyRow("MIME Type " + (i + 1), pbms, docIndex,
                        doc -> doc.getMimeType() != null ? doc.getMimeType() : "");
                container.add(mimeRow);

                if (i < maxDocs - 1) {
                    // Add separator
                    Div separator = new Div();
                    separator.getStyle().set("height", "1px");
                    separator.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
                    separator.getStyle().set("margin", "var(--lumo-space-xs) 0");
                    container.add(separator);
                }
            }
        } else {
            HorizontalLayout noDataRow = new HorizontalLayout();
            noDataRow.setSpacing(true);
            noDataRow.setWidthFull();

            Div noDataLabel = new Div();
            noDataLabel.setText("No documents");
            noDataLabel.getStyle().set("width", "200px");
            noDataLabel.getStyle().set("flex-shrink", "0");
            noDataRow.add(noDataLabel);

            for (int i = 0; i < pbms.size(); i++) {
                Div noDataValue = new Div();
                noDataValue.setText("No documents available");
                noDataValue.getStyle().set("flex", "1");
                noDataValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noDataRow.add(noDataValue);
            }

            container.add(noDataRow);
        }

        Div wrapper = new Div(container);
        wrapper.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        wrapper.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        wrapper.getStyle().set("padding", "var(--lumo-space-m)");
        return wrapper;
    }

    private HorizontalLayout createDocumentPropertyRow(String property, List<Pbm> pbms, int docIndex,
            java.util.function.Function<Document, String> valueExtractor) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();

        // Property name
        Div propertyDiv = new Div();
        propertyDiv.setText(property);
        propertyDiv.getStyle().set("font-weight", "bold");
        propertyDiv.getStyle().set("width", "200px");
        propertyDiv.getStyle().set("flex-shrink", "0");
        row.add(propertyDiv);

        // Values for each PBM
        for (Pbm pbm : pbms) {
            Div valueDiv = new Div();
            String value = "";
            if (pbm.getDocuments() != null && pbm.getDocuments().size() > docIndex) {
                Document doc = pbm.getDocuments().toArray(new Document[0])[docIndex];
                value = valueExtractor.apply(doc);
            }
            valueDiv.setText(value.isEmpty() ? "-" : value);
            valueDiv.getStyle().set("flex", "1");
            if (value.isEmpty()) {
                valueDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
            }
            row.add(valueDiv);
        }

        return row;
    }

    @SuppressWarnings("unused")
    private Div createNormsComparisonTable(List<Pbm> pbms) {
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(true);
        container.setPadding(false);

        // Create header row with PBM names
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();

        // Empty cell for property column
        Div emptyHeader = new Div();
        emptyHeader.setText("Norm Properties");
        emptyHeader.getStyle().set("font-weight", "bold");
        emptyHeader.getStyle().set("width", "200px");
        emptyHeader.getStyle().set("flex-shrink", "0");
        headerLayout.add(emptyHeader);

        // Add PBM name headers
        for (Pbm pbm : pbms) {
            Div pbmHeader = new Div();
            pbmHeader.setText(pbm.getName());
            pbmHeader.getStyle().set("font-weight", "bold");
            pbmHeader.getStyle().set("flex", "1");
            pbmHeader.getStyle().set("text-align", "center");
            headerLayout.add(pbmHeader);
        }

        container.add(headerLayout);

        // Find all unique norms to compare
        int maxNorms = pbms.stream().mapToInt(pbm -> pbm.getNorms() != null ? pbm.getNorms().size() : 0).max()
                .orElse(0);

        if (maxNorms > 0) {
            for (int i = 0; i < maxNorms; i++) {
                final int normIndex = i;

                // Norm Name row
                HorizontalLayout nameRow = createNormPropertyRow("Name " + (i + 1), pbms, normIndex,
                        norm -> norm.getName() != null ? norm.getName() : "");
                container.add(nameRow);

                // Norm Description row
                HorizontalLayout descRow = createNormPropertyRow("Description " + (i + 1), pbms, normIndex,
                        norm -> norm.getDescription() != null ? norm.getDescription() : "");
                container.add(descRow);

                if (i < maxNorms - 1) {
                    // Add separator
                    Div separator = new Div();
                    separator.getStyle().set("height", "1px");
                    separator.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
                    separator.getStyle().set("margin", "var(--lumo-space-xs) 0");
                    container.add(separator);
                }
            }
        } else {
            HorizontalLayout noDataRow = new HorizontalLayout();
            noDataRow.setSpacing(true);
            noDataRow.setWidthFull();

            Div noDataLabel = new Div();
            noDataLabel.setText("No norms");
            noDataLabel.getStyle().set("width", "200px");
            noDataLabel.getStyle().set("flex-shrink", "0");
            noDataRow.add(noDataLabel);

            for (int i = 0; i < pbms.size(); i++) {
                Div noDataValue = new Div();
                noDataValue.setText("No norms available");
                noDataValue.getStyle().set("flex", "1");
                noDataValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noDataRow.add(noDataValue);
            }

            container.add(noDataRow);
        }

        Div wrapper = new Div(container);
        wrapper.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        wrapper.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        wrapper.getStyle().set("padding", "var(--lumo-space-m)");
        return wrapper;
    }

    private HorizontalLayout createNormPropertyRow(String property, List<Pbm> pbms, int normIndex,
            java.util.function.Function<Norm, String> valueExtractor) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();

        // Property name
        Div propertyDiv = new Div();
        propertyDiv.setText(property);
        propertyDiv.getStyle().set("font-weight", "bold");
        propertyDiv.getStyle().set("width", "200px");
        propertyDiv.getStyle().set("flex-shrink", "0");
        row.add(propertyDiv);

        // Values for each PBM
        for (Pbm pbm : pbms) {
            Div valueDiv = new Div();
            String value = "";
            if (pbm.getNorms() != null && pbm.getNorms().size() > normIndex) {
                Norm norm = pbm.getNorms().toArray(new Norm[0])[normIndex];
                value = valueExtractor.apply(norm);
            }
            valueDiv.setText(value.isEmpty() ? "-" : value);
            valueDiv.getStyle().set("flex", "1");
            if (value.isEmpty()) {
                valueDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
            }
            row.add(valueDiv);
        }

        return row;
    }

    @SuppressWarnings("unused")
    private Div createWarehouseComparisonTable(List<Pbm> pbms) {
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(true);
        container.setPadding(false);

        // Create header row with PBM names
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();

        // Empty cell for property column
        Div emptyHeader = new Div();
        emptyHeader.setText("Warehouse Properties");
        emptyHeader.getStyle().set("font-weight", "bold");
        emptyHeader.getStyle().set("width", "200px");
        emptyHeader.getStyle().set("flex-shrink", "0");
        headerLayout.add(emptyHeader);

        // Add PBM name headers
        for (Pbm pbm : pbms) {
            Div pbmHeader = new Div();
            pbmHeader.setText(pbm.getName());
            pbmHeader.getStyle().set("font-weight", "bold");
            pbmHeader.getStyle().set("flex", "1");
            pbmHeader.getStyle().set("text-align", "center");
            headerLayout.add(pbmHeader);
        }

        container.add(headerLayout);

        // Find all unique warehouse items to compare
        int maxItems = pbms.stream()
                .mapToInt(pbm -> pbm.getWarehouseItems() != null ? pbm.getWarehouseItems().size() : 0).max().orElse(0);

        if (maxItems > 0) {
            for (int i = 0; i < maxItems; i++) {
                final int itemIndex = i;

                // Warehouse Number row
                HorizontalLayout numberRow = createWarehousePropertyRow("Warehouse Number " + (i + 1), pbms, itemIndex,
                        item -> item.getWarehouseNumber() != null ? item.getWarehouseNumber() : "");
                container.add(numberRow);

                // Variant Text row
                HorizontalLayout variantRow = createWarehousePropertyRow("Variant " + (i + 1), pbms, itemIndex,
                        item -> item.getVariantText() != null ? item.getVariantText() : "");
                container.add(variantRow);

                if (i < maxItems - 1) {
                    // Add separator
                    Div separator = new Div();
                    separator.getStyle().set("height", "1px");
                    separator.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
                    separator.getStyle().set("margin", "var(--lumo-space-xs) 0");
                    container.add(separator);
                }
            }
        } else {
            HorizontalLayout noDataRow = new HorizontalLayout();
            noDataRow.setSpacing(true);
            noDataRow.setWidthFull();

            Div noDataLabel = new Div();
            noDataLabel.setText("No warehouse items");
            noDataLabel.getStyle().set("width", "200px");
            noDataLabel.getStyle().set("flex-shrink", "0");
            noDataRow.add(noDataLabel);

            for (int i = 0; i < pbms.size(); i++) {
                Div noDataValue = new Div();
                noDataValue.setText("No warehouse items available");
                noDataValue.getStyle().set("flex", "1");
                noDataValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noDataRow.add(noDataValue);
            }

            container.add(noDataRow);
        }

        Div wrapper = new Div(container);
        wrapper.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        wrapper.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        wrapper.getStyle().set("padding", "var(--lumo-space-m)");
        return wrapper;
    }

    private HorizontalLayout createWarehousePropertyRow(String property, List<Pbm> pbms, int itemIndex,
            java.util.function.Function<WarehouseItem, String> valueExtractor) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();

        // Property name
        Div propertyDiv = new Div();
        propertyDiv.setText(property);
        propertyDiv.getStyle().set("font-weight", "bold");
        propertyDiv.getStyle().set("width", "200px");
        propertyDiv.getStyle().set("flex-shrink", "0");
        row.add(propertyDiv);

        // Values for each PBM
        for (Pbm pbm : pbms) {
            Div valueDiv = new Div();
            String value = "";
            if (pbm.getWarehouseItems() != null && pbm.getWarehouseItems().size() > itemIndex) {
                WarehouseItem item = pbm.getWarehouseItems().toArray(new WarehouseItem[0])[itemIndex];
                value = valueExtractor.apply(item);
            }
            valueDiv.setText(value.isEmpty() ? "-" : value);
            valueDiv.getStyle().set("flex", "1");
            if (value.isEmpty()) {
                valueDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
            }
            row.add(valueDiv);
        }

        return row;
    }

    private void addAccordionIfHasData(VerticalLayout container, Details accordion) {
        if (accordion != null) {
            container.add(accordion);
        }
    }

    private Details createDescriptionAccordion(List<Pbm> pbms) {
        // Check if any PBM has description
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> pbm.getDescription() != null && !pbm.getDescription().isBlank());

        if (!hasData)
            return null;

        Details accordion = new Details("Description");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            Div descDiv = new Div();
            if (pbm.getDescription() != null && !pbm.getDescription().isBlank()) {
                descDiv.getElement().setProperty("innerHTML", pbm.getDescription());
            } else {
                descDiv.setText("-");
                descDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                descDiv.getStyle().set("font-style", "italic");
            }
            column.add(descDiv);
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createCategoriesAccordion(List<Pbm> pbms) {
        // Check if any PBM has categories
        boolean hasData = pbms.stream().anyMatch(pbm -> pbm.getCategories() != null && !pbm.getCategories().isEmpty());

        if (!hasData)
            return null;

        Details accordion = new Details("Categories");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            if (pbm.getCategories() != null && !pbm.getCategories().isEmpty()) {
                for (Category category : pbm.getCategories()) {
                    Span categorySpan = new Span(category.getName());
                    categorySpan.getStyle().set("display", "block");
                    categorySpan.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
                    column.add(categorySpan);
                }
            } else {
                Div noData = new Div("-");
                noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noData.getStyle().set("font-style", "italic");
                column.add(noData);
            }
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createProtectsAgainstAccordion(List<Pbm> pbms) {
        // Check if any PBM has protects against data
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank());

        if (!hasData)
            return null;

        Details accordion = new Details("Protects Against");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            Div contentDiv = new Div();
            if (pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank()) {
                contentDiv.getElement().setProperty("innerHTML", pbm.getProtectsAgainst());
            } else {
                contentDiv.setText("-");
                contentDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                contentDiv.getStyle().set("font-style", "italic");
            }
            column.add(contentDiv);
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createDoesNotProtectAgainstAccordion(List<Pbm> pbms) {
        // Check if any PBM has does not protect against data
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank());

        if (!hasData)
            return null;

        Details accordion = new Details("Does Not Protect");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            Div contentDiv = new Div();
            if (pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank()) {
                contentDiv.getElement().setProperty("innerHTML", pbm.getDoesNotProtectAgainst());
            } else {
                contentDiv.setText("-");
                contentDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                contentDiv.getStyle().set("font-style", "italic");
            }
            column.add(contentDiv);
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createNotesAccordion(List<Pbm> pbms) {
        // Check if any PBM has notes or documents with type 2
        boolean hasData = pbms.stream().anyMatch(pbm -> (pbm.getNotes() != null && !pbm.getNotes().isBlank()) ||
                (pbm.getDocuments() != null
                        && pbm.getDocuments().stream().anyMatch(doc -> "2".equals(doc.getDocumentType()))));

        if (!hasData)
            return null;

        Details accordion = new Details("Notes");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            // Add notes content
            if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
                Div contentDiv = new Div();
                contentDiv.getElement().setProperty("innerHTML", pbm.getNotes());
                column.add(contentDiv);
            }

            // Add documents with type 2
            if (pbm.getDocuments() != null) {
                var type2Documents = pbm.getDocuments().stream()
                        .filter(doc -> "2".equals(doc.getDocumentType()))
                        .toList();
                if (!type2Documents.isEmpty()) {
                    if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
                        // Add separator if we have both content and documents
                        column.add(new com.vaadin.flow.component.html.Hr());
                    }
                    VerticalLayout documentsDiv = createCompactDownloadableDocuments(type2Documents);
                    column.add(documentsDiv);
                }
            }

            // Show placeholder if no content at all
            if ((pbm.getNotes() == null || pbm.getNotes().isBlank()) &&
                    (pbm.getDocuments() == null
                            || pbm.getDocuments().stream().noneMatch(doc -> "2".equals(doc.getDocumentType())))) {
                Div noContent = new Div("-");
                noContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noContent.getStyle().set("font-style", "italic");
                column.add(noContent);
            }

            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createUsageInstructionsAccordion(List<Pbm> pbms) {
        // Check if any PBM has usage instructions or documents with type 1
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) ||
                        (pbm.getDocuments() != null
                                && pbm.getDocuments().stream().anyMatch(doc -> "1".equals(doc.getDocumentType()))));

        if (!hasData)
            return null;

        Details accordion = new Details("Usage Instructions");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            // Add usage instructions content
            if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
                Div contentDiv = new Div();
                contentDiv.getElement().setProperty("innerHTML", pbm.getUsageInstructions());
                column.add(contentDiv);
            }

            // Add documents with type 1
            if (pbm.getDocuments() != null) {
                var type1Documents = pbm.getDocuments().stream()
                        .filter(doc -> "1".equals(doc.getDocumentType()))
                        .toList();
                if (!type1Documents.isEmpty()) {
                    if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
                        // Add separator if we have both content and documents
                        column.add(new com.vaadin.flow.component.html.Hr());
                    }
                    VerticalLayout documentsDiv = createCompactDownloadableDocuments(type1Documents);
                    column.add(documentsDiv);
                }
            }

            // Show placeholder if no content at all
            if ((pbm.getUsageInstructions() == null || pbm.getUsageInstructions().isBlank()) &&
                    (pbm.getDocuments() == null
                            || pbm.getDocuments().stream().noneMatch(doc -> "1".equals(doc.getDocumentType())))) {
                Div noContent = new Div("-");
                noContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noContent.getStyle().set("font-style", "italic");
                column.add(noContent);
            }

            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createDistributionAccordion(List<Pbm> pbms) {
        // Check if any PBM has distribution data
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> pbm.getDistribution() != null && !pbm.getDistribution().isBlank());

        if (!hasData)
            return null;

        Details accordion = new Details("Distribution");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            Div contentDiv = new Div();
            if (pbm.getDistribution() != null && !pbm.getDistribution().isBlank()) {
                contentDiv.getElement().setProperty("innerHTML", pbm.getDistribution());
            } else {
                contentDiv.setText("-");
                contentDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
                contentDiv.getStyle().set("font-style", "italic");
            }
            column.add(contentDiv);
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createStandardsAccordion(List<Pbm> pbms) {
        // Check if any PBM has standards data or norms
        boolean hasData = pbms.stream().anyMatch(pbm -> (pbm.getStandards() != null && !pbm.getStandards().isBlank()) ||
                (pbm.getNorms() != null && !pbm.getNorms().isEmpty()));

        if (!hasData)
            return null;

        Details accordion = new Details("Standards");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            // Add standards content
            if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
                Div contentDiv = new Div();
                contentDiv.getElement().setProperty("innerHTML", pbm.getStandards());
                column.add(contentDiv);
            }

            // Add norms
            if (pbm.getNorms() != null && !pbm.getNorms().isEmpty()) {
                if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
                    // Add separator if we have both content and norms
                    column.add(new com.vaadin.flow.component.html.Hr());
                }
                VerticalLayout normsDiv = createCompactDownloadableNorms(pbm.getNorms());
                column.add(normsDiv);
            }

            // Show placeholder if no content at all
            if ((pbm.getStandards() == null || pbm.getStandards().isBlank()) &&
                    (pbm.getNorms() == null || pbm.getNorms().isEmpty())) {
                Div noContent = new Div("-");
                noContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noContent.getStyle().set("font-style", "italic");
                column.add(noContent);
            }

            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private Details createWarehouseNumbersAccordion(List<Pbm> pbms) {
        // Check if any PBM has warehouse items
        boolean hasData = pbms.stream()
                .anyMatch(pbm -> pbm.getWarehouseItems() != null && !pbm.getWarehouseItems().isEmpty());

        if (!hasData)
            return null;

        Details accordion = new Details("Warehouse");
        accordion.setWidthFull();
        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        for (Pbm pbm : pbms) {
            VerticalLayout column = new VerticalLayout();
            column.getStyle().set("flex", "1");
            column.setPadding(true);
            column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            column.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

            if (pbm.getWarehouseItems() != null && !pbm.getWarehouseItems().isEmpty()) {
                // Create a compact table for warehouse items
                Grid<WarehouseItem> grid = new Grid<>(WarehouseItem.class, false);
                grid.addColumn(WarehouseItem::getWarehouseNumber).setHeader("Warehouse Number").setFlexGrow(1);
                grid.addColumn(WarehouseItem::getVariantText).setHeader("Variant").setFlexGrow(1);
                grid.setItems(pbm.getWarehouseItems());
                grid.setAllRowsVisible(true);
                grid.setWidthFull();

                column.add(grid);
            } else {
                Div noData = new Div("-");
                noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noData.getStyle().set("font-style", "italic");
                column.add(noData);
            }
            content.add(column);
        }

        accordion.add(content);
        return accordion;
    }

    private VerticalLayout createCompactDownloadableDocuments(List<Document> documents) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        if (documents.isEmpty()) {
            Div noData = new Div("No documents available");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            noData.getStyle().set("font-style", "italic");
            layout.add(noData);
        } else {
            for (Document doc : documents) {
                if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
                    HorizontalLayout docLayout = new HorizontalLayout();
                    docLayout.setSpacing(true);
                    docLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                    docLayout.setPadding(true);
                    docLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
                    docLayout.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
                    docLayout.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
                    docLayout.setWidthFull();

                    // File icon
                    Span icon = new Span();
                    icon.getElement().setAttribute("class", "las la-file-download");
                    icon.getStyle().set("font-size", "1.2em");
                    icon.getStyle().set("color", "var(--lumo-primary-color)");

                    // File info
                    VerticalLayout infoLayout = new VerticalLayout();
                    infoLayout.setPadding(false);
                    infoLayout.setSpacing(false);
                    infoLayout.setFlexGrow(1);

                    // File name/link
                    String fileName = doc.getFilePath();
                    if (fileName != null && fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }

                    Anchor fileLink = new Anchor("/static/" + doc.getFilePath(),
                            fileName != null ? fileName : "Download");
                    fileLink.getElement().setAttribute("download", true);
                    fileLink.getElement().setAttribute("target", "_blank");
                    fileLink.getStyle().set("text-decoration", "none");
                    fileLink.getStyle().set("color", "var(--lumo-primary-color)");
                    fileLink.getStyle().set("font-weight", "500");
                    fileLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    infoLayout.add(fileLink);

                    // Description if available
                    if (doc.getDescription() != null && !doc.getDescription().isBlank()) {
                        Span description = new Span(doc.getDescription());
                        description.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
                        infoLayout.add(description);
                    }

                    docLayout.add(icon, infoLayout);
                    layout.add(docLayout);
                }
            }
        }

        return layout;
    }

    private VerticalLayout createCompactDownloadableNorms(Set<Norm> norms) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        if (norms == null || norms.isEmpty()) {
            Div noData = new Div("No norms available");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            noData.getStyle().set("font-style", "italic");
            layout.add(noData);
        } else {
            for (Norm norm : norms) {
                HorizontalLayout normLayout = new HorizontalLayout();
                normLayout.setSpacing(true);
                normLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                normLayout.setPadding(true);
                normLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
                normLayout.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
                normLayout.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
                normLayout.setWidthFull();

                // File icon
                Span icon = new Span();
                icon.getElement().setAttribute("class", "las la-file-download");
                icon.getStyle().set("font-size", "1.2em");
                icon.getStyle().set("color", "var(--lumo-primary-color)");

                // Norm info layout
                VerticalLayout infoLayout = new VerticalLayout();
                infoLayout.setPadding(false);
                infoLayout.setSpacing(false);
                infoLayout.setFlexGrow(1);

                // Norm name
                if (norm.getName() != null && !norm.getName().isBlank()) {
                    H4 nameHeader = new H4(norm.getName());
                    nameHeader.getStyle().set("margin", "0 0 2px 0");
                    nameHeader.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    infoLayout.add(nameHeader);
                }

                // File link (if available)
                if (norm.getFilePath() != null && !norm.getFilePath().isBlank()) {
                    String fileName = norm.getFilePath();
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }

                    Anchor fileLink = new Anchor("/static/" + norm.getFilePath(), "Download: " + fileName);
                    fileLink.getElement().setAttribute("download", true);
                    fileLink.getElement().setAttribute("target", "_blank");
                    fileLink.getStyle().set("text-decoration", "none");
                    fileLink.getStyle().set("color", "var(--lumo-primary-color)");
                    fileLink.getStyle().set("font-weight", "500");
                    fileLink.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                    infoLayout.add(fileLink);
                }

                // Description if available
                if (norm.getDescription() != null && !norm.getDescription().isBlank()) {
                    Span description = new Span(norm.getDescription());
                    description.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                    description.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    infoLayout.add(description);
                }

                normLayout.add(icon, infoLayout);
                layout.add(normLayout);
            }
        }

        return layout;
    }

}
