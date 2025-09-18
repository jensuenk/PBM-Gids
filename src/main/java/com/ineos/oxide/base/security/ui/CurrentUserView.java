package com.ineos.oxide.base.security.ui;

import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.security.ui.panels.UserPanel;
import com.ineos.oxide.base.ui.IneosAppLayout;
import com.ineos.oxide.base.ui.services.ServiceCookies;
import com.ineos.oxide.pbmgids.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.annotation.UIScope;

import jakarta.annotation.security.PermitAll;

@Route(value = CurrentUserView.ROUTE, layout = MainView.class)
@UIScope
@PermitAll
@PropertySource("classpath:application.yaml")
public class CurrentUserView extends IneosAppLayout implements HasDynamicTitle, BeforeEnterObserver {
    protected static final String ROUTE = "current_user";
    private String title = "<Not fetched>";
    private transient ServiceCookies serviceCookies;

    public CurrentUserView(ServiceCookies serviceCookies) {
        super();
        this.serviceCookies = serviceCookies;
        initUi();
    }

    @Override
    protected void initUi() {
        super.initUi();
        this.setSizeFull();
        Button buttonLogoff = new Button("Logoff", event -> {
            // Invalidate the underlying HTTP session instead of VaadinSession
            VaadinServletRequest.getCurrent().getHttpServletRequest().getSession().invalidate();

            // // Redirect to the login page (or any other page)
            // UI.getCurrent().navigate("/");
        });
        this.add(buttonLogoff,
                new UserPanel(UserPanel.UserPanelMode.VIEW, SecurityUtils.getCurrentUser().get(), serviceCookies));
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    @Override
    protected String getRoute() {
        return ROUTE;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        SecurityUtils.getCurrentUser().ifPresentOrElse(user -> {
            title = user.getDisplayName();
        }, () -> {
            title = "<Not fetched>";
        });
    }

}
