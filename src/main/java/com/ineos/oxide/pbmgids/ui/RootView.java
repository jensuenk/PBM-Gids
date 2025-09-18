package com.ineos.oxide.pbmgids.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.ui.IneosAppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;

import jakarta.annotation.PostConstruct;

@Route(value = "/", layout = MainView.class)
@UIScope
@AnonymousAllowed
@PropertySource("classpath:application.yaml")
public class RootView extends IneosAppLayout implements HasDynamicTitle {
	private static final long serialVersionUID = 1L;

	@Value("${spring.application.name}")
	String appName;

	@Value("${under_construction_notif}")
	boolean underConstructionNotif;
	@Value("${under_construction_notif.msg}")
	String underConstructionNotifMsg;

	public RootView() {
		super();
	}

	@PostConstruct
	private void init() {
		if (underConstructionNotif) {
			String root_message = underConstructionNotifMsg;
			this.add(new H2(root_message));
			Image img = new Image("images/underconstruction.png", "Under Construction");
			img.setWidth("50%");
			this.add(img);
		} else {
			if (!SecurityUtils.isUserLoggedIn()) {
				this.add(new H2(getProperty("welcome-message")));
				this.add(new Anchor("/catalog", "Open PBM Catalog"));
			}
		}
	}

	@Override
	protected String getRoute() {
		return "/";
	}

	@Override
	public String getPageTitle() {
		return appName;
	}
}
