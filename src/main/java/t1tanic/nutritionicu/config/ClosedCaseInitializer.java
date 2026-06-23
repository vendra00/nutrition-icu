package t1tanic.nutritionicu.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.PatientCase;
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.SofaBand;
import t1tanic.nutritionicu.repo.PatientCaseRepository;

/**
 * Seeds a library of ~100 anonymized, completed ICU nutrition cases into the {@link PatientCase} archive,
 * spread across real clinical archetypes (sepsis, trauma, pancreatitis, ARDS, refeeding, burns, etc.).
 * Each case carries de-identified features and a course narrative with the nutrition intervention, marker
 * trajectory and outcome — the comparison cohort the Insights "Compare" feature searches. Deterministic
 * (fixed seed) and idempotent: skips when synthetic cases already exist. Demo data; disable with
 * {@code app.sandbox.seed=false}.
 */
@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(name = "app.sandbox.seed", havingValue = "true", matchIfMissing = true)
public class ClosedCaseInitializer implements ApplicationRunner {

    private static final int TARGET = 100;

    /** One archetype per admission diagnosis (the unit's categories), with plausible ranges and course. */
    private static final List<Archetype> ARCHETYPES = List.of(
            new Archetype(AdmissionDiagnosis.TBI, "isolated traumatic brain injury",
                    "Early enteral feeding via NG tube, transitioned to PEG around day 10; 25 kcal/kg and "
                            + "1.5 g/kg protein; prokinetics for gastroparesis; tight glycaemic control; "
                            + "overfeeding avoided.",
                    18, 70, 22, 29, 4, 7, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 12, 32, 0.74,
                    false, false),
            new Archetype(AdmissionDiagnosis.TBI_POLYTRAUMA, "severe TBI with polytrauma",
                    "Early high-protein enteral feeding (2.0 g/kg) once resuscitated; energy advanced "
                            + "cautiously with raised intracranial pressure; multi-injury and transfusion support.",
                    18, 55, 22, 28, 5, 8, new SofaBand[]{SofaBand.GE_10}, 16, 40, 0.64,
                    false, false),
            new Archetype(AdmissionDiagnosis.ACUTE_SPINAL_CORD_INJURY, "acute traumatic spinal cord injury",
                    "Lower energy target (neurogenic reduction in expenditure) to avoid overfeeding; protein "
                            + "1.5-2.0 g/kg; aggressive bowel regimen for neurogenic ileus; early enteral feeding.",
                    18, 65, 21, 28, 4, 7, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 18, 45, 0.82,
                    false, false),
            new Archetype(AdmissionDiagnosis.POLYTRAUMA_NO_SEVERE_TBI, "polytrauma without severe head injury",
                    "Early enteral feeding from day 1; high-protein target (2.0 g/kg) reached day 4 with a "
                            + "standard polymeric formula; early mobilisation.",
                    18, 50, 21, 28, 3, 6, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 8, 24, 0.86,
                    false, false),
            new Archetype(AdmissionDiagnosis.MAJOR_BURNS, "major burns",
                    "Hypermetabolic course - very high energy (30-35 kcal/kg) and protein (2.0-2.2 g/kg) via "
                            + "early enteral feeding; trace-element and vitamin supplementation.",
                    20, 65, 22, 30, 5, 8, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 22, 50, 0.76,
                    false, false),
            new Archetype(AdmissionDiagnosis.SEPTIC_SHOCK, "septic shock",
                    "Enteral nutrition started day 2 at 15 kcal/kg, advanced to 25 kcal/kg and 1.3 g/kg "
                            + "protein by day 5; prokinetics for high gastric residuals; insulin for stress "
                            + "hyperglycaemia.",
                    50, 82, 22, 31, 6, 9, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 10, 28, 0.68,
                    false, false),
            new Archetype(AdmissionDiagnosis.ACUTE_RESPIRATORY_FAILURE, "acute respiratory failure / ARDS",
                    "Enteral feeding continued during prone positioning at 20 kcal/kg, protein 1.5 g/kg; "
                            + "permissive underfeeding the first week then full target; early rehabilitation.",
                    45, 80, 24, 34, 5, 8, new SofaBand[]{SofaBand.GE_10}, 14, 36, 0.64,
                    false, false),
            new Archetype(AdmissionDiagnosis.COMPLEX_POSTOP, "complex postoperative course",
                    "Feeding delayed by ileus/instability; trophic EN day 3, full target by day 6; "
                            + "supplemental parenteral nutrition days 4-7 to cover the energy deficit.",
                    50, 80, 22, 31, 4, 7, new SofaBand[]{SofaBand.LT_6, SofaBand.B6_9}, 8, 22, 0.84,
                    false, false),
            new Archetype(AdmissionDiagnosis.OTHER_NEUROLOGICAL, "other neurological emergency (stroke/encephalopathy)",
                    "Dysphagia - enteral feeding via nasogastric tube with aspiration precautions; later PEG "
                            + "or texture-modified oral trial; 25 kcal/kg, 1.3 g/kg protein.",
                    40, 82, 22, 29, 4, 7, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 10, 30, 0.70,
                    false, false),
            new Archetype(AdmissionDiagnosis.OTHER_MEDICAL, "other medical critical illness (e.g. pancreatitis, decompensation)",
                    "Cautious, malnutrition-aware enteral start advanced as tolerated; electrolytes monitored "
                            + "for refeeding; lipid-restricted formula when hypertriglyceridaemic.",
                    45, 85, 16, 30, 5, 9, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 8, 26, 0.66,
                    true, false));

    private final PatientCaseRepository caseRepository;

    public ClosedCaseInitializer(PatientCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (caseRepository.existsBySourcePatientIdIsNull()) {
            return;
        }
        Random rnd = new Random(20260622L); // deterministic so the demo archive is reproducible
        List<PatientCase> cases = new ArrayList<>(TARGET);
        for (int i = 0; i < TARGET; i++) {
            cases.add(generate(rnd, i + 1, ARCHETYPES.get(i % ARCHETYPES.size())));
        }
        caseRepository.saveAll(cases);
        log.info("Seeded {} anonymized closed cases into the comparison archive", cases.size());
    }

    private PatientCase generate(Random rnd, int seq, Archetype a) {
        int age = rangeInt(rnd, a.ageLo(), a.ageHi());
        Sex sex = rnd.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        double bmi = round1(rangeDouble(rnd, a.bmiLo(), a.bmiHi()));
        int nutricMax = 10;
        int nutric = rangeInt(rnd, a.nutricLo(), a.nutricHi());
        SofaBand sofa = a.sofas()[rnd.nextInt(a.sofas().length)];
        int los = rangeInt(rnd, a.losLo(), a.losHi());
        boolean survived = rnd.nextDouble() < a.survival();

        PatientCase c = new PatientCase();
        c.setCaseCode(String.format("CASE-S%03d", seq));
        c.setSourcePatientId(null);
        c.setAdmissionDiagnosis(a.diagnosis());
        c.setAgeYears(age);
        c.setSex(sex);
        c.setBmi(bmi);
        c.setNutricScore(nutric);
        c.setNutricMax(nutricMax);
        c.setHighRisk(nutric >= 5);
        c.setSofaOrdinal(sofa.ordinal());
        c.setSofaBand(sofa.name());
        c.setLengthOfStayDays(los);
        c.setDischarged(survived);
        c.setCourseText(narrative(rnd, a, age, sex, bmi, nutric, nutricMax, sofa, los, survived));
        return c;
    }

    private String narrative(Random rnd, Archetype a, int age, Sex sex, double bmi, int nutric, int nutricMax,
                             SofaBand sofa, int los, boolean survived) {
        double crpStart = rangeDouble(rnd, 16, 34);
        double crpEnd = survived ? rangeDouble(rnd, 3, 9) : rangeDouble(rnd, 18, 34);
        double albStart = rangeDouble(rnd, 1.7, 2.5);
        double albEnd = survived ? rangeDouble(rnd, 2.8, 3.4) : rangeDouble(rnd, 1.8, 2.4);
        int preStart = rangeInt(rnd, 6, 11);
        int preEnd = survived ? rangeInt(rnd, 14, 22) : rangeInt(rnd, 6, 11);
        double weightStart = round1(bmi * 2.89); // assumes ~1.70 m
        double weightEnd = round1(weightStart * (1 - rangeDouble(rnd, 0.04, 0.10)));

        StringBuilder sb = new StringBuilder();
        sb.append("Diagnosis: ").append(a.diagnosis().label()).append("; ");
        sb.append(String.format(Locale.US, "Age %d, %s; BMI %.1f; NUTRIC %d/%d (%s); SOFA %s; ",
                age, sex.name().toLowerCase(Locale.ROOT), bmi, nutric, nutricMax,
                nutric >= 5 ? "high risk" : "low risk", sofa.name()));
        sb.append(a.dx()).append(". ").append(a.intervention()).append(' ');
        sb.append(String.format(Locale.US,
                "CRP %.0f->%.0f mg/dL, albumin %.1f->%.1f g/dL, prealbumin %d->%d mg/dL; weight %.1f->%.1f kg.",
                crpStart, crpEnd, albStart, albEnd, preStart, preEnd, weightStart, weightEnd));
        if (a.refeeding()) {
            sb.append(String.format(Locale.US,
                    " Phosphate dipped to %.1f mg/dL on day 3 (refeeding), corrected with replacement.",
                    rangeDouble(rnd, 1.2, 1.9)));
        }
        if (a.hyperTrig()) {
            sb.append(String.format(Locale.US, " Triglycerides peaked at %d mg/dL (lipid-restricted feed).",
                    rangeInt(rnd, 350, 680)));
        }
        sb.append(survived
                ? " Outcome: recovered, discharged from ICU on day " + los + "."
                : " Outcome: died in ICU on day " + los + ".");
        return sb.toString();
    }

    private record Archetype(AdmissionDiagnosis diagnosis, String dx, String intervention, int ageLo, int ageHi,
                             double bmiLo, double bmiHi, int nutricLo, int nutricHi, SofaBand[] sofas,
                             int losLo, int losHi, double survival, boolean refeeding, boolean hyperTrig) {
    }

    private static int rangeInt(Random r, int lo, int hi) {
        return lo + r.nextInt(hi - lo + 1);
    }

    private static double rangeDouble(Random r, double lo, double hi) {
        return lo + r.nextDouble() * (hi - lo);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
