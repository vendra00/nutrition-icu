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

    /** Clinical archetypes with plausible ranges, the typical nutrition course, and survival likelihood. */
    private static final List<Archetype> ARCHETYPES = List.of(
            new Archetype("septic shock (intra-abdominal source)",
                    "Enteral nutrition started day 2 at 15 kcal/kg, advanced to 25 kcal/kg and 1.3 g/kg "
                            + "protein by day 5; prokinetics for high gastric residuals; insulin for stress "
                            + "hyperglycaemia.",
                    55, 82, 22, 31, 6, 9, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 10, 28, 0.70,
                    false, false),
            new Archetype("polytrauma",
                    "Early enteral feeding from day 1; high-protein target (2.0 g/kg) reached day 4 with a "
                            + "standard polymeric formula; aggressive mobilisation.",
                    18, 46, 21, 28, 3, 6, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 7, 22, 0.86,
                    false, false),
            new Archetype("major gastrointestinal surgery",
                    "Feeding delayed by post-operative ileus; trophic EN day 3, full target by day 6; "
                            + "supplemental parenteral nutrition days 4–7 to cover the energy deficit.",
                    50, 78, 21, 30, 4, 7, new SofaBand[]{SofaBand.LT_6, SofaBand.B6_9}, 6, 18, 0.88,
                    false, false),
            new Archetype("severe acute pancreatitis",
                    "Nasojejunal enteral feeding from day 2 with a lipid-restricted formula; advanced as "
                            + "tolerated; glycaemic control optimised.",
                    40, 66, 27, 36, 5, 8, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 12, 30, 0.76,
                    false, true),
            new Archetype("ARDS / acute respiratory failure",
                    "EN continued during prone positioning at 20 kcal/kg, protein 1.5 g/kg; permissive "
                            + "underfeeding the first week then full target; early rehabilitation.",
                    45, 76, 24, 34, 5, 8, new SofaBand[]{SofaBand.GE_10}, 14, 36, 0.64,
                    false, false),
            new Archetype("refeeding syndrome on severe malnutrition",
                    "Cautious start at 10 kcal/kg with phosphate, potassium and magnesium replacement plus "
                            + "thiamine; energy advanced over 5 days under close monitoring.",
                    52, 84, 14, 18, 6, 9, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 10, 26, 0.69,
                    true, false),
            new Archetype("major burns",
                    "Hypermetabolic course — very high energy (30–35 kcal/kg) and protein (2.0–2.2 g/kg) via "
                            + "early enteral feeding; trace-element and vitamin supplementation.",
                    25, 60, 22, 30, 5, 8, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 20, 46, 0.78,
                    false, false),
            new Archetype("aspiration pneumonia with frailty",
                    "Dysphagia — enteral feeding via nasogastric tube; protein-energy supplements; later "
                            + "texture-modified oral trial.",
                    75, 92, 17, 24, 6, 9, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 8, 20, 0.55,
                    false, false),
            new Archetype("cardiogenic shock after cardiac surgery",
                    "Feeding delayed by haemodynamic instability; trophic EN once stable on day 3; "
                            + "fluid-restricted energy-dense formula.",
                    60, 83, 24, 33, 5, 8, new SofaBand[]{SofaBand.GE_10}, 8, 24, 0.68,
                    false, false),
            new Archetype("traumatic brain injury",
                    "Early enteral feeding via NG tube, transitioned to PEG on day 10; 25 kcal/kg and "
                            + "1.5 g/kg protein; prokinetics for gastroparesis.",
                    35, 72, 23, 30, 4, 7, new SofaBand[]{SofaBand.B6_9, SofaBand.GE_10}, 12, 32, 0.72,
                    false, false));

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

    private record Archetype(String dx, String intervention, int ageLo, int ageHi, double bmiLo, double bmiHi,
                             int nutricLo, int nutricHi, SofaBand[] sofas, int losLo, int losHi,
                             double survival, boolean refeeding, boolean hyperTrig) {
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
