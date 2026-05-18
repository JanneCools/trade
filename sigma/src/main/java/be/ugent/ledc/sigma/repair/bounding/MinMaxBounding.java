package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.values.OrdinalValueIterator;

import java.util.Set;

/**
 * Extension of the RangedBounding, which does not consider the observed values.
 * Instead, it samples values in a range between a given minimum and maximum value.
 * @author tboeckli
 * @param <T>
 */
public class MinMaxBounding<T extends Comparable<? super T>> extends RangedBounding<T> {

    private final T min;
    private final T max;

    public MinMaxBounding(T min, T max, int cap) {
        super(0, cap);
        this.min = min;
        this.max = max;
    }

    public MinMaxBounding(T min, T max) {
        this(min, max, 500);
    }

    @Override
    public OrdinalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, Set<T> hints) {
        T low = contractor.get(min);
        T high = contractor.get(max);
        Interval<T> range = Interval.closed(low, high);
        return getPermittedValues(a, range, bag, contractor, hints);
    }

}
