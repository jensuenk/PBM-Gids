package com.ineos.oxide.base.common.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * A simple navigation item component, based on ListItem element.
 */
public class MenuItemInfo extends ListItem {
    private static final long serialVersionUID = 1735988510989959893L;
    private final Class<? extends Component> view;

    public MenuItemInfo(String menuTitle, String iconClass, Class<? extends Component> view) {
        this.view = view;
        RouterLink link = new RouterLink();
        link.setHighlightCondition(HighlightConditions.sameLocation());
        link.addClassNames("menu-item-link");
        link.setRoute(view);

        Span text = new Span(menuTitle);
        text.addClassNames("menu-item-text");

        link.add(new LineAwesomeIcon(iconClass), text);
        add(link);
    }

    public MenuItemInfo(String menuTitle, String iconClass, Class<? extends Component> view,
            RouteParameters routeParameters) {
        this.view = view;
        RouterLink link = new RouterLink(view, routeParameters);
        link.setHighlightCondition(HighlightConditions.sameLocation());
        link.addClassNames("menu-item-link");
        link.setRoute(view, routeParameters);

        Span text = new Span(menuTitle);
        text.addClassNames("menu-item-text");

        link.add(new LineAwesomeIcon(iconClass), text);
        add(link);
    }

    public MenuItemInfo(String menuTitle, String iconClass, MenuItemInfo... subMenuItems) {
        this.view = null;
        Accordion accordion = new Accordion();
        accordion.addClassNames("menu-item-link");
        UnorderedList list = new UnorderedList();
        list.addClassNames("navigation-list");
        list.add(subMenuItems);
        accordion.add(menuTitle, list);
        add(accordion);
        accordion.close();
        accordion.getChildren()
                .filter(component -> component instanceof com.vaadin.flow.component.accordion.AccordionPanel)
                .forEach(panel -> panel.getElement().getStyle().set("width", "100%"));
    }

    public Class<?> getView() {
        return view;
    }

    /**
     * Simple wrapper to create icons using LineAwesome iconset. See
     * https://icons8.com/line-awesome
     */
    @NpmPackage(value = "line-awesome", version = "1.3.0")
    public static class LineAwesomeIcon extends Span {
        private static final long serialVersionUID = -7243525507102404683L;

        public LineAwesomeIcon(String lineawesomeClassnames) {
            addClassNames("menu-item-icon");
            if (!lineawesomeClassnames.isEmpty()) {
                addClassNames(lineawesomeClassnames);
            }
        }
    }

}
