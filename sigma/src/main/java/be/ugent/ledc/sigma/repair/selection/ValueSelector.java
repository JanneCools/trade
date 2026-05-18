package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

/**
 * Selects, from a set of permitted values, a repair value by means of a certain
 * selection strategy.
 * @author abronsel
 * @param <T> Datatype for which this value selector applies
 */
public interface ValueSelector<T>
{
    public T selectValue(String attributeToTreat, DataObject dirtyOriginal, ValueIterator<T> permittedValues, DataObject repair);
}
