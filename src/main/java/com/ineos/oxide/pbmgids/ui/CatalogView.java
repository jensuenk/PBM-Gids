package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.services.CategoryDataService;
import com.ineos.oxide.pbmgids.ui.dialogs.PbmComparisonDialog;
import com.ineos.oxide.pbmgids.ui.dialogs.PbmDetailsDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Main catalog view that displays PBMs for a specific category.
 * This view is accessed via URL: /catalog/{categoryId}
 * 
 * Refactored to use separate dialog components for clean separation of
 * concerns.
 */
@Route(value = "catalog/:categoryId", layout = MainView.class)
@PageTitle("")
@AnonymousAllowed
public class CatalogView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger logger = Logger.getLogger(CatalogView.class.getName());
    private static final int MAX_COMPARISON_ITEMS = 3;

    // Components
    private final H2 categoryTitle;
    private final ComboBox<String> searchField;
    private final Grid<Pbm> pbmGrid;
    private final Button compareButton;
    private final HorizontalLayout toolbarLayout;

    // Services
    private final CategoryDataService categoryDataService;

    // Dialogs
    private final PbmDetailsDialog detailsDialog;
    private final PbmComparisonDialog comparisonDialog;

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

        // Initialize dialogs
        this.detailsDialog = new PbmDetailsDialog();
        this.comparisonDialog = new PbmComparisonDialog();

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
            handleInvalidCategory();
        }
    }

    private void initializeView() {
        setPadding(true);
        setupSearchField();
        setupToolbar();
        setupGridColumns();
        add(searchField, categoryTitle, toolbarLayout, pbmGrid);
        setSizeFull();
    }

    private void setupSearchField() {
        searchField.setPlaceholder("Search PBMs...");
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setAllowCustomValue(true);

        // Configure autocomplete behavior
        searchField.addCustomValueSetListener(event -> {
            String customValue = event.getDetail();
            searchField.setValue(customValue);
            performSearch(customValue);
        });

        searchField.addValueChangeListener(event -> performSearch(event.getValue()));

        // Set up lazy data provider for dynamic suggestions
        searchField.setItems(query -> {
            String filter = query.getFilter().orElse("");
            currentSearchFilter = filter;

            if (filter.length() < 2) {
                return java.util.stream.Stream.empty();
            }

            List<String> suggestions = categoryDataService.getPbmNameSuggestions(filter);
            return suggestions.stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });

        searchField.setRenderer(new ComponentRenderer<>(this::createHighlightedSuggestion));
    }

    private void setupToolbar() {
        compareButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        compareButton.setEnabled(false);
        compareButton.addClickListener(e -> showComparisonDialog());

        Span infoText = new Span("Select up to " + MAX_COMPARISON_ITEMS + " PBMs to compare");
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

    private Checkbox createCompareCheckbox(Pbm pbm) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(selectedPbmsForComparison.contains(pbm));
        checkbox.addValueChangeListener(event -> handleComparisonSelection(event.getValue(), pbm, checkbox));
        return checkbox;
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
            container.setText(suggestion);
        } else {
            // Create highlighted text
            if (startIndex > 0) {
                container.add(new Span(suggestion.substring(0, startIndex)));
            }

            Span boldMatch = new Span(suggestion.substring(startIndex, startIndex + currentSearchFilter.length()));
            boldMatch.getStyle().set("font-weight", "bold");
            container.add(boldMatch);

            if (startIndex + currentSearchFilter.length() < suggestion.length()) {
                container.add(new Span(suggestion.substring(startIndex + currentSearchFilter.length())));
            }
        }

        return container;
    }

    private void handleComparisonSelection(boolean selected, Pbm pbm, Checkbox checkbox) {
        if (selected) {
            if (!selectedPbmsForComparison.contains(pbm)) {
                if (selectedPbmsForComparison.size() < MAX_COMPARISON_ITEMS) {
                    selectedPbmsForComparison.add(pbm);
                } else {
                    checkbox.setValue(false);
                    Notification.show("You can only compare up to " + MAX_COMPARISON_ITEMS + " PBMs")
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }
        } else {
            selectedPbmsForComparison.remove(pbm);
        }
        updateCompareButtonState();
    }

    private void updateCompareButtonState() {
        compareButton.setEnabled(selectedPbmsForComparison.size() >= 2);
        compareButton.setText("Compare Selected (" + selectedPbmsForComparison.size() + ")");
    }

    private void loadPbmsForCategory(Integer categoryId) {
        allPbms = categoryDataService.loadPbmsByCategory(categoryId);
        pbmGrid.setItems(allPbms);
        searchField.clear();
        clearComparisonSelection();
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
            pbmGrid.setItems(allPbms);
        } else {
            List<Pbm> searchResults = categoryDataService.searchAllPbms(searchTerm.trim());
            pbmGrid.setItems(searchResults);
        }
    }

    private void handleInvalidCategory() {
        this.currentCategoryId = null;
        categoryTitle.setText("PBM Catalog - Invalid Category");
        allPbms.clear();
        pbmGrid.setItems();
    }

    private void clearComparisonSelection() {
        selectedPbmsForComparison.clear();
        updateCompareButtonState();
    }

    private void showPbmDetails(Pbm pbm) {
        if (pbm != null) {
            detailsDialog.showPbm(pbm);
        }
    }

    private void showComparisonDialog() {
        if (selectedPbmsForComparison.size() < 2) {
            Notification.show("Please select at least 2 PBMs to compare")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        comparisonDialog.showComparison(selectedPbmsForComparison);
    }

    private String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }

        // Remove leading slash if present
        String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // Construct the static resource URL
        return "/images/" + cleanPath;
    }
}
