package com.ineos.oxide.pbmgids.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.common.ui.IosMainLayout;
import com.ineos.oxide.base.common.ui.MenuItemInfo;
import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.security.services.ServiceUsers;
import com.ineos.oxide.base.security.ui.CurrentUserView;
import com.ineos.oxide.base.security.ui.LoginView;
import com.ineos.oxide.base.security.ui.RoleView;
import com.ineos.oxide.base.security.ui.UserView;
import com.ineos.oxide.base.services.HasLogger;
import com.ineos.oxide.base.services.HasResources;
import com.ineos.oxide.pbmgids.model.entities.Category;
import com.ineos.oxide.pbmgids.services.CatalogService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.UIScope;

import jakarta.annotation.PostConstruct;

@UIScope
@PropertySource("classpath:application.yaml")
public class MainView extends IosMainLayout implements HasLogger, HasResources {
	private static final long serialVersionUID = 8194971240691330917L;
	private static final String MENU_ICON_LIST_UL = "la la-list-ul";
	private static final String MENU_TITLE = "menu.title";

	private ServiceUsers serviceUsers;
	private transient CatalogService catalogService;

	public MainView(ServiceUsers serviceUsers, CatalogService catalogService) {
		this.serviceUsers = serviceUsers;
		this.catalogService = catalogService;
	}

	@PostConstruct
	private void initUi() {
		if (SecurityUtils.isUserLoggedIn()) {
			addRightUpperComponent(getButtonCurrentUser());
		} else {
			addRightUpperComponent(getLoginButton());
		}
		addLeftBottomComponent(getDatabaseName());
	}

	private Component getDatabaseName() {
		return new Span(serviceUsers.getDatabaseServerName());
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

	@Override
	protected MenuItemInfo[] createMenuItems() {
		List<MenuItemInfo> menuItems = new ArrayList<>();
		menuItems.addAll(generateMenuCategories());

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

	private List<MenuItemInfo> generateMenuCategories() {
		List<MenuItemInfo> menuItems = new ArrayList<>();
		catalogService.getRootCategories().stream().forEach(cat -> {
			menuItems.add(createRecursiveMenuItem(cat));
		});
		return menuItems;
	}

	private MenuItemInfo createRecursiveMenuItem(Category category) {
		List<Category> children = catalogService.getChildren(category.getId());

		if (children == null || children.isEmpty()) {
			// Leaf category - create direct navigation link
			MenuItemInfo item = new MenuItemInfo(category.getName(), MENU_ICON_LIST_UL,
					CatalogView.class, new RouteParameters("categoryId", category.getId().toString()));
			// close all open accordions to ensure proper highlighting of selected item

			return item;
		} else {
			// Parent category with children - create submenu with recursive items
			List<MenuItemInfo> subMenuItems = new ArrayList<>();

			// Recursively add children only (don't add the parent as a child of itself)
			children.stream().forEach(child -> {
				subMenuItems.add(createRecursiveMenuItem(child));
			});

			return new MenuItemInfo(category.getName(), MENU_ICON_LIST_UL,
					subMenuItems.toArray(new MenuItemInfo[subMenuItems.size()]));
		}
	}
}
