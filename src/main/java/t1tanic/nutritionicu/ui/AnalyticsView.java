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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.LabResultRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.AnalyteCatalog;

/** Pick a patient and an analyte to see its value-over-time trend and the underlying readings. */
@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics · ICU Nutrition")
public class AnalyticsView extends VerticalLayout {

    private final LabResultRepository resultRepository;
    private final AnalyteCatalog analyteCatalog;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<AnalyteOption> analyteBox = new ComboBox<>("Analyte");
    private final Div chartHolder = new Div();
    private final Grid<LabResult> grid = new Grid<>(LabResult.class, false);

    /**
     * A pickable analyte. Canonicalised labels are identified by their {@code code} and
     * aggregate every raw-label synonym (e.g. plasma {@code Pla-} and serum {@code Srm-}
     * variants) into one series; labels not yet in the catalog fall back to a single raw name.
     */
    private record AnalyteOption(String label, String code, String rawName) {
        boolean byCode() {
            return code != null;
        }
    }

    public AnalyticsView(PatientRepository patientRepository,
                         LabResultRepository resultRepository,
                         AnalyteCatalog analyteCatalog) {
        this.resultRepository = resultRepository;
        this.analyteCatalog = analyteCatalog;
        setSizeFull();
        setPadding(true);
        add(new H2("Analytics"));

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> onPatientSelected(e.getValue()));

        analyteBox.setItemLabelGenerator(AnalyteOption::label);
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
        analyteBox.setItems(buildOptions(resultRepository.findDistinctAnalyteNames(patient.getId())));
        analyteBox.setEnabled(true);
    }

    /** Collapses raw analyte labels into one option per canonical code (synonyms merged). */
    private List<AnalyteOption> buildOptions(List<String> rawNames) {
        Map<String, AnalyteOption> byCode = new LinkedHashMap<>();
        List<AnalyteOption> options = new ArrayList<>();
        for (String raw : rawNames) {
            String code = analyteCatalog.codeFor(raw);
            if (code == null) {
                options.add(new AnalyteOption(analyteCatalog.displayName(raw), null, raw));
            } else {
                byCode.computeIfAbsent(code, c -> new AnalyteOption(analyteCatalog.displayName(raw), c, null));
            }
        }
        options.addAll(byCode.values());
        options.sort(Comparator.comparing(AnalyteOption::label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private void renderSeries() {
        chartHolder.removeAll();
        Patient patient = patientBox.getValue();
        AnalyteOption analyte = analyteBox.getValue();
        if (patient == null || analyte == null) {
            grid.setItems(List.of());
            return;
        }

        List<LabResult> series = analyte.byCode()
                ? resultRepository.findByPatientIdAndAnalyteCodeOrderByObservedAtAsc(patient.getId(), analyte.code())
                : resultRepository.findByPatientIdAndAnalyteNameOrderByObservedAtAsc(patient.getId(), analyte.rawName());
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
