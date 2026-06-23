package t1tanic.nutritionicu.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.exception.VaadinUiErrorHandler;

/** Installs the friendly {@link VaadinUiErrorHandler} on every Vaadin session as it starts. */
@Component
public class VaadinErrorInitListener implements VaadinServiceInitListener {

    private final transient VaadinUiErrorHandler errorHandler = new VaadinUiErrorHandler();

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(sessionInit ->
                sessionInit.getSession().setErrorHandler(errorHandler));
    }
}
