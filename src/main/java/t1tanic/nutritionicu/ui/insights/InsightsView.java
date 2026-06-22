package t1tanic.nutritionicu.ui.insights;
import t1tanic.nutritionicu.ui.MainLayout;
import t1tanic.nutritionicu.ui.common.BarList;
import t1tanic.nutritionicu.ui.common.Donut;
import t1tanic.nutritionicu.ui.common.TrendChart;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import t1tanic.nutritionicu.dto.KnowledgeRef;
import t1tanic.nutritionicu.dto.PatientInsight;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.NutritionDelivery;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.PatientCase;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.model.enums.InsightLanguage;
import t1tanic.nutritionicu.model.enums.InsightType;
import t1tanic.nutritionicu.service.insight.InsightService;
import t1tanic.nutritionicu.service.insight.KnowledgeBaseService;
import t1tanic.nutritionicu.service.insight.PatientCaseService;
import t1tanic.nutritionicu.service.lab.LabResultService;
import t1tanic.nutritionicu.service.nutrition.EnergyAssessmentService;
import t1tanic.nutritionicu.service.nutrition.NutritionDeliveryService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * AI-assisted nutrition insights: pick a monitored patient to see their nutrition trend charts and have
 * Claude analyse a de-identified snapshot. Results are saved, so identical inputs reuse the stored
 * answer (no API call) and earlier analyses can be reviewed and compared. Decision support only.
 */
@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights · ICU Nutrition")
@PermitAll
public class InsightsView extends VerticalLayout {

    /** Nutrition labs charted under Trends, in clinical priority order; the first few with data are shown. */
    private static final Map<String, String> CHART_LABS = new LinkedHashMap<>();

    static {
        CHART_LABS.put("GLUCOSE", "Glucose");
        CHART_LABS.put("CRP", "C-reactive protein");
        CHART_LABS.put("PHOSPHATE", "Phosphate");
        CHART_LABS.put("POTASSIUM", "Potassium");
        CHART_LABS.put("ALBUMIN", "Albumin");
        CHART_LABS.put("PREALBUMIN", "Prealbumin");
        CHART_LABS.put("MAGNESIUM", "Magnesium");
        CHART_LABS.put("UREA", "Urea");
    }

    private static final int MAX_LAB_CHARTS = 6;
    /** How many of the nearest archived cases to chart in the Cohort tab (a broader set than the AI compare). */
    private static final int CHART_PEERS = 12;
    private static final String GREEN = "#2E7D32";
    private static final String RED = "#C62828";
    private static final String GREY = "#9E9E9E";
    private static final String BLUE = "#1565C0";
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final transient InsightService insightService;
    private final transient PatientCaseService patientCaseService;
    private final transient KnowledgeBaseService knowledgeBase;
    private final transient LabResultService labService;
    private final transient EnergyAssessmentService energyService;
    private final transient NutritionDeliveryService deliveryService;

    private final ComboBox<InsightLanguage> languagePicker = new ComboBox<>("Language");
    private final Button generate = new Button("Generate insights");
    private final Button compare = new Button("Compare similar patients");
    private final Button archive = new Button("Archive as case");
    private final Span archiveInfo = new Span();
    private final ProgressBar progress = new ProgressBar();
    private final Details trends = new Details();
    private final Span status = new Span();
    private final Markdown result = new Markdown();
    private final Div resultCard = new Div(result);
    private final Details grounding = new Details();
    private final Pre sent = new Pre();
    private final Details sentDetails = new Details("Data sent to the model (de-identified)", sent);
    private final Div historyContainer = new Div();

    public InsightsView(InsightService insightService, PatientCaseService patientCaseService,
                        KnowledgeBaseService knowledgeBase, PatientService patientService,
                        LabResultService labService, EnergyAssessmentService energyService,
                        NutritionDeliveryService deliveryService) {
        this.insightService = insightService;
        this.patientCaseService = patientCaseService;
        this.knowledgeBase = knowledgeBase;
        this.labService = labService;
        this.energyService = energyService;
        this.deliveryService = deliveryService;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H2("Insights"));
        Paragraph intro = new Paragraph("Select a monitored patient to see their nutrition trend charts, "
                + "and let Claude analyse a de-identified snapshot (labs over time, anthropometry, NUTRIC "
                + "risk, energy assessments and feed delivery) for trends and suggested actions.");
        intro.addClassNames(LumoUtility.TextColor.SECONDARY);
        Paragraph disclaimer = new Paragraph("Decision support only — not a prescription. No name or NHC "
                + "is sent; the exact data shared is shown below each result. Analyses are saved for review.");
        disclaimer.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        add(intro, disclaimer);

        ComboBox<Patient> picker = new ComboBox<>("Patient");
        picker.setItems(patientService.findMonitored());
        picker.setItemLabelGenerator(InsightsView::patientLabel);
        picker.setWidth("320px");

        languagePicker.setItems(InsightLanguage.values());
        languagePicker.setItemLabelGenerator(InsightLanguage::label);
        languagePicker.setValue(InsightLanguage.EN);
        languagePicker.setAllowCustomValue(false);
        languagePicker.setClearButtonVisible(false);
        languagePicker.setWidth("180px");

        generate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        archive.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout controls = new HorizontalLayout(picker, languagePicker, generate, compare, archive);
        controls.setAlignItems(Alignment.BASELINE);
        add(controls);

        archiveInfo.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        add(archiveInfo);
        updateArchiveInfo();

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setWidth("320px");
        add(progress);

        trends.setWidthFull();

        status.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        status.setVisible(false);

        decorate(resultCard);
        resultCard.setVisible(false);

        grounding.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        grounding.setVisible(false);

        sent.getStyle().set("white-space", "pre-wrap").set("margin", "0")
                .set("font-size", "var(--lumo-font-size-s)");
        sentDetails.setVisible(false);

        historyContainer.setWidthFull();

        add(status, resultCard, grounding, sentDetails, trends, historyContainer);

        if (!insightService.isConfigured()) {
            generate.setEnabled(false);
            compare.setEnabled(false);
            add(setupNotice());
        }

        picker.addValueChangeListener(e -> {
            Patient patient = e.getValue();
            if (patient == null) {
                clearResult();
                trends.removeAll();
                trends.setVisible(false);
                historyContainer.removeAll();
                return;
            }
            renderTrends(patient.getId());
            renderHistory(patient.getId());
            showLatestInsight(patient.getId());
        });

        generate.addClickListener(e -> launch(picker.getValue(), false));
        compare.addClickListener(e -> launch(picker.getValue(), true));
        archive.addClickListener(e -> archiveSelected(picker.getValue()));
    }

    /** Snapshots the selected patient into the anonymized case archive (manual, idempotent). */
    private void archiveSelected(Patient patient) {
        if (patient == null) {
            Notification.show("Select a patient first.");
            return;
        }
        try {
            PatientCase archived = patientCaseService.archive(patient.getId());
            updateArchiveInfo();
            Notification.show("Archived as " + archived.getCaseCode() + ". The case archive now has "
                    + patientCaseService.archiveSize() + " case(s).");
        } catch (Exception ex) {
            Notification error = Notification.show("Archive failed: " + rootMessage(ex), 5000,
                    Notification.Position.MIDDLE);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateArchiveInfo() {
        long n = patientCaseService.archiveSize();
        archiveInfo.setText("Case archive: " + n + " anonymized case(s). Comparison searches this archive.");
    }

    /** Runs an analysis or comparison off the UI thread, updating via polling so the UI stays responsive. */
    private void launch(Patient patient, boolean comparison) {
        if (patient == null) {
            Notification.show("Select a patient first.");
            return;
        }
        UI ui = UI.getCurrent();
        Long patientId = patient.getId();
        InsightLanguage language = languagePicker.getValue();
        generate.setEnabled(false);
        compare.setEnabled(false);
        progress.setVisible(true);
        clearResult();
        ui.setPollInterval(500);

        Thread worker = new Thread(() -> {
            try {
                PatientInsight insight = comparison
                        ? insightService.compare(patientId, language)
                        : insightService.analyze(patientId, language);
                ui.access(() -> {
                    showInsight(insight, statusFor(insight));
                    renderHistory(patientId);
                });
            } catch (Exception ex) {
                ui.access(() -> {
                    Notification error = Notification.show("AI request failed: " + rootMessage(ex), 6000,
                            Notification.Position.MIDDLE);
                    error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            } finally {
                ui.access(() -> {
                    progress.setVisible(false);
                    generate.setEnabled(true);
                    compare.setEnabled(true);
                    ui.setPollInterval(-1);
                });
            }
        }, "ai-insight");
        worker.setDaemon(true);
        worker.start();
    }

    // --- Result rendering (shared by generation and history review) ---

    private void showInsight(PatientInsight insight, String statusText) {
        result.setContent(insight.markdown());
        sent.setText(insight.deidentifiedSummary());
        status.setText(statusText);
        status.setVisible(true);
        grounding.removeAll();
        List<KnowledgeRef> refs = insight.knowledgeSources();
        if (refs.isEmpty()) {
            grounding.setVisible(false);
        } else {
            grounding.setSummaryText("Grounded on " + refs.size() + " reference(s)");
            UnorderedList list = new UnorderedList();
            for (KnowledgeRef ref : refs) {
                list.add(new ListItem(referenceLink(ref)));
            }
            grounding.add(list);
            grounding.setOpened(true);
            grounding.setVisible(true);
        }
        resultCard.setVisible(true);
        sentDetails.setVisible(true);
    }

    /** A reference rendered as a link that opens the source PDF in a new tab; plain text if missing. */
    private Component referenceLink(KnowledgeRef ref) {
        if (!knowledgeBase.exists(ref.fileName())) {
            return new Span(ref.title());
        }
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = knowledgeBase.read(ref.fileName());
            return new DownloadResponse(new ByteArrayInputStream(bytes), ref.fileName(),
                    "application/pdf", bytes.length);
        }).inline();
        Anchor anchor = new Anchor(handler, ref.title());
        anchor.setTarget("_blank");
        anchor.getElement().setAttribute("title", ref.fileName());
        return anchor;
    }

    /** On selecting a patient, show their most recent saved insight (if any) instead of an empty result. */
    private void showLatestInsight(Long patientId) {
        List<PatientInsight> past = insightService.history(patientId);
        if (past.isEmpty()) {
            clearResult();
            return;
        }
        PatientInsight latest = past.get(0); // history is newest first
        languagePicker.setValue(latest.language());
        String noun = latest.type() == InsightType.COMPARISON ? "comparison" : "analysis";
        showInsight(latest, "Last saved " + noun + " (" + latest.language().label() + ") from "
                + TS.format(latest.createdAt()));
    }

    private static String statusFor(PatientInsight insight) {
        String when = TS.format(insight.createdAt());
        String lang = insight.language().label();
        String noun = insight.type() == InsightType.COMPARISON ? "comparison" : "analysis";
        if (insight.cached()) {
            return "Reused an identical earlier " + lang + " " + noun + " from " + when + " — no API call (cost saved)";
        }
        if (insight.translated()) {
            return "Translated to " + lang + " from the existing " + noun + " · " + when;
        }
        return "New " + lang + " " + noun + " · " + when;
    }

    private void clearResult() {
        status.setVisible(false);
        resultCard.setVisible(false);
        grounding.setVisible(false);
        sentDetails.setVisible(false);
        result.setContent("");
    }

    private void renderHistory(Long patientId) {
        historyContainer.removeAll();
        List<PatientInsight> past = insightService.history(patientId);
        if (past.isEmpty()) {
            return;
        }
        H3 heading = new H3("Previous analyses (" + past.size() + ")");
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL);
        Div list = new Div();
        list.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "var(--lumo-space-xs)");
        for (PatientInsight item : past) {
            String when = TS.format(item.createdAt());
            String noun = item.type() == InsightType.COMPARISON ? "comparison" : "analysis";
            Button open = new Button(
                    when + " · " + item.type().label() + " · " + item.language().name() + " · " + item.model(), e -> {
                        languagePicker.setValue(item.language());
                        showInsight(item, "Saved " + item.language().label() + " " + noun + " from " + when);
                    });
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            list.add(open);
        }
        historyContainer.add(heading, list);
    }

    // --- Trend charts (rendered from the DB on patient selection; independent of the AI call) ---

    private void renderTrends(Long patientId) {
        trends.removeAll();

        List<Component> labCharts = new ArrayList<>();
        for (Map.Entry<String, String> entry : CHART_LABS.entrySet()) {
            if (labCharts.size() >= MAX_LAB_CHARTS) {
                break;
            }
            labChart(patientId, entry.getKey(), entry.getValue()).ifPresent(labCharts::add);
        }
        Optional<Component> energy = energyChart(patientId);
        Optional<Component> delivery = deliveryChart(patientId);

        record TrendTab(String label, Component content) {
        }
        List<TrendTab> defs = new ArrayList<>();
        cohortTab(patientId).ifPresent(c -> defs.add(new TrendTab("Cohort", c)));
        if (!labCharts.isEmpty()) {
            defs.add(new TrendTab("Labs", stack(labCharts)));
        }
        energy.ifPresent(c -> defs.add(new TrendTab("Energy", c)));
        delivery.ifPresent(c -> defs.add(new TrendTab("Delivery", c)));

        if (defs.isEmpty()) {
            trends.setVisible(false);
            return;
        }

        Tabs tabs = new Tabs();
        tabs.setWidthFull();
        Div panels = new Div();
        panels.setWidthFull();
        List<Component> contents = new ArrayList<>();
        for (TrendTab def : defs) {
            tabs.add(new Tab(def.label()));
            contents.add(def.content());
            panels.add(def.content());
        }
        for (int i = 0; i < contents.size(); i++) {
            contents.get(i).setVisible(i == 0);
        }
        tabs.addSelectedChangeListener(e -> {
            int index = tabs.getSelectedIndex();
            for (int i = 0; i < contents.size(); i++) {
                contents.get(i).setVisible(i == index);
            }
        });
        trends.setSummaryText("Trends & charts");
        trends.setOpened(false); // collapsed so the insight shows first
        trends.setVisible(true);
        trends.add(tabs, panels);
    }

    private static Div stack(List<Component> charts) {
        Div column = new Div();
        column.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)").set("width", "100%");
        charts.forEach(column::add);
        return column;
    }

    /** Charts comparing the selected patient against the most similar archived cases. */
    private Optional<Component> cohortTab(Long patientId) {
        PatientCaseService.CohortComparison cc = patientCaseService.compareCohort(patientId, CHART_PEERS);
        if (!cc.hasPeers()) {
            return Optional.empty();
        }
        List<PatientCase> peers = cc.peers();
        List<Component> charts = new ArrayList<>();

        Paragraph intro = new Paragraph("The selected patient vs the " + peers.size()
                + " most similar archived cases.");
        intro.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        charts.add(intro);

        long survived = peers.stream().filter(p -> Boolean.TRUE.equals(p.getDischarged())).count();
        long died = peers.stream().filter(p -> Boolean.FALSE.equals(p.getDischarged())).count();
        long unknown = peers.size() - survived - died;
        Donut outcomes = new Donut(List.of(
                new Donut.Slice("Recovered", survived, GREEN),
                new Donut.Slice("Died", died, RED),
                new Donut.Slice("Unknown", unknown, GREY)),
                peers.size() + " cases");
        charts.add(chartCard("Outcomes of the " + peers.size() + " most similar cases", outcomes));

        charts.add(chartCard("NUTRIC: selected patient vs similar cases",
                comparisonBars(cc.nutric() == null ? null : cc.nutric().doubleValue(), peers,
                        p -> p.getNutricScore() == null ? null : p.getNutricScore().doubleValue())));
        charts.add(chartCard("BMI: selected patient vs similar cases",
                comparisonBars(cc.bmi(), peers, PatientCase::getBmi)));
        charts.add(chartCard("Length of stay (days), green = recovered / red = died", staysBars(peers)));

        return Optional.of(stack(charts));
    }

    private static BarList comparisonBars(Double indexValue, List<PatientCase> peers,
                                          Function<PatientCase, Double> metric) {
        List<BarList.Bar> bars = new ArrayList<>();
        if (indexValue != null) {
            bars.add(new BarList.Bar("Selected patient", indexValue, BLUE));
        }
        for (PatientCase p : peers) {
            Double value = metric.apply(p);
            if (value != null) {
                bars.add(new BarList.Bar(p.getCaseCode(), value, GREY));
            }
        }
        return new BarList(bars);
    }

    private static BarList staysBars(List<PatientCase> peers) {
        List<BarList.Bar> bars = new ArrayList<>();
        for (PatientCase p : peers) {
            if (p.getLengthOfStayDays() != null) {
                String color = Boolean.FALSE.equals(p.getDischarged()) ? RED : GREEN;
                bars.add(new BarList.Bar(p.getCaseCode(), p.getLengthOfStayDays(), color));
            }
        }
        return new BarList(bars);
    }

    private Optional<Component> labChart(Long patientId, String code, String label) {
        List<LabResult> series = labService.seriesByCode(patientId, code);
        List<TrendChart.Point> points = new ArrayList<>();
        for (LabResult r : series) {
            if (r.getValueNumeric() != null && r.getObservedAt() != null) {
                points.add(new TrendChart.Point(
                        r.getObservedAt().atZone(ZoneId.systemDefault()).toInstant(),
                        r.getValueNumeric().doubleValue()));
            }
        }
        if (points.size() < 2) {
            return Optional.empty();
        }
        points.sort((a, b) -> a.time().compareTo(b.time()));
        LabResult last = series.get(series.size() - 1);
        Double refLow = last.getRefLow() == null ? null : last.getRefLow().doubleValue();
        Double refHigh = last.getRefHigh() == null ? null : last.getRefHigh().doubleValue();
        String unit = last.getUnitRaw();
        String title = unit == null || unit.isBlank() ? label : label + " (" + unit + ")";
        return Optional.of(chartCard(title, new TrendChart(points, refLow, refHigh, unit)));
    }

    private Optional<Component> energyChart(Long patientId) {
        List<TrendChart.Series> seriesList = new ArrayList<>();
        List<TrendChart.Point> predicted = energyPoints(
                energyService.history(patientId, EnergyMethod.HARRIS_BENEDICT));
        List<TrendChart.Point> measured = energyPoints(
                energyService.history(patientId, EnergyMethod.INDIRECT_CALORIMETRY));
        if (!predicted.isEmpty()) {
            seriesList.add(new TrendChart.Series("Predicted (Harris-Benedict)", predicted));
        }
        if (!measured.isEmpty()) {
            seriesList.add(new TrendChart.Series("Measured (calorimetry)", measured));
        }
        boolean trendable = seriesList.stream().anyMatch(s -> s.points().size() >= 2);
        if (!trendable) {
            return Optional.empty();
        }
        return Optional.of(chartCard("Energy expenditure: measured vs predicted",
                new TrendChart(seriesList, "kcal/day")));
    }

    private static List<TrendChart.Point> energyPoints(List<EnergyAssessment> history) {
        List<TrendChart.Point> points = new ArrayList<>();
        for (EnergyAssessment e : history) {
            if (e.getTotalKcalPerDay() != null && e.getAssessedOn() != null) {
                points.add(new TrendChart.Point(
                        e.getAssessedOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        e.getTotalKcalPerDay()));
            }
        }
        return points;
    }

    private Optional<Component> deliveryChart(Long patientId) {
        List<TrendChart.Point> points = new ArrayList<>();
        for (NutritionDelivery d : deliveryService.history(patientId)) {
            if (d.percentDelivered() != null && d.getMeasuredOn() != null) {
                points.add(new TrendChart.Point(
                        d.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        d.percentDelivered()));
            }
        }
        if (points.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(chartCard("Feed delivery (% of prescribed; 80–100% target shaded)",
                new TrendChart(points, 80.0, 100.0, "%")));
    }

    private static Component chartCard(String title, Component body) {
        H3 heading = new H3(title);
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        Div scroll = new Div(body);
        scroll.getStyle().set("overflow-x", "auto").set("width", "100%");
        Div card = new Div(heading, scroll);
        card.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        decorate(card);
        return card;
    }

    private static String patientLabel(Patient patient) {
        String name = patient.getFullName();
        return name == null || name.isBlank()
                ? patient.getMedicalRecordNumber()
                : patient.getMedicalRecordNumber() + " — " + name;
    }

    private static Component setupNotice() {
        Div notice = new Div();
        notice.add(new Paragraph("AI insights are not configured. Set the ANTHROPIC_API_KEY environment "
                + "variable to an Anthropic API key (console.anthropic.com — separate from a Claude.ai "
                + "subscription) and restart the app."));
        notice.getStyle()
                .set("border", "1px solid var(--lumo-warning-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background-color", "var(--lumo-warning-color-10pct)")
                .set("padding", "var(--lumo-space-m)");
        return notice;
    }

    private static void decorate(Div card) {
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-m)");
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
