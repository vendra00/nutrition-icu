package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Placeholder for per-analyte exploration and trend charts (uses observed_at / analyte_code). */
@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics · ICU Nutrition")
public class AnalyticsView extends VerticalLayout {

    public AnalyticsView() {
        setPadding(true);
        add(new H2("Analytics"));
        add(new Paragraph("Coming soon: per-patient analyte trends over time "
                + "(e.g. platelets, creatinine, glucose) charted from observed_at, "
                + "plus filtering by analyte code and reference-range breaches."));
    }
}
