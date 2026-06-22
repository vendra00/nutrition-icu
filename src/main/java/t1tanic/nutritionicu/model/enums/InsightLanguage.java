package t1tanic.nutritionicu.model.enums;

/** Language an AI insight is produced and stored in. Each language is cached/saved separately. */
public enum InsightLanguage {

    EN("English", "Respond entirely in English, including the section headings."),
    ES("Español (España)", "Responde íntegramente en español de España (castellano), "
            + "incluidos los títulos de sección, con terminología clínica adecuada.");

    private final String label;
    private final String instruction;

    InsightLanguage(String label, String instruction) {
        this.label = label;
        this.instruction = instruction;
    }

    /** Human-readable name for the selector. */
    public String label() {
        return label;
    }

    /** Directive added to the prompt telling the model which language to answer in. */
    public String instruction() {
        return instruction;
    }
}
