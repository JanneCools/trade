package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

import java.math.BigDecimal;

public class RoundingCostFunction <T extends Number & Comparable<? super T>, C extends OrdinalContractor<T>> implements CostFunction<T> {

    private final C contractor;

    private final T minValue;
    private final T maxValue;

    public RoundingCostFunction(C contractor, Pair<T, T> minMaxValues) {
        this.contractor = contractor;
        this.minValue = minMaxValues.getFirst();
        this.maxValue = minMaxValues.getSecond();
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {

        if (originalValue == null || originalValue.compareTo(minValue) < 0 || originalValue.compareTo(maxValue) > 0)
            return 1;

        if (repairedValue == null)
            return Integer.MAX_VALUE;

        BigDecimal originalBD = new BigDecimal(originalValue.toString());

        BigDecimal remainder = originalBD.multiply(BigDecimal.valueOf(100)).remainder(BigDecimal.valueOf(100));
        if (remainder.compareTo(BigDecimal.ZERO) == 0 || remainder.compareTo(BigDecimal.valueOf(0.5)) == 0) {
            return 1;
        } else {
            return 2;
        }
    }
}
