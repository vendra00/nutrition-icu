package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.UI;

/** Static access to UI translations, for use in static helpers where {@code Component.getTranslation} isn't available. */
public final class I18n {

    private I18n() {
    }

    /** Translates a key using the current UI's locale; returns the key if no UI is bound. */
    public static String t(String key, Object... params) {
        UI ui = UI.getCurrent();
        return ui == null ? key : ui.getTranslation(key, params);
    }
}
