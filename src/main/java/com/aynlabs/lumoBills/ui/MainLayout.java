package com.aynlabs.lumoBills.ui;

import com.aynlabs.lumoBills.backend.entity.Role;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.security.SecurityService;
import com.aynlabs.lumoBills.ui.views.billing.BillingView;
import com.aynlabs.lumoBills.ui.views.customer.CustomerView;
import com.aynlabs.lumoBills.ui.views.dashboard.DashboardView;
import com.aynlabs.lumoBills.ui.views.purchase.PurchaseView;
import com.aynlabs.lumoBills.ui.views.stock.StockView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MainLayout extends AppLayout {

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;

        setPrimarySection(Section.NAVBAR);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");
        toggle.addClassName("text-white");

        // Logo / App Name in Header
        H1 logo = new H1("LumoBills");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, "text-white", "header-logo");

        HorizontalLayout leftSide = new HorizontalLayout(toggle, logo);
        leftSide.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        leftSide.setSpacing(true);

        User user = securityService.getAuthenticatedUser();

        HorizontalLayout layout = new HorizontalLayout(leftSide);
        layout.addClassName("header-layout");
        layout.setWidthFull();
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        layout.setPadding(true);

        // User Menu
        if (user != null) {
            Avatar avatar = new Avatar(user.getName());
            avatar.addClassName("cursor-pointer");

            MenuBar userMenu = new MenuBar();
            userMenu.addClassName("user-menu-bar");
            userMenu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY_INLINE);

            MenuItem menuItem = userMenu.addItem(avatar);
            SubMenu subMenu = menuItem.getSubMenu();
            subMenu.addItem("Sign out", e -> securityService.logout());

            Button themeToggle = new Button(VaadinIcon.ADJUST.create(), click -> {
                com.vaadin.flow.component.UI.getCurrent().getElement().executeJs(
                        "const isDark = document.documentElement.hasAttribute('theme');" +
                                "if (isDark) { " +
                                "document.documentElement.removeAttribute('theme'); " +
                                "document.body.removeAttribute('theme'); " +
                                "} else { " +
                                "document.documentElement.setAttribute('theme', 'dark'); " +
                                "document.body.setAttribute('theme', 'dark'); " +
                                "}");
            });
            themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            themeToggle.addClassName("text-white");

            HorizontalLayout rightSide = new HorizontalLayout(themeToggle, userMenu);
            rightSide.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            rightSide.getStyle().set("margin-left", "auto");
            layout.add(rightSide);
        }

        addToNavbar(true, layout);
    }

    private void addDrawerContent() {
        H1 appName = new H1("LumoBills");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();
        User user = securityService.getAuthenticatedUser();

        if (user != null) {
            boolean isAdmin = user.getRoles().contains(Role.ADMIN);
            java.util.Set<String> accessibleViews = user.getAccessibleViews();
            if (accessibleViews == null) {
                accessibleViews = java.util.Collections.emptySet();
            }

            if (isAdmin || accessibleViews.contains("Dashboard")) {
                nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
            }
            if (isAdmin || accessibleViews.contains("Stock")) {
                nav.addItem(new SideNavItem("Stock", StockView.class, VaadinIcon.PACKAGE.create()));
            }
            if (isAdmin || accessibleViews.contains("Categories")) {
                nav.addItem(new SideNavItem("Categories", com.aynlabs.lumoBills.ui.views.stock.CategoryView.class,
                        VaadinIcon.TAGS.create()));
            }
            if (isAdmin || accessibleViews.contains("Billing")) {
                nav.addItem(new SideNavItem("Billing", BillingView.class, VaadinIcon.INVOICE.create()));
            }
            if (isAdmin || accessibleViews.contains("Purchase")) {
                nav.addItem(new SideNavItem("Purchase", PurchaseView.class, VaadinIcon.CART.create()));
            }
            if (isAdmin || accessibleViews.contains("Customers")) {
                nav.addItem(new SideNavItem("Customers", CustomerView.class, VaadinIcon.USERS.create()));
            }
            if (isAdmin || accessibleViews.contains("Invoices")) {
                nav.addItem(new SideNavItem("Invoices", com.aynlabs.lumoBills.ui.views.billing.InvoiceListView.class,
                        VaadinIcon.LIST.create()));
            }
            if (isAdmin || accessibleViews.contains("Reports")) {
                nav.addItem(new SideNavItem("Reports", com.aynlabs.lumoBills.ui.views.reports.ReportsView.class,
                        VaadinIcon.CHART_3D.create()));
            }

            if (isAdmin) {
                nav.addItem(new SideNavItem("Admin Settings", com.aynlabs.lumoBills.ui.views.admin.AdminView.class,
                        VaadinIcon.COG.create()));
            }
        }

        return nav;
    }

    private com.vaadin.flow.component.html.Footer createFooter() {
        return new com.vaadin.flow.component.html.Footer();
    }
}
