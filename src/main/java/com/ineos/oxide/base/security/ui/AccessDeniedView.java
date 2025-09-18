package com.ineos.oxide.base.security.ui;

import com.ineos.oxide.base.ui.IneosAppLayout;
import com.ineos.oxide.pbmgids.ui.MainView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = AccessDeniedView.ROUTE, layout = MainView.class)
@AnonymousAllowed
@PageTitle("Access Denied")
public class AccessDeniedView extends IneosAppLayout {
    protected static final String ROUTE = "access-denied";

    public AccessDeniedView() {
        super();
        buildUi();
    }

    private void buildUi() {
        this.setSizeFull();
        H1 title = new H1("Access Denied");
        title.getStyle().set("color", "red").set("font-weight", "bold");
        Paragraph message = new Paragraph("You do not have permission to access this page.");
        message.getStyle().set("color", "red").set("font-weight", "bold");
        this.add(new Div(title, message));
    }

    protected String getRoute() {
        return ROUTE;
    }
}