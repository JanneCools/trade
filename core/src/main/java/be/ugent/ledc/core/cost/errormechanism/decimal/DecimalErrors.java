package be.ugent.ledc.core.cost.errormechanism.decimal;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import be.ugent.ledc.core.cost.errormechanism.integer.IntegerErrors;

import java.math.BigDecimal;
import java.util.List;

public enum DecimalErrors implements ErrorMechanism<BigDecimal> {

    SIGN_ERROR(new DecimalSignError(), "DecimalSignError", "ds"),
    ROUNDING_ERROR(new DecimalRoundingError(), "DecimalRoundingError", "dr"),
    SINGLE_DIGIT(new DecimalNumberError(IntegerErrors.SINGLE_DIGIT), "DecimalSingleDigitError", "dsd"),
    ADJACENT_TRANSPOSITION(new DecimalNumberError(IntegerErrors.ADJACENT_TRANSPOSITION), "DecimalAdjacentTranspositionError", "dat");

    private final ErrorMechanism<BigDecimal> embeddedMechanism;
    private final String name;
    private final String shortName;

    DecimalErrors(ErrorMechanism<BigDecimal> embeddedMechanism, String name, String shortName) {
        this.embeddedMechanism = embeddedMechanism;
        this.name = name;
        this.shortName = shortName;
    }

    @Override
    public boolean explains(BigDecimal originalValue, BigDecimal repairedValue, DataObject originalObject) {
        return this.embeddedMechanism.explains(originalValue, repairedValue, originalObject);
    }

    @Override
    public List<BigDecimal> explanations(BigDecimal originalValue, DataObject originalObject) {
        return this.embeddedMechanism.explanations(originalValue, originalObject);
    }

    public ErrorMechanism<BigDecimal> getEmbeddedMechanism() {
        return embeddedMechanism;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

}
