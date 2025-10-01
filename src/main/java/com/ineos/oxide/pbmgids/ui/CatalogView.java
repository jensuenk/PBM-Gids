package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.model.entities.Document;
import com.ineos.oxide.pbmgids.model.entities.Norm;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.model.entities.WarehouseItem;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.services.CategoryDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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

    // Services
    private final CategoryDataService categoryDataService;

    // State
    private Integer currentCategoryId;
    private List<Pbm> allPbms = new ArrayList<>();
    private String currentSearchFilter = "";

    public CatalogView(CatalogService catalogService) {
        this.categoryDataService = new CategoryDataService(catalogService);
        this.categoryTitle = new H2("PBM Catalog");
        this.searchField = new ComboBox<>();
        this.pbmGrid = new Grid<>(Pbm.class, false);

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

        // Setup PBM grid columns
        setupGridColumns();

        // Add components to view
        add(categoryTitle, searchField, pbmGrid);
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

    private void setupGridColumns() {
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

    private Button createDetailsButton(Pbm pbm) {
        Button btn = new Button("Details");
        btn.addClickListener(ev -> showPbmDetails(pbm));
        return btn;
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

        // Notes tab
        if (pbm.getNotes() != null && !pbm.getNotes().isBlank()) {
            Div notesContent = createHtmlContent(pbm.getNotes());
            tabSheet.add("Notes", notesContent);
        }

        // Usage Instructions tab
        if (pbm.getUsageInstructions() != null && !pbm.getUsageInstructions().isBlank()) {
            Div usageContent = createHtmlContent(pbm.getUsageInstructions());
            tabSheet.add("Usage Instructions", usageContent);
        }

        // Distribution tab
        if (pbm.getDistribution() != null && !pbm.getDistribution().isBlank()) {
            Div distributionContent = createHtmlContent(pbm.getDistribution());
            tabSheet.add("Distribution", distributionContent);
        }

        // Standards tab
        if (pbm.getStandards() != null && !pbm.getStandards().isBlank()) {
            Div standardsContent = createHtmlContent(pbm.getStandards());
            tabSheet.add("Standards", standardsContent);
        }

        // Documents tab
        if (pbm.getDocuments() != null && !pbm.getDocuments().isEmpty()) {
            Div documentsContent = createDocumentsTable(pbm.getDocuments());
            tabSheet.add("Documents", documentsContent);
        }

        // Norms tab
        if (pbm.getNorms() != null && !pbm.getNorms().isEmpty()) {
            Div normsContent = createNormsTable(pbm.getNorms());
            tabSheet.add("Norms", normsContent);
        }

        // Warehouse Items tab
        if (pbm.getWarehouseItems() != null && !pbm.getWarehouseItems().isEmpty()) {
            Div warehouseContent = createWarehouseTable(pbm.getWarehouseItems());
            tabSheet.add("Warehouse", warehouseContent);
        }

        // Close button - always at bottom
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.getStyle().set("align-self", "center");

        // Dialog layout with fixed footer
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();

        dialogLayout.add(headerLayout); // Header with image and basic info
        dialogLayout.addAndExpand(tabSheet); // Tabs take all available remaining space
        dialogLayout.add(closeButton); // This stays at the bottom

        dialog.add(dialogLayout);

        return dialog;
    }

    private Div createHtmlContent(String htmlContent) {
        Div content = new Div();
        content.getElement().setProperty("innerHTML", htmlContent);
        content.setSizeFull();
        content.getStyle().set("overflow", "auto");
        content.getStyle().set("padding", "10px");
        return content;
    }

    private Div createDocumentsTable(java.util.Set<Document> documents) {
        Grid<Document> grid = new Grid<>(Document.class, false);
        grid.addColumn(Document::getDocumentType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(Document::getFilePath).setHeader("File Path").setAutoWidth(true);
        grid.addColumn(Document::getMimeType).setHeader("MIME Type").setAutoWidth(true);
        grid.addColumn(Document::getDescription).setHeader("Description").setAutoWidth(true);
        grid.setItems(documents);
        grid.setMaxHeight("300px");

        Div container = new Div(grid);
        container.setSizeFull();
        return container;
    }

    private Div createNormsTable(java.util.Set<Norm> norms) {
        Grid<Norm> grid = new Grid<>(Norm.class, false);
        grid.addColumn(Norm::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Norm::getDescription).setHeader("Description").setAutoWidth(true);
        grid.setItems(norms);
        grid.setMaxHeight("300px");

        Div container = new Div(grid);
        container.setSizeFull();
        return container;
    }

    private Div createWarehouseTable(java.util.Set<WarehouseItem> warehouseItems) {
        Grid<WarehouseItem> grid = new Grid<>(WarehouseItem.class, false);
        grid.addColumn(WarehouseItem::getWarehouseNumber).setHeader("Warehouse Number").setAutoWidth(true);
        grid.addColumn(WarehouseItem::getVariantText).setHeader("Variant").setAutoWidth(true);
        grid.addColumn(item -> item.getPublished() != null ? item.getPublished().toString() : "")
                .setHeader("Published").setAutoWidth(true);
        grid.setItems(warehouseItems);
        grid.setMaxHeight("300px");

        Div container = new Div(grid);
        container.setSizeFull();
        return container;
    }

}
