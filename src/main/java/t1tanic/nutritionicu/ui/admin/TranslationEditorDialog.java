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
 * Edit one translation key's English and Spanish values. The key is read-only (keys come from the code, not
 * the editor); saving upserts via {@link TranslationAdminService} and refreshes the live cache. English is
 * required; clearing Spanish makes the key fall back to English rather than removing it.
 */
public class TranslationEditorDialog extends Dialog {

    private final TextField key = new TextField();
    private final TextArea english = new TextArea();
    private final TextArea spanish = new TextArea();

    public TranslationEditorDialog(TranslationAdminService service,
                                   TranslationAdminService.Row row,
                                   Runnable onSaved) {
        setHeaderTitle(getTranslation("admin.tr.edit.title"));
        setWidth("640px");

        key.setLabel(getTranslation("admin.tr.key"));
        key.setWidthFull();
        key.setReadOnly(true);
        key.setValue(row.key());

        english.setLabel(getTranslation("admin.tr.english"));
        english.setWidthFull();
        english.setValue(row.english() == null ? "" : row.english());

        spanish.setLabel(getTranslation("admin.tr.spanish"));
        spanish.setWidthFull();
        spanish.setHelperText(getTranslation("admin.tr.eshelper"));
        spanish.setValue(row.spanish() == null ? "" : row.spanish());

        FormLayout form = new FormLayout(key, english, spanish);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button cancel = new Button(getTranslation("common.cancel"), e -> close());
        Button save = new Button(getTranslation("common.save"), e -> save(service, row.key(), onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private void save(TranslationAdminService service, String keyValue, Runnable onSaved) {
        if (english.getValue() == null || english.getValue().isBlank()) {
            error(getTranslation("admin.tr.err.en"));
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
