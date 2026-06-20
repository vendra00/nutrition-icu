package t1tanic.nutritionicu.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.dto.PatientOverview;
import t1tanic.nutritionicu.dto.PatientOverview.Anthropometry;
import t1tanic.nutritionicu.dto.PatientOverview.Identity;
import t1tanic.nutritionicu.dto.PatientOverview.Risk;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.TemperatureMeasurement;

@Service
public class PatientOverviewServiceImpl implements PatientOverviewService {

    private static final String DASH = "—";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US);

    private final PatientService patientService;
    private final NutritionService nutritionService;

    public PatientOverviewServiceImpl(PatientService patientService, NutritionService nutritionService) {
        this.patientService = patientService;
        this.nutritionService = nutritionService;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientOverview build(Long patientId) {
        Patient p = patientService.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
        NutritionMetrics m = nutritionService.metricsFor(p);
        Optional<TemperatureMeasurement> temp = nutritionService.latestTemperature(patientId);
        Optional<NutritionRiskAssessment> risk = nutritionService.latestRiskAssessment(patientId);

        Identity identity = new Identity(p.getMedicalRecordNumber(), p.getFullName(), p.getSex(),
                p.ageOn(LocalDate.now()), p.getBirthDate(), p.isMonitored(),
                p.getAdmissionDate(), p.getDischargeDate());
        Anthropometry anthropometry = new Anthropometry(p.getHeightCm(), p.getCurrentWeightKg(),
                p.getUsualWeightKg(), m.bmi(), m.idealBodyWeightKg(), m.adjustedBodyWeightKg(),
                m.weightLossPercent(),
                temp.map(TemperatureMeasurement::getTemperatureCelsius).orElse(null),
                temp.map(TemperatureMeasurement::getMeasuredOn).orElse(null));
        Risk riskSnapshot = new Risk(
                risk.map(NutritionRiskAssessment::getNutricScore).orElse(null),
                risk.map(NutritionRiskAssessment::getNutricMax).orElse(null),
                risk.map(NutritionRiskAssessment::isHighRisk).orElse(null),
                risk.map(NutritionRiskAssessment::getAssessedOn).orElse(null));
        return new PatientOverview(identity, anthropometry, riskSnapshot);
    }

    @Override
    public byte[] toPdf(PatientOverview overview) {
        Identity id = overview.identity();
        Anthropometry a = overview.anthropometry();
        Risk r = overview.risk();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font regular = new PDType1Font(FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(FontName.HELVETICA_BOLD);

            float left = 50;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                Cursor c = new Cursor(cs, left, page.getMediaBox().getHeight() - 50);

                c.text(bold, 18, "Patient overview");
                c.gap(4);
                c.text(regular, 9, "NHC " + nz(id.medicalRecordNumber()) + "    generated " + DATE.format(LocalDate.now()));
                c.gap(14);

                c.text(bold, 13, "Identity");
                c.kv(regular, bold, "Name", nz(id.fullName()));
                c.kv(regular, bold, "Sex", id.sex() == null ? DASH : id.sex().name());
                c.kv(regular, bold, "Age", id.ageYears() == null ? DASH : id.ageYears() + " yrs");
                c.kv(regular, bold, "Born", date(id.birthDate()));
                c.kv(regular, bold, "Monitored", id.monitored() ? "Yes" : "No");
                c.kv(regular, bold, "Admitted", date(id.admissionDate()));
                c.kv(regular, bold, "Discharged", date(id.dischargeDate()));
                c.gap(10);

                c.text(bold, 13, "Anthropometry & nutrition");
                c.kv(regular, bold, "Height", unit(a.heightCm(), "cm"));
                c.kv(regular, bold, "Current weight", unit(a.currentWeightKg(), "kg"));
                c.kv(regular, bold, "Usual weight", unit(a.usualWeightKg(), "kg"));
                c.kv(regular, bold, "BMI", num(a.bmi()));
                c.kv(regular, bold, "Ideal body weight", unit(a.idealBodyWeightKg(), "kg"));
                c.kv(regular, bold, "Adjusted body weight", unit(a.adjustedBodyWeightKg(), "kg"));
                c.kv(regular, bold, "Recent weight loss", a.weightLossPercent() == null ? DASH : num(a.weightLossPercent()) + " %");
                c.kv(regular, bold, "Temperature (latest)", temperature(a));
                c.gap(10);

                c.text(bold, 13, "Nutritional risk (NUTRIC)");
                if (r.present()) {
                    c.kv(regular, bold, "Score", r.nutricScore() + " / " + r.nutricMax());
                    c.kv(regular, bold, "Risk", Boolean.TRUE.equals(r.highRisk()) ? "High risk" : "Low risk");
                    c.kv(regular, bold, "Assessed", date(r.assessedOn()));
                } else {
                    c.kv(regular, bold, "Status", "No assessment recorded");
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render overview PDF", e);
        }
    }

    // --- formatting helpers (mirror UiFormat, but kept here so the service has no UI dependency) ---

    private static String nz(String s) {
        return s == null || s.isBlank() ? DASH : s;
    }

    private static String num(Double v) {
        return v == null ? DASH : String.format(Locale.US, "%.1f", v);
    }

    private static String unit(Double v, String unit) {
        return v == null ? DASH : String.format(Locale.US, "%.1f %s", v, unit);
    }

    private static String date(LocalDate d) {
        return d == null ? DASH : DATE.format(d);
    }

    private static String temperature(Anthropometry a) {
        if (a.latestTemperatureC() == null) {
            return DASH;
        }
        String value = String.format(Locale.US, "%.1f °C", a.latestTemperatureC());
        return a.latestTemperatureDate() == null ? value : value + " (" + DATE.format(a.latestTemperatureDate()) + ")";
    }

    /** Tracks the write position down the page and renders text / key-value rows. */
    private static final class Cursor {
        private final PDPageContentStream cs;
        private final float left;
        private float y;

        Cursor(PDPageContentStream cs, float left, float top) {
            this.cs = cs;
            this.left = left;
            this.y = top;
        }

        void gap(float h) {
            y -= h;
        }

        void text(PDType1Font font, float size, String s) throws IOException {
            draw(font, size, left, s);
            y -= size + 6;
        }

        void kv(PDType1Font regular, PDType1Font bold, String label, String value) throws IOException {
            draw(bold, 11, left, label);
            draw(regular, 11, left + 170, value);
            y -= 17;
        }

        private void draw(PDType1Font font, float size, float x, String s) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(pdfSafe(s));
            cs.endText();
        }

        /** Replaces characters the Standard-14 WinAnsi encoding can't represent (e.g. an em dash). */
        private static String pdfSafe(String s) {
            return s.replace('—', '-').replace('–', '-');
        }
    }
}
