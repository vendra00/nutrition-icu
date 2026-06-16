package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.service.AlertService;

/** All alerts raised for monitored patients, newest first. */
@Route(value = "alerts", layout = MainLayout.class)
@PageTitle("Alerts · ICU Nutrition")
public class AlertsView extends VerticalLayout {

    public AlertsView(AlertService alertService) {
        setSizeFull();
        setPadding(true);
        add(new H2("Alerts"));

        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        grid.addColumn(AlertSummary::severity).setHeader("Severity").setAutoWidth(true);
        grid.addColumn(AlertSummary::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(AlertSummary::patientMrn).setHeader("Patient (NHC)").setAutoWidth(true);
        grid.addColumn(AlertSummary::sectors).setHeader("Sectors").setAutoWidth(true);
        grid.addColumn(AlertSummary::message).setHeader("Details").setFlexGrow(3);
        grid.addColumn(AlertSummary::createdAt).setHeader("Raised").setAutoWidth(true);
        grid.setItems(alertService.recentAlerts());
        grid.setSizeFull();
        addAndExpand(grid);
    }
}
