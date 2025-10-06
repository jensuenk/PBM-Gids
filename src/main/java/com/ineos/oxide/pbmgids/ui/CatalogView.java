package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.managers.ComparisonManager;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.ineos.oxide.pbmgids.services.CategoryDataService;
import com.ineos.oxide.pbmgids.ui.components.LayoutToggleComponent;
import com.ineos.oxide.pbmgids.ui.components.PbmGridComponent;
import com.ineos.oxide.pbmgids.ui.components.PbmTileLayout;
import com.ineos.oxide.pbmgids.ui.components.SearchComponent;
import com.ineos.oxide.pbmgids.ui.dialogs.PbmComparisonDialog;
import com.ineos.oxide.pbmgids.ui.dialogs.PbmDetailsDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Main catalog view that displays PBMs for a specific category.
 * This view is accessed via URL: /catalog/{categoryId}
 * 
 * Refactored to use separate component classes for better separation of
 * concerns.
 */
@Route(value = "catalog/:categoryId", layout = MainView.class)
@PageTitle("")
@AnonymousAllowed
public class CatalogView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger logger = Logger.getLogger(CatalogView.class.getName());

    // Components
    private final H2 categoryTitle;
    private final SearchComponent searchField;
    private final LayoutToggleComponent layoutToggle;
    private final PbmGridComponent pbmGrid;
    private final PbmTileLayout tileLayout;
    private final Button compareButton;
    private final HorizontalLayout toolbarLayout;
    private final HorizontalLayout searchLayout;

    // Services and managers
    private final CategoryDataService categoryDataService;
    private final ComparisonManager comparisonManager;

    // Dialogs
    private final PbmDetailsDialog detailsDialog;
    private final PbmComparisonDialog comparisonDialog;

    // State
    private Integer currentCategoryId;
    private List<Pbm> allPbms = new ArrayList<>();
    private String currentSearchFilter = "";

    public CatalogView(CatalogService catalogService) {
        // Initialize services and managers
        this.categoryDataService = new CategoryDataService(catalogService);
        this.comparisonManager = new ComparisonManager(this::onComparisonSelectionChanged);

        // Initialize components
        this.categoryTitle = new H2("PBM Catalog");
        this.searchField = new SearchComponent("Search PBMs...");
        this.layoutToggle = new LayoutToggleComponent();
        this.pbmGrid = new PbmGridComponent();
        this.tileLayout = new PbmTileLayout();
        this.compareButton = new Button("Compare Selected", VaadinIcon.SCALE.create());
        this.toolbarLayout = new HorizontalLayout();
        this.searchLayout = new HorizontalLayout();

        // Initialize dialogs
        this.detailsDialog = new PbmDetailsDialog(catalogService);
        this.comparisonDialog = new PbmComparisonDialog(catalogService);

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
        setupComponents();
        setupLayout();
        setSizeFull();
    }

    private void setupComponents() {
        setupSearch();
        setupLayoutToggle();
        setupGrid();
        setupTileLayout();
        setupToolbar();
    }

    private void setupLayout() {
        setupSearchLayout();
        add(searchLayout, categoryTitle, toolbarLayout, pbmGrid);
        tileLayout.setVisible(false);
    }

    private void setupSearch() {
        searchField.setSuggestionProvider(
                filter -> filter.length() >= 2 ? categoryDataService.getPbmNameSuggestions(filter) : List.of());
        searchField.setSearchHandler(this::performSearch);
    }

    private void setupSearchLayout() {
        searchLayout.setWidthFull();
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchLayout.add(searchField, layoutToggle);
        searchLayout.setFlexGrow(1, searchField);
    }

    private void setupLayoutToggle() {
        layoutToggle.setOnToggle(this::onLayoutToggle);
    }

    private void setupTileLayout() {
        tileLayout.setComparisonManager(comparisonManager);
        tileLayout.setOnDetailsClick(this::showPbmDetails);
    }

    private void setupGrid() {
        pbmGrid.setComparisonManager(comparisonManager);
        pbmGrid.setOnDetailsClick(this::showPbmDetails);
    }

    private void onLayoutToggle(LayoutToggleComponent.LayoutType layoutType) {
        boolean showTable = layoutType == LayoutToggleComponent.LayoutType.TABLE;

        if (showTable) {
            switchToTableView();
        } else {
            switchToTileView();
        }
    }

    private void switchToTableView() {
        tileLayout.setVisible(false);
        pbmGrid.setVisible(true);
        remove(tileLayout);
        if (!getChildren().anyMatch(component -> component == pbmGrid)) {
            add(pbmGrid);
        }
    }

    private void switchToTileView() {
        pbmGrid.setVisible(false);
        tileLayout.setVisible(true);
        remove(pbmGrid);
        if (!getChildren().anyMatch(component -> component == tileLayout)) {
            add(tileLayout);
        }
        updateTileLayout();
    }

    private void updateTileLayout() {
        List<Pbm> filteredPbms = getFilteredPbms();
        tileLayout.updateTiles(filteredPbms);
    }

    private List<Pbm> getFilteredPbms() {
        if (currentSearchFilter == null || currentSearchFilter.trim().isEmpty()) {
            return allPbms;
        } else {
            return categoryDataService.searchAllPbms(currentSearchFilter.trim());
        }
    }

    private void setupToolbar() {
        compareButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        compareButton.setEnabled(false);
        compareButton.addClickListener(e -> showComparisonDialog());

        Span infoText = new Span("Select up to " + ComparisonManager.getMaxComparisonItems() + " PBMs to compare");
        infoText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        infoText.getStyle().set("font-size", "var(--lumo-font-size-s)");

        toolbarLayout.add(compareButton, infoText);
        toolbarLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbarLayout.setSpacing(true);
    }

    private void onComparisonSelectionChanged(List<Pbm> selectedPbms) {
        updateCompareButtonState(selectedPbms);

        // Refresh grid checkboxes if in table view
        if (layoutToggle.isTableView()) {
            pbmGrid.refreshCheckboxes();
        }
    }

    private void updateCompareButtonState(List<Pbm> selectedPbms) {
        compareButton.setEnabled(selectedPbms.size() >= 2);
        compareButton.setText("Compare Selected (" + selectedPbms.size() + ")");
    }

    private void loadPbmsForCategory(Integer categoryId) {
        allPbms = categoryDataService.loadPbmsByCategory(categoryId);
        pbmGrid.setItems(allPbms);
        if (layoutToggle.isTileView()) {
            updateTileLayout();
        }
        searchField.clear();
        comparisonManager.clearSelection();
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
        currentSearchFilter = searchTerm;
        List<Pbm> filteredPbms = getFilteredPbms();
        pbmGrid.setItems(filteredPbms);

        if (layoutToggle.isTileView()) {
            updateTileLayout();
        }
    }

    private void handleInvalidCategory() {
        this.currentCategoryId = null;
        categoryTitle.setText("PBM Catalog - Invalid Category");
        allPbms.clear();
        pbmGrid.setItems();
        if (layoutToggle.isTileView()) {
            updateTileLayout();
        }
    }

    private void showPbmDetails(Pbm pbm) {
        if (pbm != null) {
            detailsDialog.showPbm(pbm);
        }
    }

    private void showComparisonDialog() {
        if (!comparisonManager.canCompare()) {
            Notification.show("Please select at least 2 PBMs to compare")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        comparisonDialog.showComparison(comparisonManager.getSelectedPbms());
    }
}
