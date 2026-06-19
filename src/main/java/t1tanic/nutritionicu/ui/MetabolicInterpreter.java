package t1tanic.nutritionicu.ui;

/**
 * Plain-language reading of a patient's metabolic phase and anabolic recovery from the inflammatory
 * and recovery {@link LabTrend}s.
 *
 * <p>Pure heuristics — given the trends, it returns the text and badge theme to show, with no UI
 * dependency — so the clinical reasoning can be unit tested without rendering anything. Decision
 * support only, not a diagnosis.
 */
final class MetabolicInterpreter {

    /** A reading rendered as badge text plus the Lumo theme variant that colours it. */
    record Interpretation(String text, String theme) {
    }

    /** Ebb (acute hyperinflammation) vs Flow (catabolism/recovery), read from CRP trend and PCT. */
    Interpretation phase(LabTrend crp, LabTrend pct) {
        if (!crp.hasData()) {
            return new Interpretation("Metabolic phase: insufficient CRP data", "contrast");
        }
        String text;
        String theme;
        if (crp.elevated() && crp.rising()) {
            text = "Ebb phase — acute inflammation rising (CRP %s). Avoid aggressive caloric load."
                    .formatted(crp.latestText());
            theme = "error";
        } else if (crp.elevated() && crp.falling()) {
            text = "Transition to Flow — inflammation elevated but settling (CRP %s)."
                    .formatted(crp.latestText());
            theme = "warning";
        } else if (crp.elevated()) {
            text = "Inflammation elevated (CRP %s); add a follow-up reading to establish the trend."
                    .formatted(crp.latestText());
            theme = "warning";
        } else {
            text = "Flow phase — low inflammation (CRP %s); anabolic window."
                    .formatted(crp.latestText());
            theme = "success";
        }
        if (pct.hasData() && pct.elevated()) {
            text += " Procalcitonin elevated (%s) — consider ongoing sepsis.".formatted(pct.latestText());
            theme = "error";
        }
        return new Interpretation("Metabolic phase: " + text, theme);
    }

    /** Prealbumin only reads as a nutrition trend once the acute-phase response is settling. */
    Interpretation recovery(LabTrend crp, LabTrend prealbumin) {
        if (!prealbumin.hasData()) {
            return new Interpretation("Anabolic recovery: no prealbumin data", "contrast");
        }
        boolean inflammationSettling = !crp.hasData() || !crp.elevated() || crp.falling();
        if (!inflammationSettling) {
            return new Interpretation("Anabolic recovery: prealbumin not interpretable while CRP is "
                    + "elevated/rising (acute-phase effect)", "contrast");
        }
        if (!prealbumin.hasTrend()) {
            return new Interpretation(("Anabolic recovery: single prealbumin reading (%s); needs another "
                    + "to read the trend").formatted(prealbumin.latestText()), "contrast");
        }
        if (prealbumin.rising()) {
            return new Interpretation(("Anabolic recovery: prealbumin rising (%s) as inflammation settles "
                    + "— recovery underway").formatted(prealbumin.latestText()), "success");
        }
        return new Interpretation(("Anabolic recovery: prealbumin not yet rising (%s) despite settling "
                + "inflammation").formatted(prealbumin.latestText()), "warning");
    }
}
