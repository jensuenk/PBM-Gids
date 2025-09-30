package com.ineos.oxide.pbmgids.ui.components;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Component that displays detailed information about a PBM including image, name, description, 
 * and protection information.
 */
public class PbmDetailsComponent extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    public PbmDetailsComponent() {
        setSpacing(false);
        setPadding(false);
    }

    /**
     * Updates the component to show details for the specified PBM
     * @param pbm The PBM to display details for
     */
    public void showPbm(Pbm pbm) {
        removeAll();
        
        if (pbm == null) {
            return;
        }

        // Add image if available
        if (pbm.getImage() != null && !pbm.getImage().isBlank()) {
            Image img = new Image(pbm.getImage(), pbm.getName());
            img.setWidth("160px");
            add(img);
        }

        // Add name
        add(new H3(pbm.getName()));

        // Add description if available
        if (pbm.getDescription() != null && !pbm.getDescription().isBlank()) {
            add(new Paragraph(pbm.getDescription()));
        }

        // Add protection information
        if (pbm.getProtectsAgainst() != null && !pbm.getProtectsAgainst().isBlank()) {
            add(new Paragraph("Protects: " + pbm.getProtectsAgainst()));
        }

        if (pbm.getDoesNotProtectAgainst() != null && !pbm.getDoesNotProtectAgainst().isBlank()) {
            add(new Paragraph("Does not protect: " + pbm.getDoesNotProtectAgainst()));
        }
    }

    /**
     * Clears the component
     */
    public void clear() {
        removeAll();
    }
}