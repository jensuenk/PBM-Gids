package com.ineos.oxide.base.common.ui;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;

import jakarta.annotation.PostConstruct;

/**
 * Abstract base layout for iOS-styled main application views.
 * <p>
 * This layout provides a navigation drawer, a customizable header with a title,
 * and areas for additional components on the left-bottom and right-upper sides.
 * It manages the main structure of the UI, including navigation, header, and
 * footer,
 * and allows subclasses to define their own menu items and additional
 * components.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Drawer navigation with customizable menu items.</li>
 * <li>Header with a dynamic title and right-aligned panel for extra
 * components.</li>
 * <li>Footer area in the drawer, customizable by overriding
 * {@code addLeftBottomComponent()}.</li>
 * <li>Support for dynamic page titles via {@code HasDynamicTitle} or
 * {@code @PageTitle} annotation.</li>
 * <li>Utility method for showing error notifications.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <p>
 * Extend this class and implement {@link #createMenuItems()} to provide
 * navigation items.
 * Optionally override {@link #addLeftBottomComponent()} and
 * {@link #addRightUpperComponent()}
 * to customize the drawer footer and header right panel, respectively.
 * </p>
 *
 * @author INEOS
 * @since 1.0
 */
public abstract class IosMainLayout extends AppLayout {

	/**
	 * Serial version UID for serialization.
	 */
	private static final long serialVersionUID = -984551940076918120L;
	private Footer footer;

	private H1 viewTitle;
	private VerticalLayout layout;

	@PostConstruct
	private void init() {
		setPrimarySection(Section.DRAWER);
		addToNavbar(true, createHeaderContent());
		addToDrawer(createDrawerContent());
	}

	/**
	 * @return Component
	 */
	private Component createHeaderContent() {
		DrawerToggle toggle = new DrawerToggle();
		toggle.addClassNames("view-toggle");
		toggle.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		toggle.getElement().setAttribute("aria-label", "Menu toggle");

		viewTitle = new H1();
		viewTitle.setWidth("50%");
		viewTitle.addClassNames("view-title");

		addRightUpperComponent(new Div(new Text("Left Bottom Component")));
		addLeftBottomComponent(new Div(new Text("Right Upper Component")));

		Header header = new Header(toggle, viewTitle, getRightPanel());
		header.addClassNames("view-header");
		return header;
	}

	/**
	 * @return VerticalLayout
	 */
	private VerticalLayout getRightPanel() {
		if (layout == null) {
			// Create VerticalLayout that aligns out the content to the right.
			layout = new VerticalLayout();
			layout.addClassNames("right-panel");
			layout.setWidthFull(); // Make sure it takes full width
			layout.setDefaultHorizontalComponentAlignment(Alignment.END); // Align children to the right
		}

		return layout;
	}

	private Component createDrawerContent() {
		Image img = new Image("images/INEOS_Oxide_H_black.png", "placeholder INEOS_Oxide");
		img.setWidth("100px");
		Div companyName = new Div(img);
		companyName.addClassNames("app-name");
		com.vaadin.flow.component.html.Section section = new com.vaadin.flow.component.html.Section(companyName,
				createNavigation(), footer);
		section.addClassNames("drawer-section");
		addLeftBottomComponent(new Div(new Text("Left Bottom Component")));
		return section;
	}

	private Nav createNavigation() {
		Nav nav = new Nav();
		nav.addClassNames("menu-item-container");
		nav.getElement().setAttribute("aria-labelledby", "views");

		// Wrap the links in a list; improves accessibility
		UnorderedList list = new UnorderedList();
		list.addClassNames("navigation-list");
		nav.add(list);

		for (MenuItemInfo menuItem : createMenuItems()) {
			list.add(menuItem);

		}
		return nav;
	}

	protected abstract MenuItemInfo[] createMenuItems();

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		viewTitle.setText(getCurrentPageTitle());
	}

	private String getCurrentPageTitle() {
		if (this.getContent() instanceof HasDynamicTitle) {
			return ((HasDynamicTitle) this.getContent()).getPageTitle();
		} else {
			PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
			return title == null ? "" : title.value();
		}
	}

	public void updateTitle() {
		viewTitle.setText(getCurrentPageTitle());
	}

	protected void addLeftBottomComponent(Component componentToBeaddedToFooter) {
		getFooter().removeAll();
		getFooter().add(componentToBeaddedToFooter);
	};

	protected void addRightUpperComponent(Component componentToBeaddedToHeader) {
		getRightPanel().removeAll();
		getRightPanel().add(componentToBeaddedToHeader);
	}

	protected Optional<IosMainLayout> getParentLayout() {
		return getParent().filter(IosMainLayout.class::isInstance).map(IosMainLayout.class::cast);
	}

	protected void showErrorNotification(String message) {
		Notification notification = new Notification();
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

		Div text = new Div(new Text(message));

		Button closeButton = new Button(new Icon("lumo", "cross"));
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		closeButton.getElement().setAttribute("aria-label", "Close");
		closeButton.addClickListener(event -> {
			notification.close();
		});

		HorizontalLayout layout = new HorizontalLayout(text, closeButton);
		layout.setAlignItems(Alignment.CENTER);

		notification.add(layout);
		notification.open();
	}

	private Footer getFooter() {
		if (footer == null) {
			// Create Footer that aligns out the content to the left.
			footer = new Footer();
			footer.addClassNames("footer");
			footer.setWidthFull(); // Make sure it takes full width
		}
		return footer;
	}
}
