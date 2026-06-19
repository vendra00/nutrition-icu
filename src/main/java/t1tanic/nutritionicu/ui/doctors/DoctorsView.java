package t1tanic.nutritionicu.ui.doctors;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.repo.DoctorRepository;

/** Doctors and the sectors they belong to. */
@Route(value = "doctors", layout = MainLayout.class)
@PageTitle("Doctors · ICU Nutrition")
public class DoctorsView extends VerticalLayout {

    public DoctorsView(DoctorRepository doctorRepository) {
        setSizeFull();
        setPadding(true);
        add(new H2("Doctors"));

        Grid<Doctor> grid = new Grid<>(Doctor.class, false);
        grid.addColumn(Doctor::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Doctor::getSector).setHeader("Sector").setAutoWidth(true);
        grid.setItems(doctorRepository.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }
}
