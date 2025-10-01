package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;
import java.util.function.Consumer;

import com.ineos.oxide.pbmgids.model.entities.Pbm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.renderer.ComponentRenderer;

/**
 * Reusable PBM Grid component that displays PBM items with image, name, type,
 * brand, and details button.
 */
public class PbmGrid extends Grid<Pbm> {
    private static final long serialVersionUID = 1L;

    private Consumer<Pbm> detailsClickHandler;

    public PbmGrid() {
        super(Pbm.class, false);
        initializeColumns();
        setupEventHandlers();
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void initializeColumns() {
        // Image column
        addColumn(new ComponentRenderer<>(this::createImageComponent))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setWidth("80px");

        // Name column
        addColumn(Pbm::getName)
                .setHeader("Name")
                .setAutoWidth(true);

        // Type column
        addColumn(Pbm::getTypeName)
                .setHeader("Type")
                .setAutoWidth(true);

        // Brand column
        addColumn(Pbm::getBrand)
                .setHeader("Brand")
                .setAutoWidth(true);

        // Details button column
        addColumn(new ComponentRenderer<>(this::createDetailsButton))
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private void setupEventHandlers() {
        asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null && detailsClickHandler != null) {
                detailsClickHandler.accept(e.getValue());
            }
        });
    }

    private Div createImageComponent(Pbm pbm) {
        if (pbm.getImage() == null || pbm.getImage().isBlank()) {
            return new Div();
        }

        Image img = new Image(pbm.getImage(), pbm.getName());
        img.setAlt(pbm.getName());
        img.setWidth("64px");

        Div container = new Div(img);
        return container;
    }

    private Button createDetailsButton(Pbm pbm) {
        Button btn = new Button("Details");
        btn.addClickListener(ev -> {
            select(pbm);
            if (detailsClickHandler != null) {
                detailsClickHandler.accept(pbm);
            }
        });
        return btn;
    }

    /**
     * Sets the handler for when details are requested for a PBM
     * 
     * @param handler Consumer that handles the PBM details request
     */
    public void setDetailsClickHandler(Consumer<Pbm> handler) {
        this.detailsClickHandler = handler;
    }

    /**
     * Loads PBMs into the grid
     * 
     * @param pbms List of PBMs to display
     */
    public void loadPbms(List<Pbm> pbms) {
        setItems(pbms);
        deselectAll();
    }

    /**
     * Clears the grid
     */
    public void clearGrid() {
        setItems();
        deselectAll();
    }
}