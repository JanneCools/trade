package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

/**
 * Cost function that computes the distance of the repair value to some expected value computer with a given function
 * @param <T>
 * @param <U>
 */
public class CalculationCostFunction <T extends Comparable<? super T>, U extends Comparable<? super U>> implements CostFunction<T> {

    String attribute;
    OrdinalContractor<T> contractor;
    List<String> dependentAttributes;
    List<OrdinalContractor<U>> dependentContractors;
    Function<List<U>,T> function;
    UnaryOperator<Long> updater;

    public CalculationCostFunction(
            String attribute, OrdinalContractor<T> contractor,
            List<String> dependentAttributes, List<OrdinalContractor<U>> dependentContractors,
            Function<List<U>, T> function) {
        this.attribute = attribute;
        this.contractor = contractor;
        this.dependentAttributes = dependentAttributes;
        this.dependentContractors = dependentContractors;
        this.function = function;
        this.updater = null;
    }

    public CalculationCostFunction(
            String attribute, OrdinalContractor<T> contractor,
            List<String> dependentAttributes, List<OrdinalContractor<U>> dependentContractors,
            Function<List<U>, T> function, UnaryOperator<Long> updater) {
        this.attribute = attribute;
        this.contractor = contractor;
        this.dependentAttributes = dependentAttributes;
        this.dependentContractors = dependentContractors;
        this.function = function;
        this.updater = updater;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        String suffix = "#curr";
        if (!contractor.getFromDataObject(originalObject, attribute+suffix).equals(originalValue)) {
            suffix = "#next";
        }

        // get value of dependent attribute for calculation
        String finalSuffix = suffix;
        List<U> otherValues = IntStream.range(0, dependentAttributes.size())
                .mapToObj(i -> dependentContractors.get(i).getFromDataObject(originalObject, dependentAttributes.get(i)+finalSuffix))
                .toList();
        T expectedValue = function.apply(otherValues);
        long distanceToTarget = expectedValue.compareTo(repairedValue) <= 0
                ? contractor.cardinality(new Interval<>(expectedValue, repairedValue))
                : contractor.cardinality(new Interval<>(repairedValue, expectedValue));

        if (updater!=null) {
            distanceToTarget = updater.apply(distanceToTarget);
        }

        return (int) distanceToTarget + 1;
    }
}
