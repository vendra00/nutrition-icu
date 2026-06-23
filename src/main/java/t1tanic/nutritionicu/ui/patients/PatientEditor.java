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
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.service.patient.PatientService;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;

/**
 * Add/edit dialog for a patient's demographic and administrative details. Pass {@code null} as the
 * patient to create a new one; pass an existing patient to edit it. Anthropometry (height/weight) is
 * edited in the Nutrition tab and the stay window in {@link PatientStayDialog}, so neither appears here.
 */
public class PatientEditor extends Dialog {

    private final TextField nhc = new TextField(I18n.t("editor.nhc"));
    private final TextField fullName = new TextField(I18n.t("editor.fullname"));
    private final DatePicker birthDate = new DatePicker(I18n.t("editor.birthdate"));
    private final ComboBox<Sex> sex = new ComboBox<>(I18n.t("editor.sex"));
    private final TextField healthCardId = new TextField(I18n.t("editor.cip"));
    private final TextField socialSecurityNumber = new TextField(I18n.t("editor.ssn"));
    private final ComboBox<AdmissionDiagnosis> admissionDiagnosis = new ComboBox<>(I18n.t("editor.diagnosis"));
    private final Checkbox monitored = new Checkbox(I18n.t("editor.monitored"));

    public PatientEditor(Patient patient, PatientService patientService, Runnable onSaved) {
        boolean creating = patient == null;
        setHeaderTitle(creating ? I18n.t("editor.new") : I18n.t("editor.edit") + " · " + patient.getFullName());
        setWidth("460px");

        nhc.setRequiredIndicatorVisible(true);
        UiFormat.dayMonthYear(birthDate);
        sex.setItems(Sex.values());
        sex.setItemLabelGenerator(s -> I18n.t("sex." + s.name()));
        sex.setValue(Sex.UNKNOWN);
        admissionDiagnosis.setItems(AdmissionDiagnosis.values());
        admissionDiagnosis.setItemLabelGenerator(d -> I18n.t("diagnosis." + d.name()));
        admissionDiagnosis.setClearButtonVisible(true);

        if (!creating) {
            nhc.setValue(nullToEmpty(patient.getMedicalRecordNumber()));
            fullName.setValue(nullToEmpty(patient.getFullName()));
            birthDate.setValue(patient.getBirthDate());
            sex.setValue(patient.getSex() == null ? Sex.UNKNOWN : patient.getSex());
            healthCardId.setValue(nullToEmpty(patient.getHealthCardId()));
            socialSecurityNumber.setValue(nullToEmpty(patient.getSocialSecurityNumber()));
            admissionDiagnosis.setValue(patient.getAdmissionDiagnosis());
            monitored.setValue(patient.isMonitored());
        }

        FormLayout form = new FormLayout(
                nhc, fullName, birthDate, sex, healthCardId, socialSecurityNumber, admissionDiagnosis, monitored);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("320px", 2));
        form.setColspan(monitored, 2);
        add(form);

        Button cancel = new Button(I18n.t("common.cancel"), e -> close());
        Button save = new Button(I18n.t("common.save"), e -> save(patient, patientService, onSaved));
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
                admissionDiagnosis.getValue(),
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
