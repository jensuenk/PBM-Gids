package com.ineos.oxide.pbmgids.ui;

import java.util.logging.Logger;

import com.ineos.oxide.base.ui.IneosAppLayout;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "catalog", layout = MainView.class)
@PageTitle("PBM Catalog")
@AnonymousAllowed
public class CatalogView extends IneosAppLayout {
    private static final long serialVersionUID = 1L;

    private final CatalogService catalogService;

    // Removed categoryTree as it's now in MainView
    private final Grid<Pbm> pbmGrid = new Grid<>(Pbm.class, false);
    private final Div details = new Div();
    private final Tabs relationTabs = new Tabs();
    private final Div relationContent = new Div();

    private Pbm currentSelection;

    public CatalogView(CatalogService catalogService) {
        this.catalogService = catalogService;
        initUI();

        // Check if there's a selected category from the session
        handleInitialCategorySelection();
    }

    private void initUI() {
        setPadding(true);

        // Main: PBM grid (no longer need category tree here)
        pbmGrid.addColumn(new ComponentRenderer<>(item -> {
            if (item.getImage() == null || item.getImage().isBlank())
                return new Div();
            Image img = new Image(item.getImage(), item.getName());
            img.setAlt(item.getName());
            img.setWidth("64px");
            return img;
        })).setHeader("").setAutoWidth(true).setFlexGrow(0).setWidth("80px");
        pbmGrid.addColumn(Pbm::getName).setHeader("Name").setAutoWidth(true);
        pbmGrid.addColumn(Pbm::getTypeName).setHeader("Type").setAutoWidth(true);
        pbmGrid.addColumn(Pbm::getBrand).setHeader("Brand").setAutoWidth(true);
        pbmGrid.addColumn(new ComponentRenderer<>(pbm -> {
            com.vaadin.flow.component.button.Button btn = new com.vaadin.flow.component.button.Button("Details");
            btn.addClickListener(ev -> {
                pbmGrid.select(pbm);
                showDetails(pbm);
            });
            return btn;
        })).setHeader("").setAutoWidth(true).setFlexGrow(0);
        pbmGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        pbmGrid.asSingleSelect().addValueChangeListener(e -> showDetails(e.getValue()));

        // Right: details + relations tabs
        details.getStyle().set("padding", "var(--lumo-space-m)");
        details.setMinHeight("200px"); // Ensure details section has minimum height

        // Initialize tabs
        Tab tabDocs = new Tab("Documents");
        Tab tabNorms = new Tab("Norms");
        Tab tabWarehouse = new Tab("Warehouse");
        relationTabs.add(tabDocs, tabNorms, tabWarehouse);
        relationTabs.setSelectedTab(tabDocs); // Set default selection
        relationTabs.addSelectedChangeListener(e -> refreshRelations());

        relationContent.setSizeFull();
        relationContent.getStyle().set("min-height", "300px"); // Ensure relation content has minimum height

        VerticalLayout right = new VerticalLayout(details, relationTabs, relationContent);
        right.setWidth(35, Unit.PERCENTAGE);
        right.setSpacing(false);
        right.setPadding(false);
        right.setAlignItems(Alignment.STRETCH);
        right.setFlexGrow(0, details);
        right.setFlexGrow(0, relationTabs);
        right.setFlexGrow(1, relationContent); // Allow relation content to expand

        // Use horizontal layout with PBM grid and details panel
        HorizontalLayout main = new HorizontalLayout(pbmGrid, right);
        main.setSizeFull();
        main.setFlexGrow(1, pbmGrid);
        main.setFlexGrow(0, right);

        add(main);
        setSizeFull();

        // Initialize with clean state
        showDetails(null);
    }

    private void handleInitialCategorySelection() {
        // Check if there's a selected category from the MainView navigation
        getUI().ifPresent(ui -> {
            Object categoryId = ui.getSession().getAttribute("selectedCategoryId");
            if (categoryId instanceof Integer) {
                loadCategoryById((Integer) categoryId);
                // Clear the session attribute after using it
                ui.getSession().setAttribute("selectedCategoryId", null);
            }
        });
    }

    private void loadCategoryById(Integer categoryId) {
        pbmGrid.setItems(catalogService.getByCategory(categoryId));
        // Clear current selection and details when loading new category
        pbmGrid.deselectAll();
        showDetails(null);
    }

    private void showDetails(Pbm pbm) {
        if (pbm == null) {
            details.removeAll();
            relationContent.removeAll();
            currentSelection = null;
            return;
        }
        Logger.getLogger(CatalogView.class.getName()).info("PBM selected: " + pbm.getName());
        currentSelection = pbm;
        details.removeAll();
        VerticalLayout wrap = new VerticalLayout();
        wrap.setSpacing(false);
        wrap.setPadding(false);
        if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
            Image img = new Image(pbm.getImage(), pbm.getName());
            img.setWidth("160px");
            wrap.add(img);
        }
        wrap.add(new com.vaadin.flow.component.html.H3(pbm.getName()));
        if (pbm.getDescription() != null && !pbm.getDescription().isBlank())
            wrap.add(new com.vaadin.flow.component.html.Paragraph(pbm.getDescription()));
        if (pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank())
            wrap.add(new com.vaadin.flow.component.html.Paragraph("Protects: " + pbm.getProtectsAgainst()));
        if (pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank())
            wrap.add(new com.vaadin.flow.component.html.Paragraph(
                    "Does not protect: " + pbm.getDoesNotProtectAgainst()));
        details.add(wrap);
        refreshRelations();
    }

    private void refreshRelations() {
        relationContent.removeAll();
        if (currentSelection == null) {
            relationContent.add(new com.vaadin.flow.component.html.Span("Select a PBM to view related information"));
            return;
        }

        Tab selected = relationTabs.getSelectedTab();
        String label = selected != null ? selected.getLabel() : "Documents";
        Logger.getLogger(CatalogView.class.getName()).info("Refreshing relations tab: " + label);

        switch (label) {
            case "Norms":
                Grid<com.ineos.oxide.pbmgids.model.entities.Norm> ng = new Grid<>(
                        com.ineos.oxide.pbmgids.model.entities.Norm.class, false);
                ng.addColumn(com.ineos.oxide.pbmgids.model.entities.Norm::getName).setHeader("Name").setAutoWidth(true);
                ng.addColumn(com.ineos.oxide.pbmgids.model.entities.Norm::getDescription).setHeader("Description")
                        .setAutoWidth(true);
                if (currentSelection.getNorms() != null) {
                    ng.setItems(currentSelection.getNorms());
                }
                ng.setSizeFull();
                ng.setAllRowsVisible(true);
                relationContent.add(ng);
                if (currentSelection.getNorms() == null || currentSelection.getNorms().isEmpty()) {
                    relationContent.add(new com.vaadin.flow.component.html.Span("No norms available for this PBM"));
                }
                break;
            case "Warehouse":
                Grid<com.ineos.oxide.pbmgids.model.entities.WarehouseItem> wg = new Grid<>(
                        com.ineos.oxide.pbmgids.model.entities.WarehouseItem.class, false);
                wg.addColumn(com.ineos.oxide.pbmgids.model.entities.WarehouseItem::getWarehouseNumber)
                        .setHeader("Number").setAutoWidth(true);
                wg.addColumn(com.ineos.oxide.pbmgids.model.entities.WarehouseItem::getVariantText).setHeader("Variant")
                        .setAutoWidth(true);
                if (currentSelection.getWarehouseItems() != null) {
                    wg.setItems(currentSelection.getWarehouseItems());
                }
                wg.setSizeFull();
                wg.setAllRowsVisible(true);
                relationContent.add(wg);
                if (currentSelection.getWarehouseItems() == null || currentSelection.getWarehouseItems().isEmpty()) {
                    relationContent
                            .add(new com.vaadin.flow.component.html.Span("No warehouse items available for this PBM"));
                }
                break;
            case "Documents":
            default:
                Grid<com.ineos.oxide.pbmgids.model.entities.Document> dg = new Grid<>(
                        com.ineos.oxide.pbmgids.model.entities.Document.class, false);
                dg.addColumn(com.ineos.oxide.pbmgids.model.entities.Document::getDocumentType).setHeader("Type");
                dg.addColumn(com.ineos.oxide.pbmgids.model.entities.Document::getDescription).setHeader("Description");
                dg.addColumn(com.ineos.oxide.pbmgids.model.entities.Document::getFilePath).setHeader("File");
                if (currentSelection.getDocuments() != null) {
                    dg.setItems(currentSelection.getDocuments());
                }
                dg.setSizeFull();
                dg.setAllRowsVisible(true);
                relationContent.add(dg);
                if (currentSelection.getDocuments() == null || currentSelection.getDocuments().isEmpty()) {
                    relationContent.add(new com.vaadin.flow.component.html.Span("No documents available for this PBM"));
                }
                break;
        }
    }

    @Override
    protected String getRoute() {
        return "/catalog";
    }
}
