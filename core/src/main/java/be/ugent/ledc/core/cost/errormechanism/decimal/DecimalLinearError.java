package be.ugent.ledc.core.cost.errormechanism.decimal;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecimalLinearError implements ErrorMechanism<BigDecimal> {

    private final BigDecimal a;
    private final BigDecimal b;
    private final BigDecimal c;
    private final int scale;
    private final RoundingMode roundingMode;

    public DecimalLinearError(BigDecimal a, BigDecimal b, BigDecimal c, int scale, RoundingMode roundingMode) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.scale = scale;
        this.roundingMode = roundingMode;
        if(Objects.equals(c, new BigDecimal(0)))
            throw new RepairRuntimeException("A linear error cannot have c=0. Division by zero.");
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, int scale, RoundingMode roundingMode) {
        this(a,b,new BigDecimal(1), scale, roundingMode);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, BigDecimal c, RoundingMode roundingMode) {
        this(a,b,c,0,roundingMode);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, BigDecimal c, int scale) {
        this(a,b,c,scale,RoundingMode.HALF_UP);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, BigDecimal c) {
        this(a,b,c,0,RoundingMode.HALF_UP);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, int scale) {
        this(a,b,new BigDecimal(1),scale,RoundingMode.HALF_UP);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b, RoundingMode roundingMode) {
        this(a,b,new BigDecimal(1),0,roundingMode);
    }

    public DecimalLinearError(BigDecimal a, BigDecimal b) {
        this(a,b,new BigDecimal("1"),0,RoundingMode.HALF_UP);
    }

    @Override
    public boolean explains(BigDecimal originalValue, BigDecimal repairedValue, DataObject originalObject) {
        return originalValue != null &&
                a.add((originalValue.multiply(b).divide(c, scale, roundingMode))).equals(repairedValue);
    }

    @Override
    public List<BigDecimal> explanations(BigDecimal originalValue, DataObject originalObject) {
        return originalValue == null
                ? new ArrayList<>()
                : Stream.of(a.add((originalValue.multiply(b).divide(c,scale,roundingMode)))).collect(Collectors.toList());
    }

}
