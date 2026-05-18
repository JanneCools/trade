package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

/**
 * An iterable cost function is characterized by the fact that, for a given
 * original or dirty value, one get request a navigator that iterates over repair values
 * in order of lowest cost first.
 * @author abronsel
 * @param <T> 
 */

public interface IterableCostFunction<T> extends CostFunction<T> {

    ForwardNavigator<T> getRepairNavigator(T originalValue, ValueIterator<T> permittedValues, DataObject originalObject) throws RepairException;

}
