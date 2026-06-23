package t1tanic.nutritionicu.ui.nutrition;

import t1tanic.nutritionicu.ui.common.I18n;

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
            return new Interpretation(I18n.t("metabolic.phase.nodata"), "contrast");
        }
        String text;
        String theme;
        if (crp.elevated() && crp.rising()) {
            text = I18n.t("metabolic.phase.ebb", crp.latestText());
            theme = "error";
        } else if (crp.elevated() && crp.falling()) {
            text = I18n.t("metabolic.phase.transition", crp.latestText());
            theme = "warning";
        } else if (crp.elevated()) {
            text = I18n.t("metabolic.phase.elevated", crp.latestText());
            theme = "warning";
        } else {
            text = I18n.t("metabolic.phase.flow", crp.latestText());
            theme = "success";
        }
        if (pct.hasData() && pct.elevated()) {
            text += " " + I18n.t("metabolic.phase.pct", pct.latestText());
            theme = "error";
        }
        return new Interpretation(text, theme);
    }

    /** Prealbumin only reads as a nutrition trend once the acute-phase response is settling. */
    Interpretation recovery(LabTrend crp, LabTrend prealbumin) {
        if (!prealbumin.hasData()) {
            return new Interpretation(I18n.t("metabolic.recovery.nodata"), "contrast");
        }
        boolean inflammationSettling = !crp.hasData() || !crp.elevated() || crp.falling();
        if (!inflammationSettling) {
            return new Interpretation(I18n.t("metabolic.recovery.notinterpretable"), "contrast");
        }
        if (!prealbumin.hasTrend()) {
            return new Interpretation(I18n.t("metabolic.recovery.single", prealbumin.latestText()), "contrast");
        }
        if (prealbumin.rising()) {
            return new Interpretation(I18n.t("metabolic.recovery.rising", prealbumin.latestText()), "success");
        }
        return new Interpretation(I18n.t("metabolic.recovery.notrising", prealbumin.latestText()), "warning");
    }
}
