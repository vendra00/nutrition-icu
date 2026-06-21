package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.ui.alerts.AlertsView;
import t1tanic.nutritionicu.ui.analytics.AnalyticsView;
import t1tanic.nutritionicu.ui.dashboard.DashboardView;
import t1tanic.nutritionicu.ui.doctors.DoctorsView;
import t1tanic.nutritionicu.ui.energy.EnergyView;
import t1tanic.nutritionicu.ui.formula.NutritionFormulaView;
import t1tanic.nutritionicu.ui.insights.InsightsView;
import t1tanic.nutritionicu.ui.nutrition.NutritionView;
import t1tanic.nutritionicu.ui.patients.PatientsView;

/**
 * App shell: a header with a drawer toggle, the signed-in user and a logout button, and a left-side
 * navigation listing the main sections. The admin-only "Nutrition formula" entry is hidden for doctors.
 * Every view declares {@code layout = MainLayout.class} to live inside it.
 *
 * <p>{@code @PermitAll}: the layout must be at least as accessible as the views it hosts; per-view
 * restrictions (e.g. admin-only formula tab) are declared on the views themselves.
 */
@PermitAll
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        setPrimarySection(Section.DRAWER);
        addToNavbar(header());
        addToDrawer(navigation());
    }

    private HorizontalLayout header() {
        H1 title = new H1("ICU Nutrition");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span user = new Span(authContext.getPrincipalName().orElse(""));
        user.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        Button logout = new Button("Log out", VaadinIcon.SIGN_OUT.create(), e -> authContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout account = new HorizontalLayout(user, logout);
        account.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, account);
        header.setFlexGrow(1, title);
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
        nav.addItem(new SideNavItem("Nutrition", NutritionView.class, VaadinIcon.CUTLERY.create()));
        if (authContext.hasRole("ADMIN")) {
            nav.addItem(new SideNavItem("Nutrition formula", NutritionFormulaView.class, VaadinIcon.LIST.create()));
        }
        nav.addItem(new SideNavItem("Energy", EnergyView.class, VaadinIcon.FIRE.create()));
        nav.addItem(new SideNavItem("Doctors", DoctorsView.class, VaadinIcon.DOCTOR.create()));
        nav.addItem(new SideNavItem("Analytics", AnalyticsView.class, VaadinIcon.CHART.create()));
        nav.addItem(new SideNavItem("Insights", InsightsView.class, VaadinIcon.LIGHTBULB.create()));
        return nav;
    }
}
