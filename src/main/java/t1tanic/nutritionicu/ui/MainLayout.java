package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Locale;
import t1tanic.nutritionicu.config.LocalePreference;
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
        H1 title = new H1(getTranslation("app.title"));
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span user = new Span(authContext.getPrincipalName().orElse(""));
        user.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        Button logout = new Button(getTranslation("header.logout"), VaadinIcon.SIGN_OUT.create(),
                e -> authContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout account = new HorizontalLayout(languageSelector(), user, logout);
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
        nav.addItem(new SideNavItem(getTranslation("nav.dashboard"), DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.alerts"), AlertsView.class, VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.patients"), PatientsView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.nutrition"), NutritionView.class, VaadinIcon.CUTLERY.create()));
        if (authContext.hasRole("ADMIN")) {
            nav.addItem(new SideNavItem(getTranslation("nav.formula"), NutritionFormulaView.class, VaadinIcon.LIST.create()));
        }
        nav.addItem(new SideNavItem(getTranslation("nav.energy"), EnergyView.class, VaadinIcon.FIRE.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.doctors"), DoctorsView.class, VaadinIcon.DOCTOR.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.analytics"), AnalyticsView.class, VaadinIcon.CHART.create()));
        nav.addItem(new SideNavItem(getTranslation("nav.insights"), InsightsView.class, VaadinIcon.LIGHTBULB.create()));
        return nav;
    }

    /** Language switcher: persists the choice for the session and reloads so every view re-renders. */
    private Select<Locale> languageSelector() {
        Select<Locale> language = new Select<>();
        language.setItems(LocalePreference.ENGLISH, LocalePreference.SPANISH);
        language.setItemLabelGenerator(locale ->
                "es".equals(locale.getLanguage()) ? getTranslation("language.es") : getTranslation("language.en"));
        language.setValue(LocalePreference.get(VaadinSession.getCurrent()));
        language.setWidth("130px");
        language.addValueChangeListener(e -> {
            if (e.getValue() == null) {
                return;
            }
            LocalePreference.set(VaadinSession.getCurrent(), e.getValue());
            UI.getCurrent().setLocale(e.getValue());
            UI.getCurrent().getPage().reload();
        });
        return language;
    }
}
