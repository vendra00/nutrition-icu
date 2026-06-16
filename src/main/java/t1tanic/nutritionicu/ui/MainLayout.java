package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * App shell: a header with a drawer toggle and a left-side navigation listing the
 * main sections. Every view declares {@code layout = MainLayout.class} to live inside it.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(header());
        addToDrawer(navigation());
    }

    private HorizontalLayout header() {
        H1 title = new H1("ICU Nutrition");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        header.setWidthFull();
        return header;
    }

    private SideNav navigation() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Alerts", AlertsView.class, VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem("Patients", PatientsView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Doctors", DoctorsView.class, VaadinIcon.DOCTOR.create()));
        nav.addItem(new SideNavItem("Analytics", AnalyticsView.class, VaadinIcon.CHART.create()));
        nav.addItem(new SideNavItem("Insights", InsightsView.class, VaadinIcon.LIGHTBULB.create()));
        return nav;
    }
}
