package t1tanic.nutritionicu.exception;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1tanic.nutritionicu.ui.common.I18n;

/**
 * Catches uncaught exceptions during Vaadin UI actions and shows the user a friendly error notification: the
 * exception's own message for our {@link ApplicationException}s (user-safe), or a generic localized message
 * (with a logged stack trace) for anything unexpected — instead of Vaadin's default internal-error overlay.
 */
public class VaadinUiErrorHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(VaadinUiErrorHandler.class);

    @Override
    public void error(ErrorEvent event) {
        Throwable cause = domainCause(event.getThrowable());
        String message;
        if (cause instanceof ApplicationException) {
            log.warn("Application error in UI: {}", cause.getMessage());
            message = cause.getMessage();
        } else {
            log.error("Unexpected error in UI", event.getThrowable());
            message = I18n.t("error.unexpected");
        }
        notifyUser(message);
    }

    private static void notifyUser(String message) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return; // outside a UI context (already logged)
        }
        ui.access(() -> {
            Notification notification = Notification.show(message, 6000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
    }

    /** The first {@link ApplicationException} in the cause chain (services may be proxied/wrapped), else the top. */
    private static Throwable domainCause(Throwable thrown) {
        for (Throwable t = thrown; t != null && t.getCause() != t; t = t.getCause()) {
            if (t instanceof ApplicationException) {
                return t;
            }
        }
        return thrown;
    }
}
