package com.ineos.oxide.pbmgids.ui.components;

import java.util.logging.Logger;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

/**
 * Component that displays PBM relations in tabbed format (Documents, Norms,
 * Warehouse).
 */
public class PbmRelationsComponent extends Div {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(PbmRelationsComponent.class.getName());

    private final Tabs relationTabs;
    private final Div relationContent;
    private Pbm currentPbm;

    // Tab references
    private final Tab documentsTab;
    private final Tab normsTab;
    private final Tab warehouseTab;

    public PbmRelationsComponent() {
        setSizeFull();

        // Initialize tabs
        documentsTab = new Tab("Documents");
        normsTab = new Tab("Norms");
        warehouseTab = new Tab("Warehouse");

        relationTabs = new Tabs(documentsTab, normsTab, warehouseTab);
        relationTabs.setSelectedTab(documentsTab);
        relationTabs.addSelectedChangeListener(e -> refreshContent());

        // Content area
        relationContent = new Div();
        relationContent.setSizeFull();
        relationContent.getStyle().set("min-height", "300px");

        add(relationTabs, relationContent);
    }

    /**
     * Shows relations for the specified PBM
     * 
     * @param pbm The PBM to show relations for
     */
    public void showPbmRelations(Pbm pbm) {
        this.currentPbm = pbm;
        refreshContent();
    }

    /**
     * Clears the relations display
     */
    public void clear() {
        this.currentPbm = null;
        relationContent.removeAll();
        relationContent.add(new Span("Select a PBM to view related information"));
    }

    private void refreshContent() {
        relationContent.removeAll();

        if (currentPbm == null) {
            relationContent.add(new Span("No PBM selected"));
            return;
        }

        Tab selectedTab = relationTabs.getSelectedTab();
        String tabLabel = selectedTab != null ? selectedTab.getLabel() : "Documents";
        logger.info("Refreshing relations tab: " + tabLabel);

        switch (tabLabel) {
            case "Norms":
                showNorms();
                break;
            case "Warehouse":
                showWarehouseItems();
                break;
            case "Documents":
            default:
                showDocuments();
                break;
        }
    }

    private void showDocuments() {
        Grid<com.ineos.oxide.pbmgids.model.entities.Document> documentsGrid = new Grid<>(
                com.ineos.oxide.pbmgids.model.entities.Document.class, false);

        // Only show file path as requested
        documentsGrid.addColumn(com.ineos.oxide.pbmgids.model.entities.Document::getFilePath)
                .setHeader("File");

        if (currentPbm.getDocuments() != null && !currentPbm.getDocuments().isEmpty()) {
            documentsGrid.setItems(currentPbm.getDocuments());
        } else {
            relationContent.add(new Span("No documents available for this PBM"));
            return;
        }

        documentsGrid.setSizeFull();
        documentsGrid.setAllRowsVisible(true);
        relationContent.add(documentsGrid);
    }

    private void showNorms() {
        Grid<com.ineos.oxide.pbmgids.model.entities.Norm> normsGrid = new Grid<>(
                com.ineos.oxide.pbmgids.model.entities.Norm.class, false);

        normsGrid.addColumn(com.ineos.oxide.pbmgids.model.entities.Norm::getName)
                .setHeader("Name")
                .setAutoWidth(true);
        normsGrid.addColumn(com.ineos.oxide.pbmgids.model.entities.Norm::getDescription)
                .setHeader("Description")
                .setAutoWidth(true);

        if (currentPbm.getNorms() != null && !currentPbm.getNorms().isEmpty()) {
            normsGrid.setItems(currentPbm.getNorms());
        } else {
            relationContent.add(new Span("No norms available for this PBM"));
            return;
        }

        normsGrid.setSizeFull();
        normsGrid.setAllRowsVisible(true);
        relationContent.add(normsGrid);
    }

    private void showWarehouseItems() {
        Grid<com.ineos.oxide.pbmgids.model.entities.WarehouseItem> warehouseGrid = new Grid<>(
                com.ineos.oxide.pbmgids.model.entities.WarehouseItem.class, false);

        warehouseGrid.addColumn(com.ineos.oxide.pbmgids.model.entities.WarehouseItem::getWarehouseNumber)
                .setHeader("Number")
                .setAutoWidth(true);
        warehouseGrid.addColumn(com.ineos.oxide.pbmgids.model.entities.WarehouseItem::getVariantText)
                .setHeader("Variant")
                .setAutoWidth(true);

        if (currentPbm.getWarehouseItems() != null && !currentPbm.getWarehouseItems().isEmpty()) {
            warehouseGrid.setItems(currentPbm.getWarehouseItems());
        } else {
            relationContent.add(new Span("No warehouse items available for this PBM"));
            return;
        }

        warehouseGrid.setSizeFull();
        warehouseGrid.setAllRowsVisible(true);
        relationContent.add(warehouseGrid);
    }
}