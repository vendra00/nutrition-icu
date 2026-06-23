package t1tanic.nutritionicu.ui.patients;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.patient.PatientService;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;

/** Edits a patient's admission and discharge (release) dates. */
public class PatientStayDialog extends Dialog {

    public PatientStayDialog(Patient patient, PatientService patientService, Runnable onSaved) {
        setHeaderTitle(I18n.t("stay.title") + " · " + patient.getFullName());
        setWidth("380px");

        DatePicker admission = new DatePicker(I18n.t("stay.admission"));
        DatePicker discharge = new DatePicker(I18n.t("stay.discharge"));
        UiFormat.dayMonthYear(admission);
        UiFormat.dayMonthYear(discharge);
        if (patient.getAdmissionDate() != null) {
            admission.setValue(patient.getAdmissionDate());
        }
        if (patient.getDischargeDate() != null) {
            discharge.setValue(patient.getDischargeDate());
        }

        FormLayout form = new FormLayout(admission, discharge);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button cancel = new Button(I18n.t("common.cancel"), e -> close());
        Button save = new Button(I18n.t("common.save"), e -> {
            patientService.updateStay(patient.getId(), admission.getValue(), discharge.getValue());
            onSaved.run();
            close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }
}
