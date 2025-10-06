package com.ineos.oxide.pbmgids.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Manages PBM comparison selection logic.
 * Handles selection limits, notifications, and state updates.
 */
public class ComparisonManager {

    private static final int MAX_COMPARISON_ITEMS = 3;

    private final List<Pbm> selectedPbms = new ArrayList<>();
    private Consumer<List<Pbm>> onSelectionChanged;

    public ComparisonManager() {
    }

    public ComparisonManager(Consumer<List<Pbm>> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    /**
     * Attempts to add or remove a PBM from comparison selection.
     * 
     * @param pbm      The PBM to add/remove
     * @param selected Whether to add (true) or remove (false)
     * @return true if the operation was successful, false if it was rejected (e.g.,
     *         limit reached)
     */
    public boolean setSelected(Pbm pbm, boolean selected) {
        if (selected) {
            return addToSelection(pbm);
        } else {
            return removeFromSelection(pbm);
        }
    }

    private boolean addToSelection(Pbm pbm) {
        if (selectedPbms.contains(pbm)) {
            return true; // Already selected
        }

        if (selectedPbms.size() >= MAX_COMPARISON_ITEMS) {
            Notification.show("You can only compare up to " + MAX_COMPARISON_ITEMS + " PBMs")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        selectedPbms.add(pbm);
        notifySelectionChanged();
        return true;
    }

    private boolean removeFromSelection(Pbm pbm) {
        boolean removed = selectedPbms.remove(pbm);
        if (removed) {
            notifySelectionChanged();
        }
        return true;
    }

    public boolean isSelected(Pbm pbm) {
        return selectedPbms.contains(pbm);
    }

    public List<Pbm> getSelectedPbms() {
        return new ArrayList<>(selectedPbms);
    }

    public int getSelectionCount() {
        return selectedPbms.size();
    }

    public boolean canCompare() {
        return selectedPbms.size() >= 2;
    }

    public void clearSelection() {
        selectedPbms.clear();
        notifySelectionChanged();
    }

    public void setOnSelectionChanged(Consumer<List<Pbm>> callback) {
        this.onSelectionChanged = callback;
    }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(getSelectedPbms());
        }
    }

    public static int getMaxComparisonItems() {
        return MAX_COMPARISON_ITEMS;
    }
}