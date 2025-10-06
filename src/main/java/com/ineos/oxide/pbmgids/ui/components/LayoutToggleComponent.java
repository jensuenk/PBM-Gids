package com.ineos.oxide.pbmgids.ui.components;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Toggle button component for switching between table and tile layouts.
 * Handles icon updates and tooltip text based on current state.
 */
public class LayoutToggleComponent extends Button {

    public enum LayoutType {
        TABLE, TILE
    }

    private LayoutType currentLayout = LayoutType.TABLE;
    private Consumer<LayoutType> onToggle;

    public LayoutToggleComponent() {
        setupButton();
        updateAppearance();
    }

    private void setupButton() {
        addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        addClickListener(e -> toggle());
    }

    private void toggle() {
        currentLayout = (currentLayout == LayoutType.TABLE) ? LayoutType.TILE : LayoutType.TABLE;
        updateAppearance();

        if (onToggle != null) {
            onToggle.accept(currentLayout);
        }
    }

    private void updateAppearance() {
        if (currentLayout == LayoutType.TABLE) {
            setIcon(VaadinIcon.GRID_SMALL.create());
            setTooltipText("Switch to tile view");
        } else {
            setIcon(VaadinIcon.TABLE.create());
            setTooltipText("Switch to table view");
        }
    }

    // Public API

    public LayoutType getCurrentLayout() {
        return currentLayout;
    }

    public void setCurrentLayout(LayoutType layout) {
        this.currentLayout = layout;
        updateAppearance();
    }

    public boolean isTableView() {
        return currentLayout == LayoutType.TABLE;
    }

    public boolean isTileView() {
        return currentLayout == LayoutType.TILE;
    }

    public void setOnToggle(Consumer<LayoutType> callback) {
        this.onToggle = callback;
    }
}