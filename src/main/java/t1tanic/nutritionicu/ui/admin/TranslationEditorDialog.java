package t1tanic.nutritionicu.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import t1tanic.nutritionicu.service.i18n.TranslationAdminService;

/**
 * Add or edit a single translation key with its English and Spanish values. Pass {@code null} to create a
 * new key (the key field is editable and checked for collisions); pass an existing row to edit (the key is
 * read-only). Saving upserts via {@link TranslationAdminService} and refreshes the live cache.
 */
public class TranslationEditorDialog extends Dialog {

    private final TextField key = new TextField();
    private final TextArea english = new TextArea();
    private final TextArea spanish = new TextArea();

    public TranslationEditorDialog(TranslationAdminService service,
                                   TranslationAdminService.Row existing,
                                   Runnable onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(getTranslation(creating ? "admin.tr.new.title" : "admin.tr.edit.title"));
        setWidth("640px");

        key.setLabel(getTranslation("admin.tr.key"));
        key.setWidthFull();
        key.setReadOnly(!creating);
        if (creating) {
            key.setHelperText(getTranslation("admin.tr.keyhelper"));
        }

        english.setLabel(getTranslation("admin.tr.english"));
        english.setWidthFull();
        spanish.setLabel(getTranslation("admin.tr.spanish"));
        spanish.setWidthFull();
        spanish.setHelperText(getTranslation("admin.tr.eshelper"));

        if (!creating) {
            key.setValue(existing.key());
            english.setValue(existing.english() == null ? "" : existing.english());
            spanish.setValue(existing.spanish() == null ? "" : existing.spanish());
        }

        FormLayout form = new FormLayout(key, english, spanish);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button cancel = new Button(getTranslation("common.cancel"), e -> close());
        Button save = new Button(getTranslation("common.save"), e -> save(service, creating, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private void save(TranslationAdminService service, boolean creating, Runnable onSaved) {
        String keyValue = key.getValue() == null ? "" : key.getValue().strip();
        if (keyValue.isBlank()) {
            error(getTranslation("admin.tr.err.key"));
            return;
        }
        if (english.getValue() == null || english.getValue().isBlank()) {
            error(getTranslation("admin.tr.err.en"));
            return;
        }
        if (creating && service.exists(keyValue)) {
            error(getTranslation("admin.tr.err.exists"));
            return;
        }
        service.save(keyValue, english.getValue(), spanish.getValue());
        Notification.show(getTranslation("admin.tr.saved", keyValue), 3000, Notification.Position.BOTTOM_START);
        onSaved.run();
        close();
    }

    private static void error(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
