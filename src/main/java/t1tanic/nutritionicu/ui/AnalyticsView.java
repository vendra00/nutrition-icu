package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.LabResultRepository;
import t1tanic.nutritionicu.repo.PatientRepository;

/** Pick a patient and an analyte to see its value-over-time trend and the underlying readings. */
@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics · ICU Nutrition")
public class AnalyticsView extends VerticalLayout {

    private final LabResultRepository resultRepository;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<String> analyteBox = new ComboBox<>("Analyte");
    private final Div chartHolder = new Div();
    private final Grid<LabResult> grid = new Grid<>(LabResult.class, false);

    public AnalyticsView(PatientRepository patientRepository, LabResultRepository resultRepository) {
        this.resultRepository = resultRepository;
        setSizeFull();
        setPadding(true);
        add(new H2("Analytics"));

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> onPatientSelected(e.getValue()));

        analyteBox.setEnabled(false);
        analyteBox.addValueChangeListener(e -> renderSeries());

        add(new HorizontalLayout(patientBox, analyteBox));

        chartHolder.setWidthFull();
        add(chartHolder);

        grid.addColumn(LabResult::getObservedAt).setHeader("Observed").setAutoWidth(true);
        grid.addColumn(LabResult::getValueRaw).setHeader("Value").setAutoWidth(true);
        grid.addColumn(LabResult::getUnitRaw).setHeader("Unit").setAutoWidth(true);
        grid.addColumn(LabResult::getFlag).setHeader("Flag").setAutoWidth(true);
        grid.addColumn(LabResult::getRefRaw).setHeader("Reference").setAutoWidth(true);
        add(grid);
    }

    private void onPatientSelected(Patient patient) {
        analyteBox.clear();
        chartHolder.removeAll();
        grid.setItems(List.of());
        if (patient == null) {
            analyteBox.setEnabled(false);
            return;
        }
        analyteBox.setItems(resultRepository.findDistinctAnalyteNames(patient.getId()));
        analyteBox.setEnabled(true);
    }

    private void renderSeries() {
        chartHolder.removeAll();
        Patient patient = patientBox.getValue();
        String analyte = analyteBox.getValue();
        if (patient == null || analyte == null) {
            grid.setItems(List.of());
            return;
        }

        List<LabResult> series =
                resultRepository.findByPatientIdAndAnalyteNameOrderByObservedAtAsc(patient.getId(), analyte);
        grid.setItems(series);

        List<TrendChart.Point> points = series.stream()
                .filter(r -> r.getValueNumeric() != null && r.getObservedAt() != null)
                .map(r -> new TrendChart.Point(
                        r.getObservedAt().atZone(ZoneId.systemDefault()).toInstant(),
                        r.getValueNumeric().doubleValue()))
                .toList();

        Double refLow = lastNonNull(series, LabResult::getRefLow);
        Double refHigh = lastNonNull(series, LabResult::getRefHigh);
        String unit = series.stream().map(LabResult::getUnitRaw)
                .filter(Objects::nonNull).reduce((a, b) -> b).orElse(null);

        chartHolder.add(new TrendChart(points, refLow, refHigh, unit));
    }

    private Double lastNonNull(List<LabResult> series, java.util.function.Function<LabResult, BigDecimal> getter) {
        return series.stream().map(getter).filter(Objects::nonNull)
                .reduce((a, b) -> b).map(BigDecimal::doubleValue).orElse(null);
    }
}
