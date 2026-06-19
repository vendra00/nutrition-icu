package t1tanic.nutritionicu.ui.insights;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Placeholder for progression insights (rate-of-change detection, likely scenarios). */
@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights · ICU Nutrition")
public class InsightsView extends VerticalLayout {

    public InsightsView() {
        setPadding(true);
        add(new H2("Insights"));
        add(new Paragraph("Coming soon: progression insights for monitored patients — "
                + "rate-of-change detection (e.g. ≥20% rise in creatinine), "
                + "and guidance on likely scenarios from the accumulated trends."));
    }
}
