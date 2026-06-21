package t1tanic.nutritionicu.ui.dashboard;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.theme.lumo.LumoUtility;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.service.alert.AlertService;
import t1tanic.nutritionicu.service.patient.DoctorService;
import t1tanic.nutritionicu.service.patient.PatientService;

/** Landing overview: headline counts and the most recent alerts. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard · ICU Nutrition")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(AlertService alertService,
                         PatientService patientService,
                         DoctorService doctorService) {
        setPadding(true);
        setSpacing(true);

        var alerts = alertService.recentAlerts();
        HorizontalLayout cards = new HorizontalLayout(
                statCard("Monitored patients", patientService.findMonitored().size()),
                statCard("Active alerts", alerts.size()),
                statCard("Doctors", (int) doctorService.count()));
        cards.setWidthFull();
        add(cards);

        add(new H2("Recent alerts"));
        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        grid.addColumn(AlertSummary::severity).setHeader("Severity").setAutoWidth(true);
        grid.addColumn(AlertSummary::patientMrn).setHeader("Patient (NHC)").setAutoWidth(true);
        grid.addColumn(AlertSummary::sectors).setHeader("Sectors").setAutoWidth(true);
        grid.addColumn(AlertSummary::message).setHeader("Details").setFlexGrow(3);
        grid.addColumn(AlertSummary::createdAt).setHeader("Raised").setAutoWidth(true);
        grid.setItems(alerts);
        add(grid);
    }

    private Component statCard(String label, int value) {
        Span number = new Span(String.valueOf(value));
        number.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
        Span caption = new Span(label);
        caption.addClassNames(LumoUtility.TextColor.SECONDARY);
        VerticalLayout card = new VerticalLayout(number, caption);
        card.setSpacing(false);
        card.setPadding(true);
        card.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.LARGE);
        card.setWidth("200px");
        return card;
    }
}
