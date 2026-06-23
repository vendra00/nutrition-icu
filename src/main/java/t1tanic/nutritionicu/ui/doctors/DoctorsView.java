package t1tanic.nutritionicu.ui.doctors;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.service.patient.DoctorService;

/** Doctors and the sectors they belong to. */
@Route(value = "doctors", layout = MainLayout.class)
@PermitAll
public class DoctorsView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("doctors.title") + " · " + getTranslation("app.title");
    }

    public DoctorsView(DoctorService doctorService) {
        setSizeFull();
        setPadding(true);
        add(new H2(getTranslation("doctors.title")));

        Grid<Doctor> grid = new Grid<>(Doctor.class, false);
        grid.addColumn(Doctor::getName).setHeader(getTranslation("doctors.col.name")).setFlexGrow(2);
        grid.addColumn(d -> d.getSector() == null ? "" : getTranslation("sector." + d.getSector().name()))
                .setHeader(getTranslation("doctors.col.sector")).setAutoWidth(true);
        grid.setItems(doctorService.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }
}
