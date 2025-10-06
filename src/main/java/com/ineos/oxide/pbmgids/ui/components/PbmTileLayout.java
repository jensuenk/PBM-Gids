package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;
import java.util.function.Consumer;

import com.ineos.oxide.pbmgids.managers.ComparisonManager;
import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

/**
 * Layout container for displaying PBM tiles.
 * Manages tile creation and layout properties.
 */
public class PbmTileLayout extends FlexLayout {

    private ComparisonManager comparisonManager;
    private Consumer<Pbm> onDetailsClick;

    public PbmTileLayout() {
        setupLayout();
    }

    private void setupLayout() {
        setWidthFull();
        setFlexWrap(FlexWrap.WRAP);
        getStyle()
                .set("gap", "20px")
                .set("padding", "16px 0 0 20px")
                .set("justify-content", "flex-start")
                .set("box-sizing", "border-box");
    }

    public void updateTiles(List<Pbm> pbms) {
        removeAll();

        for (Pbm pbm : pbms) {
            PbmTileComponent tile = createTile(pbm);
            add(tile);
        }
    }

    private PbmTileComponent createTile(Pbm pbm) {
        PbmTileComponent tile = new PbmTileComponent(pbm);

        // Set initial selection state
        if (comparisonManager != null) {
            tile.setSelected(comparisonManager.isSelected(pbm));
        }

        // Setup callbacks
        tile.setOnSelectionChange(selected -> {
            if (comparisonManager != null) {
                boolean success = comparisonManager.setSelected(pbm, selected);
                if (!success) {
                    // Revert tile selection if it was rejected
                    tile.setSelected(false);
                }
            }
        });

        tile.setOnDetailsClick(() -> {
            if (onDetailsClick != null) {
                onDetailsClick.accept(pbm);
            }
        });

        return tile;
    }

    // Public API

    public void setComparisonManager(ComparisonManager manager) {
        this.comparisonManager = manager;
    }

    public void setOnDetailsClick(Consumer<Pbm> callback) {
        this.onDetailsClick = callback;
    }
}