package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.security.services.ServiceUsers;
import com.ineos.oxide.base.security.ui.CurrentUserView;
import com.ineos.oxide.base.security.ui.LoginView;
import com.ineos.oxide.base.security.ui.RoleView;
import com.ineos.oxide.base.security.ui.UserView;
import com.ineos.oxide.base.services.HasLogger;
import com.ineos.oxide.base.services.HasResources;
import com.ineos.oxide.base.ui.IosMainLayout;
import com.ineos.oxide.base.ui.MenuItemInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.UIScope;

@UIScope
@PropertySource("classpath:application.yaml")
public class MainView extends IosMainLayout implements HasLogger, HasResources {
	private static final long serialVersionUID = 8194971240691330917L;
	private static final String MENU_ICON_LIST_UL = "la la-list-ul";
	private ServiceUsers serviceUsers;
	private static final String MENU_TITLE = "menu.title";

	private transient ServiceIndexType serviceIndexType;

	public MainView(ServiceUsers serviceUsers, ServiceIndexType serviceIndexType) {
		super();
		setServiceUser(serviceUsers);
		setServiceIndexType(serviceIndexType);
		initUi();
	}

	private void initUi() {
		if (SecurityUtils.isUserLoggedIn()) {
			addRightUpperComponent(getButtonCurrentUser());
		} else {
			addRightUpperComponent(getLoginButton());
		}
		addLeftBottomComponent(getDatabaseName());
	}

	private Component getDatabaseName() {
		return new Span(getServiceUsers().getDatabaseServerName());
	}

	private Button getLoginButton() {
		Button button = new Button(getProperty("login"), new Icon(VaadinIcon.USER),
				event -> getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
		button.setId("login-button");
		return button;
	}

	private Button getButtonCurrentUser() {
		return new Button(SecurityUtils.getCurrentUser().get().getDisplayName(), new Icon(VaadinIcon.USER),
				event -> getUI().ifPresent(ui -> ui.navigate(CurrentUserView.class)));

	}

	protected MenuItemInfo[] createMenuItems() {
		List<MenuItemInfo> menuItems = new ArrayList<>();
		// Public catalog entry
		menuItems.add(new MenuItemInfo("Catalog", MENU_ICON_LIST_UL, CatalogView.class));

		if (SecurityUtils.isUserLoggedIn() && SecurityUtils.isUserInRole("ROLE_ADMIN")) {
			menuItems.add(new MenuItemInfo(
					getProperty(UserView.class, MENU_TITLE),
					MENU_ICON_LIST_UL, UserView.class));
			menuItems.add(new MenuItemInfo(
					getProperty(RoleView.class, MENU_TITLE),
					MENU_ICON_LIST_UL, RoleView.class));

		}

		return menuItems.toArray(new MenuItemInfo[menuItems.size()]);
	}

	private void setServiceUser(ServiceUsers serviceUsers) {
		this.serviceUsers = serviceUsers;
	}

	private ServiceUsers getServiceUsers() {
		return serviceUsers;
	}
}
