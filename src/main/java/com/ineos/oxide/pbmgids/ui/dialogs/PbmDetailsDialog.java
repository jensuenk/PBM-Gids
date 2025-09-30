package com.ineos.oxide.pbmgids.ui.dialogs;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.ineos.oxide.pbmgids.ui.components.PbmDetailsComponent;
import com.ineos.oxide.pbmgids.ui.components.PbmRelationsComponent;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Modal dialog that displays detailed PBM information including relations.
 */
public class PbmDetailsDialog extends Dialog {
    private static final long serialVersionUID = 1L;
    
    private final PbmDetailsComponent detailsComponent;
    private final PbmRelationsComponent relationsComponent;

    public PbmDetailsDialog() {
        setupDialog();
        
        // Initialize components
        detailsComponent = new PbmDetailsComponent();
        relationsComponent = new PbmRelationsComponent();
        
        // Create layout
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setSpacing(true);
        dialogContent.setPadding(true);
        dialogContent.add(detailsComponent, relationsComponent);
        
        // Set flex grow to allow relations to expand
        dialogContent.setFlexGrow(0, detailsComponent);
        dialogContent.setFlexGrow(1, relationsComponent);
        
        add(dialogContent);
    }

    private void setupDialog() {
        setWidth("80%");
        setHeight("80%");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setDraggable(true);
        setResizable(true);
    }

    /**
     * Opens the dialog displaying details for the specified PBM
     * @param pbm The PBM to display details for
     */
    public void showPbm(Pbm pbm) {
        if (pbm == null) {
            return;
        }
        
        detailsComponent.showPbm(pbm);
        relationsComponent.showPbmRelations(pbm);
        
        // Set dynamic header title
        setHeaderTitle("PBM Details: " + pbm.getName());
        
        open();
    }

    /**
     * Clears the dialog content and closes it
     */
    public void clearAndClose() {
        detailsComponent.clear();
        relationsComponent.clear();
        close();
    }
}