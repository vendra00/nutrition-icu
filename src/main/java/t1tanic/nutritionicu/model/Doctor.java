package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.Sector;

/**
 * A doctor who uses the system. Minimal for now — name and sector — to be
 * extended later (credentials, contact, link to requested reports, etc.).
 */
@Entity
@Table(name = "doctor")
@Getter
@Setter
@NoArgsConstructor
public class Doctor extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    /** Clinical sector the doctor works in, e.g. ICU, NEUROLOGY. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sector", nullable = false, length = 32)
    private Sector sector;

    public Doctor(String name, Sector sector) {
        this.name = name;
        this.sector = sector;
    }
}
