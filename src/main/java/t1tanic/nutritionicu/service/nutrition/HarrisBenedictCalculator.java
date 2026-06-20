package t1tanic.nutritionicu.service.nutrition;

import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.model.enums.WeightClass;

/**
 * Harris-Benedict resting energy expenditure, faithfully reproducing the
 * <a href="https://www.rccc.eu/nutri/HB.html">rccc.eu</a> calculator.
 *
 * <p>Basal expenditure (GEB) uses the 1919 sex-specific equations; in obesity the weight fed to the
 * equation is pulled toward an averaged ideal body weight (25% correction for BMI 30–40, 50% above
 * 40). Total expenditure (GET) is {@code GEB × (0.1 + stress factor)} — the 0.1 is the source
 * calculator's fixed activity term.
 *
 * <p>Note: the source uses {@code 6.775} as the male age coefficient (canonical Harris-Benedict is
 * {@code 6.755}); reproduced here to match the reference calculator.
 */
@Component
public class HarrisBenedictCalculator {

    /**
     * @param sex      MALE or FEMALE (UNKNOWN is rejected)
     * @param weightKg actual body weight, kg (&gt; 0)
     * @param heightCm height, cm (&gt; 0)
     * @param ageYears age in whole years (&gt; 0)
     * @param stress   clinical stress degree
     */
    public EnergyExpenditureResult calculate(Sex sex, double weightKg, double heightCm,
                                             int ageYears, StressFactor stress) {
        if (sex == null || sex == Sex.UNKNOWN) {
            throw new IllegalArgumentException("Sex must be MALE or FEMALE");
        }
        if (weightKg <= 0 || heightCm <= 0 || ageYears <= 0) {
            throw new IllegalArgumentException("Weight, height and age must be positive");
        }

        double meters = heightCm / 100.0;
        double bmi = round1(weightKg / (meters * meters));

        double idealKg = averagedIdealWeight(sex, heightCm);
        WeightClass weightClass = WeightClass.forBmi(bmi);
        double weightUsed = weightClass.adjustmentConstant() == null
                ? weightKg
                : Math.round(idealKg + (weightKg - idealKg) * weightClass.adjustmentConstant());

        long basal = Math.round(basalExpenditure(sex, weightUsed, heightCm, ageYears));
        long total = Math.round(basal * (0.1 + stress.factor()));
        double kcalPerKg = round1((double) total / weightKg);

        return new EnergyExpenditureResult(bmi, weightClass, round1(idealKg), weightUsed,
                (int) basal, (int) total, kcalPerKg, stress);
    }

    /** The 1919 Harris-Benedict basal equations. */
    private static double basalExpenditure(Sex sex, double weightKg, double heightCm, int ageYears) {
        return sex == Sex.MALE
                ? 66.5 + 13.75 * weightKg + 5.003 * heightCm - 6.775 * ageYears
                : 655.1 + 9.563 * weightKg + 1.85 * heightCm - 4.676 * ageYears;
    }

    /** Mean of Devine, Robinson, Miller, Hamwi and the BMI-22 ideal weights (rccc.eu "PI"). */
    private static double averagedIdealWeight(Sex sex, double heightCm) {
        double overShort = heightCm - 152.4; // cm above 5 ft
        double bmi22 = 22 * (heightCm / 100.0) * (heightCm / 100.0);
        double devine;
        double robinson;
        double miller;
        double hamwi;
        if (sex == Sex.MALE) {
            devine = overShort * 0.91 + 50;
            robinson = overShort * 0.748 + 52;
            miller = overShort * 0.555 + 56.2;
            hamwi = overShort * 1.063 + 48.2;
        } else {
            devine = overShort * 0.91 + 45.5;
            robinson = overShort * 0.669 + 49;
            miller = overShort * 0.5354 + 53.1;
            hamwi = overShort * 0.866 + 45.5;
        }
        return (devine + robinson + miller + hamwi + bmi22) / 5;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
