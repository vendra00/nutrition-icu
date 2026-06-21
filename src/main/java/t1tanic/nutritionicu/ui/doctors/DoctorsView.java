package t1tanic.nutritionicu.ui.doctors;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.service.patient.DoctorService;

/** Doctors and the sectors they belong to. */
@Route(value = "doctors", layout = MainLayout.class)
@PageTitle("Doctors · ICU Nutrition")
@PermitAll
public class DoctorsView extends VerticalLayout {

    public DoctorsView(DoctorService doctorService) {
        setSizeFull();
        setPadding(true);
        add(new H2("Doctors"));

        Grid<Doctor> grid = new Grid<>(Doctor.class, false);
        grid.addColumn(Doctor::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Doctor::getSector).setHeader("Sector").setAutoWidth(true);
        grid.setItems(doctorService.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }
}
