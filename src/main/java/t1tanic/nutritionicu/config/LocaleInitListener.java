package t1tanic.nutritionicu.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/** Applies the per-session language preference to every UI as it initializes. */
@Component
public class LocaleInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().setLocale(LocalePreference.get(uiEvent.getUI().getSession())));
    }
}
