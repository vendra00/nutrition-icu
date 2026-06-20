package t1tanic.nutritionicu.ui.patients;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import t1tanic.nutritionicu.dto.PatientDetails;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.service.PatientService;

/**
 * Add/edit dialog for a patient's demographic and administrative details. Pass {@code null} as the
 * patient to create a new one; pass an existing patient to edit it. Anthropometry (height/weight) is
 * edited in the Nutrition tab and the stay window in {@link PatientStayDialog}, so neither appears here.
 */
public class PatientEditor extends Dialog {

    private final TextField nhc = new TextField("Medical record number (NHC)");
    private final TextField fullName = new TextField("Full name");
    private final DatePicker birthDate = new DatePicker("Birth date");
    private final ComboBox<Sex> sex = new ComboBox<>("Sex");
    private final TextField healthCardId = new TextField("Health-card id (CIP)");
    private final TextField socialSecurityNumber = new TextField("Social security number");
    private final Checkbox monitored = new Checkbox("Actively monitored");

    public PatientEditor(Patient patient, PatientService patientService, Runnable onSaved) {
        boolean creating = patient == null;
        setHeaderTitle(creating ? "New patient" : "Edit · " + patient.getFullName());
        setWidth("460px");

        nhc.setRequiredIndicatorVisible(true);
        sex.setItems(Sex.values());
        sex.setItemLabelGenerator(PatientEditor::sexLabel);
        sex.setValue(Sex.UNKNOWN);

        if (!creating) {
            nhc.setValue(nullToEmpty(patient.getMedicalRecordNumber()));
            fullName.setValue(nullToEmpty(patient.getFullName()));
            birthDate.setValue(patient.getBirthDate());
            sex.setValue(patient.getSex() == null ? Sex.UNKNOWN : patient.getSex());
            healthCardId.setValue(nullToEmpty(patient.getHealthCardId()));
            socialSecurityNumber.setValue(nullToEmpty(patient.getSocialSecurityNumber()));
            monitored.setValue(patient.isMonitored());
        }

        FormLayout form = new FormLayout(
                nhc, fullName, birthDate, sex, healthCardId, socialSecurityNumber, monitored);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("320px", 2));
        form.setColspan(monitored, 2);
        add(form);

        Button cancel = new Button("Cancel", e -> close());
        Button save = new Button("Save", e -> save(patient, patientService, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private void save(Patient patient, PatientService patientService, Runnable onSaved) {
        PatientDetails details = new PatientDetails(
                nhc.getValue(),
                emptyToNull(fullName.getValue()),
                birthDate.getValue(),
                sex.getValue(),
                emptyToNull(healthCardId.getValue()),
                emptyToNull(socialSecurityNumber.getValue()),
                monitored.getValue());
        try {
            if (patient == null) {
                patientService.create(details);
            } else {
                patientService.updateDetails(patient.getId(), details);
            }
        } catch (IllegalArgumentException ex) {
            Notification error = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        onSaved.run();
        close();
    }

    private static String sexLabel(Sex s) {
        return switch (s) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case UNKNOWN -> "Unknown";
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
