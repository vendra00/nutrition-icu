package t1tanic.nutritionicu.ui.analytics;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.LabResultRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.AnalyteCatalog;

/** Pick a patient and an analyte to see its value-over-time trend and the underlying readings. */
@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics · ICU Nutrition")
public class AnalyticsView extends VerticalLayout {

    private final transient LabResultRepository resultRepository;
    private final transient AnalyteCatalog analyteCatalog;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final RadioButtonGroup<Mode> modeBox = new RadioButtonGroup<>("Comparison");
    private final MultiSelectComboBox<AnalyteOption> analyteBox = new MultiSelectComboBox<>("Analytes");
    private final Div chartHolder = new Div();
    private final Grid<LabResult> grid = new Grid<>(LabResult.class, false);

    /** The selected patient's full option list, restored when filters relax. */
    private List<AnalyteOption> allOptions = List.of();
    /** Guards programmatic item/selection changes from re-triggering filtering. */
    private boolean suppressFilter;

    /** How to combine several analytes on one chart. */
    private enum Mode {
        CROSS("Cross data (normalized)"),
        SAME_UNIT("Same unit (absolute)");

        private final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    /**
     * A pickable analyte. Canonicalised labels are identified by their {@code code} and
     * aggregate every raw-label synonym (e.g. plasma {@code Pla-} and serum {@code Srm-}
     * variants) into one series; labels not yet in the catalog fall back to a single raw name.
     * {@code unit} is the analyte's raw unit, used to group same-unit analytes.
     */
    private record AnalyteOption(String label, String code, String rawName, String unit) {
        boolean byCode() {
            return code != null;
        }
    }

    public AnalyticsView(PatientRepository patientRepository,
                         LabResultRepository resultRepository,
                         AnalyteCatalog analyteCatalog) {
        this.resultRepository = resultRepository;
        this.analyteCatalog = analyteCatalog;
        setWidthFull();
        setPadding(true);
        add(new H2("Analytics"));

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> onPatientSelected(e.getValue()));

        modeBox.setItems(Mode.values());
        modeBox.setItemLabelGenerator(m -> m.label);
        modeBox.setValue(Mode.CROSS);
        modeBox.addValueChangeListener(e -> onModeChanged());

        analyteBox.setItemLabelGenerator(AnalyteOption::label);
        analyteBox.setEnabled(false);
        analyteBox.setHelperText("One analyte: absolute values + reference band. "
                + "Several: overlaid per the comparison mode.");
        analyteBox.addValueChangeListener(e -> onAnalytesChanged());

        HorizontalLayout controls = new HorizontalLayout(patientBox, modeBox, analyteBox);
        controls.setAlignItems(Alignment.END);
        add(controls);

        chartHolder.setWidthFull();
        add(chartHolder);

        grid.addColumn(r -> analyteCatalog.displayName(r.getAnalyteName()))
                .setHeader("Analyte").setAutoWidth(true);
        grid.addColumn(r -> UiFormat.dateTime(r.getObservedAt())).setHeader("Observed").setAutoWidth(true);
        grid.addColumn(LabResult::getValueRaw).setHeader("Value").setAutoWidth(true);
        grid.addColumn(LabResult::getUnitRaw).setHeader("Unit").setAutoWidth(true);
        grid.addColumn(LabResult::getFlag).setHeader("Flag").setAutoWidth(true);
        grid.addColumn(LabResult::getRefRaw).setHeader("Reference").setAutoWidth(true);
        add(grid);
    }

    private void onPatientSelected(Patient patient) {
        suppressFilter = true;
        analyteBox.clear();
        chartHolder.removeAll();
        grid.setItems(List.of());
        allOptions = patient == null ? List.of() : buildOptions(patient.getId());
        analyteBox.setItems(allOptions);
        analyteBox.setEnabled(patient != null);
        suppressFilter = false;
        renderSeries();
    }

    /** Switching mode starts a fresh comparison: clear the picker and relax any unit filter. */
    private void onModeChanged() {
        suppressFilter = true;
        analyteBox.clear();
        analyteBox.setItems(allOptions);
        suppressFilter = false;
        renderSeries();
    }

    private void onAnalytesChanged() {
        if (suppressFilter) {
            return;
        }
        if (modeBox.getValue() == Mode.SAME_UNIT) {
            applyUnitFilter();
        }
        renderSeries();
    }

    /** In same-unit mode, once one analyte is picked, restrict the picker to analytes sharing its unit. */
    private void applyUnitFilter() {
        Set<AnalyteOption> selected = new LinkedHashSet<>(analyteBox.getValue());
        List<AnalyteOption> allowed;
        if (selected.isEmpty()) {
            allowed = allOptions;
        } else {
            String anchor = selected.iterator().next().unit();
            selected.removeIf(o -> !sameUnit(o.unit(), anchor));
            allowed = allOptions.stream()
                    .filter(o -> selected.contains(o) || sameUnit(o.unit(), anchor))
                    .toList();
        }
        // setItems() clears the MultiSelectComboBox selection, so restore it (the allowed list keeps it).
        suppressFilter = true;
        analyteBox.setItems(allowed);
        analyteBox.setValue(selected);
        suppressFilter = false;
    }

    private static boolean sameUnit(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    /** Collapses raw labels into one option per canonical code (synonyms merged), carrying each unit. */
    private List<AnalyteOption> buildOptions(Long patientId) {
        Map<String, String> unitByName = new HashMap<>();
        for (Object[] row : resultRepository.findAnalyteNameUnits(patientId)) {
            unitByName.compute((String) row[0], (name, current) -> current != null ? current : (String) row[1]);
        }

        Map<String, AnalyteOption> byCode = new LinkedHashMap<>();
        List<AnalyteOption> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : unitByName.entrySet()) {
            String raw = entry.getKey();
            String unit = entry.getValue();
            String code = analyteCatalog.codeFor(raw);
            if (code == null) {
                options.add(new AnalyteOption(analyteCatalog.displayName(raw), null, raw, unit));
            } else {
                byCode.computeIfAbsent(code, c -> new AnalyteOption(analyteCatalog.displayName(raw), c, null, unit));
            }
        }
        options.addAll(byCode.values());
        options.sort(Comparator.comparing(AnalyteOption::label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private void renderSeries() {
        chartHolder.removeAll();
        Patient patient = patientBox.getValue();
        Set<AnalyteOption> selected = analyteBox.getValue();
        if (patient == null || selected.isEmpty()) {
            grid.setItems(List.of());
            return;
        }

        List<AnalyteOption> analytes = selected.stream()
                .sorted(Comparator.comparing(AnalyteOption::label, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LabResult> allReadings = new ArrayList<>();
        List<TrendChart.Series> chartSeries = new ArrayList<>();
        for (AnalyteOption analyte : analytes) {
            List<LabResult> readings = readingsFor(patient, analyte);
            allReadings.addAll(readings);
            chartSeries.add(new TrendChart.Series(analyte.label(), pointsOf(readings)));
        }
        allReadings.sort(Comparator.comparing(LabResult::getObservedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        grid.setItems(allReadings);

        if (analytes.size() == 1) {
            // Single analyte: keep absolute values, reference band and unit.
            Double refLow = lastNonNull(allReadings, LabResult::getRefLow);
            Double refHigh = lastNonNull(allReadings, LabResult::getRefHigh);
            String unit = allReadings.stream().map(LabResult::getUnitRaw)
                    .filter(Objects::nonNull).reduce((a, b) -> b).orElse(null);
            chartHolder.add(new TrendChart(pointsOf(allReadings), refLow, refHigh, unit));
        } else if (modeBox.getValue() == Mode.SAME_UNIT) {
            // All share a unit: overlay on one absolute axis.
            chartHolder.add(new TrendChart(chartSeries, analytes.get(0).unit()));
        } else {
            // Mixed units: normalize each line to its own range.
            chartHolder.add(new TrendChart(chartSeries));
        }
    }

    private List<LabResult> readingsFor(Patient patient, AnalyteOption analyte) {
        return analyte.byCode()
                ? resultRepository.findByPatientIdAndAnalyteCodeOrderByObservedAtAsc(patient.getId(), analyte.code())
                : resultRepository.findByPatientIdAndAnalyteNameOrderByObservedAtAsc(patient.getId(), analyte.rawName());
    }

    private static List<TrendChart.Point> pointsOf(List<LabResult> readings) {
        return readings.stream()
                .filter(r -> r.getValueNumeric() != null && r.getObservedAt() != null)
                .map(r -> new TrendChart.Point(
                        r.getObservedAt().atZone(ZoneId.systemDefault()).toInstant(),
                        r.getValueNumeric().doubleValue()))
                .toList();
    }

    private Double lastNonNull(List<LabResult> series, java.util.function.Function<LabResult, BigDecimal> getter) {
        return series.stream().map(getter).filter(Objects::nonNull)
                .reduce((a, b) -> b).map(BigDecimal::doubleValue).orElse(null);
    }
}
