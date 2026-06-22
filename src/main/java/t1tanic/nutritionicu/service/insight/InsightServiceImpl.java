package t1tanic.nutritionicu.service.insight;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import t1tanic.nutritionicu.dto.KnowledgeRef;
import t1tanic.nutritionicu.dto.PatientInsight;
import t1tanic.nutritionicu.dto.PatientOverview;
import t1tanic.nutritionicu.model.AiInsight;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.NutritionDelivery;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.model.enums.InsightLanguage;
import t1tanic.nutritionicu.model.enums.InsightType;
import t1tanic.nutritionicu.model.enums.ResultFlag;
import t1tanic.nutritionicu.repo.AiInsightRepository;
import t1tanic.nutritionicu.service.lab.LabResultService;
import t1tanic.nutritionicu.service.nutrition.EnergyAssessmentService;
import t1tanic.nutritionicu.service.nutrition.NutritionDeliveryService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Builds a de-identified clinical summary (no name / NHC) from the patient's stored nutrition data and
 * asks Claude for trends and guideline-aligned suggestions. Each contributing read is independently
 * transactional, so no DB transaction is held open across the (slow) HTTP call to the model.
 */
@Service
public class InsightServiceImpl implements InsightService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd-MM");

    /** Nutrition-relevant analyte codes (canonical) mapped to display labels, in reporting order. */
    private static final Map<String, String> NUTRITION_LABS = new LinkedHashMap<>();

    static {
        NUTRITION_LABS.put("GLUCOSE", "Glucose");
        NUTRITION_LABS.put("UREA", "Urea");
        NUTRITION_LABS.put("CRP", "C-reactive protein");
        NUTRITION_LABS.put("PROCALCITONIN", "Procalcitonin");
        NUTRITION_LABS.put("ALBUMIN", "Albumin");
        NUTRITION_LABS.put("PREALBUMIN", "Prealbumin");
        NUTRITION_LABS.put("TOTAL_PROTEIN", "Total protein");
        NUTRITION_LABS.put("TRIGLYCERIDES", "Triglycerides");
        NUTRITION_LABS.put("PHOSPHATE", "Phosphate");
        NUTRITION_LABS.put("POTASSIUM", "Potassium");
        NUTRITION_LABS.put("MAGNESIUM", "Magnesium");
        NUTRITION_LABS.put("CALCIUM", "Calcium");
        NUTRITION_LABS.put("SODIUM", "Sodium");
        NUTRITION_LABS.put("LACTATE", "Lactate");
    }

    private static final String SYSTEM_PROMPT = """
            You are a clinical nutrition specialist supporting an ICU nutrition team. You receive a \
            DE-IDENTIFIED snapshot of one critically ill patient's nutrition data.

            Produce a STRUCTURED report in GitHub-flavoured Markdown with EXACTLY the following sections, \
            in this order, and nothing else. Respond in the language requested in the user's message \
            (translate the headings), but keep the same section set, order and table columns EVERY time so \
            reports are directly comparable across patients and runs.

            ## Summary
            One or two sentences: the patient (age, sex, BMI, NUTRIC) and the headline nutritional issue.

            ## Key parameters
            A Markdown table with EXACTLY these four columns, in this order:
            Parameter | Latest value | Trend | Interpretation
            Include one row for each of the following that is PRESENT in the data, in this order: BMI, \
            Weight change, NUTRIC, Energy target, Feed delivery %, Glucose, CRP, Procalcitonin, Albumin, \
            Prealbumin, Total protein, Urea, Phosphate, Potassium, Magnesium, Sodium, Triglycerides, \
            Lactate. Omit a row only when that parameter is absent from the data. "Trend" is one of up, \
            down, stable or n/a. Keep numbers and units exactly as given; never invent values.

            ## Likely nutritional concerns
            3-6 bullet points.

            ## Suggested actions
            3-6 concise bullet points grounded in ESPEN/ASPEN ICU principles, with per-kg energy/protein \
            targets where appropriate. When reference material is provided, cite the document by name.

            ## Cautions & what to verify
            3-6 bullet points; explicitly flag refeeding risk when phosphate, potassium or magnesium are \
            low or falling. End with a one-line caveat that this is decision support, not a prescription.

            Rules: never invent values or rows; do not add, remove, rename or reorder sections or table \
            columns; flag missing data as such.""";

    private static final String TRANSLATE_SYSTEM = """
            You are a medical translator. Translate the user's clinical nutrition analysis into the \
            requested language. Preserve the Markdown structure and headings (translate the heading text), \
            all clinical meaning, numbers, units and any document names cited in parentheses (keep file \
            names verbatim). Do not add, remove or reinterpret clinical content. Output only the translated \
            Markdown.""";

    private static final String COMPARISON_SYSTEM = """
            You are a clinical nutrition specialist. You receive an INDEX ICU patient and a set of \
            DE-IDENTIFIED archived cases, each labelled with a CASE-id, with their features and course \
            (lab trajectories, weight, length of stay/outcome).

            Produce a STRUCTURED comparison report in GitHub-flavoured Markdown with EXACTLY the following \
            sections, in this order, and nothing else. Respond in the language requested in the user's \
            message (translate the headings), but keep the section set, order, table COLUMNS and table ROWS \
            identical EVERY time so reports are directly comparable.

            ## Summary
            One or two sentences: the index patient (age, sex, BMI, NUTRIC, SOFA) and how many archived \
            cases were compared.

            ## Comparison table
            A single Markdown table. The first column is "Parameter"; the second column is "Index patient"; \
            then ONE column per archived case headed by its CASE-id, in the order the cases are given. \
            Include EXACTLY these rows, in this order, and NO others:
            Age / Sex
            BMI (kg/m2)
            NUTRIC (score/max)
            SOFA
            CRP initial -> latest (mg/dL)
            Albumin initial -> latest (g/dL)
            Prealbumin latest (mg/dL)
            Weight initial -> latest (kg)
            Outcome
            Fill each cell only from the data provided. Where a value is not present write "N/A". Put no \
            commentary inside the table.

            ## Probable course
            3-6 bullet points inferring the index patient's likely trajectory from patterns across the \
            cohort. State the cohort size and that this is associative and low-sample.

            ## What helped similar patients
            3-6 bullet points on interventions associated with better trajectories in the cohort.

            ## Recommended actions & cautions
            3-6 concise bullet points grounded in ESPEN/ASPEN principles; cite reference material by name \
            when provided. End with a one-line caveat that this is hypothesis-generating decision support, \
            not prognosis or a prescription.

            Rules: never invent values or rows; do not add, remove, rename or reorder sections, table \
            columns or table rows; keep numbers and units exactly as given.""";

    /** How many nearest peers to include in a comparison. */
    private static final int MAX_PEERS = 4;

    private final AnthropicClient client;
    private final KnowledgeBaseService knowledgeBase;
    private final PatientCaseService patientCaseService;
    private final AiInsightRepository insightRepository;
    private final PatientService patientService;
    private final PatientOverviewService overviewService;
    private final EnergyAssessmentService energyService;
    private final NutritionDeliveryService deliveryService;
    private final LabResultService labService;

    public InsightServiceImpl(AnthropicClient client, KnowledgeBaseService knowledgeBase,
                              PatientCaseService patientCaseService, AiInsightRepository insightRepository,
                              PatientService patientService, PatientOverviewService overviewService,
                              EnergyAssessmentService energyService, NutritionDeliveryService deliveryService,
                              LabResultService labService) {
        this.client = client;
        this.knowledgeBase = knowledgeBase;
        this.patientCaseService = patientCaseService;
        this.insightRepository = insightRepository;
        this.patientService = patientService;
        this.overviewService = overviewService;
        this.energyService = energyService;
        this.deliveryService = deliveryService;
        this.labService = labService;
    }

    @Override
    public boolean isConfigured() {
        return client.isConfigured();
    }

    @Override
    public PatientInsight analyze(Long patientId, InsightLanguage language) {
        String summary = buildSummary(patientId);
        return resolve(patientId, language, InsightType.ANALYSIS, summary, SYSTEM_PROMPT,
                "Analyse this de-identified ICU patient snapshot and advise the nutrition team.");
    }

    @Override
    public PatientInsight compare(Long patientId, InsightLanguage language) {
        PatientCaseService.Cohort cohort = patientCaseService.cohortContext(patientId, MAX_PEERS);
        if (cohort.size() == 0) {
            throw new IllegalStateException("No comparable archived cases yet — archive some patients as "
                    + "cases first (and the patient needs age and BMI on record).");
        }
        String content = buildSummary(patientId) + "\n\n" + cohort.text();
        return resolve(patientId, language, InsightType.COMPARISON, content, COMPARISON_SYSTEM,
                "Compare the INDEX patient with the similar past patients below. Describe the likely course "
                        + "and which actions were associated with better trajectories.");
    }

    /**
     * Shared 3-tier resolution: exact cache hit (no API call) -> translate an existing same-content insight
     * from another language -> full generation. {@code storedSummary} is hashed, stored and shown to the
     * user; {@code task} is the instruction prefixed to it for a fresh generation.
     */
    private PatientInsight resolve(Long patientId, InsightLanguage language, InsightType type,
                                   String storedSummary, String systemPrompt, String task) {
        String knowledge = knowledgeBase.combinedText();
        String model = client.model();
        // systemPrompt is in the hash so changing the report template invalidates old cached reports.
        String contentHash = sha256(String.join("|", type.name(), model, systemPrompt, knowledge, storedSummary));
        String inputHash = sha256(String.join("|", contentHash, language.name()));

        Optional<AiInsight> exact =
                insightRepository.findFirstByPatientIdAndInputHashOrderByIdDesc(patientId, inputHash);
        if (exact.isPresent()) {
            return toDto(exact.get(), true); // same input + language — reuse, no API call
        }

        Optional<AiInsight> source =
                insightRepository.findFirstByPatientIdAndContentHashOrderByIdAsc(patientId, contentHash);
        if (source.isPresent()) {
            // same content in another language — translate (cheaper than re-generating)
            String translated = client.complete(TRANSLATE_SYSTEM, null, language.instruction()
                    + "\n\nTranslate the following analysis:\n\n" + source.get().getMarkdown());
            return toDto(save(patientId, type, contentHash, inputHash, language, model, storedSummary,
                    translated, source.get().getKnowledgeSources(), true), false);
        }

        String markdown = client.complete(systemPrompt, knowledge,
                language.instruction() + "\n\n" + task + "\n\n" + storedSummary);
        return toDto(save(patientId, type, contentHash, inputHash, language, model, storedSummary,
                markdown, knowledgeBase.referencesForStorage(), false), false);
    }

    private AiInsight save(Long patientId, InsightType type, String contentHash, String inputHash,
                           InsightLanguage language, String model, String summary, String markdown,
                           String knowledgeSources, boolean translated) {
        Patient patient = patientService.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown patient: " + patientId));
        AiInsight entity = new AiInsight();
        entity.setPatient(patient);
        entity.setType(type);
        entity.setContentHash(contentHash);
        entity.setInputHash(inputHash);
        entity.setModel(model);
        entity.setLanguage(language);
        entity.setDeidentifiedSummary(summary);
        entity.setMarkdown(markdown);
        entity.setKnowledgeSources(knowledgeSources);
        entity.setTranslated(translated);
        return insightRepository.save(entity);
    }

    @Override
    public List<PatientInsight> history(Long patientId) {
        return insightRepository.findByPatientIdOrderByCreatedAtDescIdDesc(patientId).stream()
                .map(insight -> toDto(insight, true))
                .toList();
    }

    private static PatientInsight toDto(AiInsight insight, boolean cached) {
        InsightType type = insight.getType() == null ? InsightType.ANALYSIS : insight.getType();
        return new PatientInsight(insight.getId(), insight.getCreatedAt(), cached,
                Boolean.TRUE.equals(insight.getTranslated()), type, insight.getLanguage(), insight.getModel(),
                insight.getDeidentifiedSummary(), insight.getMarkdown(), parseRefs(insight.getKnowledgeSources()));
    }

    /** Parses stored "name\ttitle" lines into references; old rows (file name only) use the name as title. */
    private static List<KnowledgeRef> parseRefs(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        List<KnowledgeRef> refs = new ArrayList<>();
        for (String line : stored.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab < 0) {
                refs.add(new KnowledgeRef(line, line));
            } else {
                refs.add(new KnowledgeRef(line.substring(0, tab), line.substring(tab + 1)));
            }
        }
        return refs;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public String previewSummary(Long patientId) {
        return buildSummary(patientId);
    }

    private String buildSummary(Long patientId) {
        PatientOverview overview = overviewService.build(patientId);
        StringBuilder sb = new StringBuilder();

        PatientOverview.Identity id = overview.identity();
        sb.append("PATIENT (de-identified)\n");
        sb.append("- Age: ").append(numInt(id.ageYears())).append(" years; sex: ").append(id.sex()).append('\n');
        sb.append("- Monitored: ").append(id.monitored() ? "yes" : "no");
        if (id.admissionDate() != null) {
            sb.append("; admitted ").append(id.admissionDate().format(DAY));
        }
        if (id.dischargeDate() != null) {
            sb.append("; discharged ").append(id.dischargeDate().format(DAY));
        }
        sb.append('\n');

        PatientOverview.Anthropometry a = overview.anthropometry();
        sb.append("\nANTHROPOMETRY\n");
        sb.append("- Height: ").append(num(a.heightCm())).append(" cm; current weight: ")
                .append(num(a.currentWeightKg())).append(" kg; usual weight: ").append(num(a.usualWeightKg())).append(" kg\n");
        sb.append("- BMI: ").append(num(a.bmi())).append("; ideal BW: ").append(num(a.idealBodyWeightKg()))
                .append(" kg; adjusted BW: ").append(num(a.adjustedBodyWeightKg())).append(" kg\n");
        sb.append("- Weight loss: ").append(num(a.weightLossPercent())).append(" %\n");
        if (a.latestTemperatureC() != null) {
            sb.append("- Latest temperature: ").append(num(a.latestTemperatureC())).append(" C");
            if (a.latestTemperatureDate() != null) {
                sb.append(" (").append(a.latestTemperatureDate().format(DAY)).append(')');
            }
            sb.append('\n');
        }

        PatientOverview.Risk risk = overview.risk();
        sb.append("\nNUTRITIONAL RISK (NUTRIC)\n");
        if (risk.present()) {
            sb.append("- Score: ").append(risk.nutricScore()).append('/').append(numInt(risk.nutricMax()))
                    .append(Boolean.TRUE.equals(risk.highRisk()) ? " (HIGH risk)" : " (low risk)");
            if (risk.assessedOn() != null) {
                sb.append("; assessed ").append(risk.assessedOn().format(DAY));
            }
            sb.append('\n');
        } else {
            sb.append("- Not assessed\n");
        }

        appendEnergy(sb, patientId);
        appendDelivery(sb, patientId);
        appendLabs(sb, patientId);
        return sb.toString();
    }

    private void appendEnergy(StringBuilder sb, Long patientId) {
        Optional<EnergyAssessment> hb = energyService.latest(patientId, EnergyMethod.HARRIS_BENEDICT);
        Optional<EnergyAssessment> ic = energyService.latest(patientId, EnergyMethod.INDIRECT_CALORIMETRY);
        if (hb.isEmpty() && ic.isEmpty()) {
            return;
        }
        sb.append("\nENERGY EXPENDITURE\n");
        hb.ifPresent(e -> {
            sb.append("- Harris-Benedict (predicted): ").append(numInt(e.getTotalKcalPerDay())).append(" kcal/day");
            if (e.getKcalPerKgPerDay() != null) {
                sb.append(" (~").append(num(e.getKcalPerKgPerDay())).append(" kcal/kg/day)");
            }
            if (e.getStressFactor() != null) {
                sb.append("; stress factor ").append(e.getStressFactor());
            }
            sb.append("; on ").append(e.getAssessedOn().format(DAY)).append('\n');
        });
        ic.ifPresent(e -> {
            sb.append("- Indirect calorimetry (measured): ").append(numInt(e.getTotalKcalPerDay())).append(" kcal/day");
            if (e.getKcalPerKgPerDay() != null) {
                sb.append(" (~").append(num(e.getKcalPerKgPerDay())).append(" kcal/kg/day)");
            }
            if (e.getRq() != null) {
                sb.append("; RQ ").append(num(e.getRq()));
            }
            sb.append("; on ").append(e.getAssessedOn().format(DAY)).append('\n');
        });
    }

    private void appendDelivery(StringBuilder sb, Long patientId) {
        Optional<NutritionDelivery> latest = deliveryService.latest(patientId);
        if (latest.isEmpty()) {
            return;
        }
        NutritionDelivery d = latest.get();
        sb.append("\nFEED DELIVERY (latest)\n");
        sb.append("- Prescribed: ").append(num(d.getPrescribedMlPerHour())).append(" ml/h; actual: ")
                .append(num(d.getActualMlPerHour())).append(" ml/h");
        if (d.percentDelivered() != null) {
            sb.append("; delivered ").append(Math.round(d.percentDelivered())).append("% of prescribed");
        }
        if (d.getKcalPerMl() != null) {
            sb.append("; formula density ").append(num(d.getKcalPerMl())).append(" kcal/ml");
        }
        sb.append("; on ").append(d.getMeasuredOn().format(DAY)).append('\n');
    }

    private void appendLabs(StringBuilder sb, Long patientId) {
        StringBuilder labs = new StringBuilder();
        for (Map.Entry<String, String> entry : NUTRITION_LABS.entrySet()) {
            List<LabResult> series = labService.seriesByCode(patientId, entry.getKey());
            if (series.isEmpty()) {
                continue;
            }
            labs.append("- ").append(entry.getValue()).append(": ").append(formatSeries(series)).append('\n');
        }
        if (!labs.isEmpty()) {
            sb.append("\nNUTRITION-RELEVANT LABS (most recent first, up to 4 readings)\n").append(labs);
        }
    }

    /** Last up to 4 readings, newest first: "132 mg/dL (HIGH) 12-06; 146 (HIGH) 10-06". */
    private static String formatSeries(List<LabResult> series) {
        int size = series.size();
        List<LabResult> recent = series.subList(Math.max(0, size - 4), size);
        StringBuilder line = new StringBuilder();
        for (int i = recent.size() - 1; i >= 0; i--) {
            LabResult r = recent.get(i);
            line.append(r.getValueRaw() == null ? "?" : r.getValueRaw());
            if (r.getUnitRaw() != null && !r.getUnitRaw().isBlank()) {
                line.append(' ').append(r.getUnitRaw());
            }
            if (r.getFlag() != null && r.getFlag() != ResultFlag.NORMAL) {
                line.append(" (").append(r.getFlag()).append(')');
            }
            if (r.getObservedAt() != null) {
                line.append(' ').append(r.getObservedAt().toLocalDate().format(SHORT));
            }
            if (i > 0) {
                line.append("; ");
            }
        }
        return line.toString();
    }

    private static String num(Double value) {
        return value == null ? "n/a" : String.format(Locale.US, "%.1f", value);
    }

    private static String numInt(Integer value) {
        return value == null ? "n/a" : value.toString();
    }
}
