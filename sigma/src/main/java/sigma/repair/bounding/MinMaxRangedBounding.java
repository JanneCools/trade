package sigma.repair.bounding;

import core.dataset.DataObject;
import core.datastructures.Interval;
import core.datastructures.Multiset;
import sigma.datastructures.contracts.OrdinalContractor;
import sigma.datastructures.values.OrdinalValueIterator;

import java.util.Set;

/**
 * Extension of the RangedBounding class, which uses a minimal and maximal value
 * to bound the domain of an attribute in case all observed values are missing.
 * @author tboeckli
 * @param <T>
 */
public class MinMaxRangedBounding<T extends Comparable<? super T>> extends RangedBounding<T> {

    private final T min;
    private final T max;

    public MinMaxRangedBounding(T min, T max, int offset, int cap) {
        super(offset, cap);
        this.min = min;
        this.max = max;
    }

    public MinMaxRangedBounding(T min, T max, int offset) {
        this(min, max, offset, 500);
    }

    public MinMaxRangedBounding(T min, T max) {
        this(min, max, 0,500);
    }

    @Override
    public OrdinalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, Set<T> hints) {

        if (bag.keySet().stream().allMatch(o -> o.get(a) == null)) {
            T low = contractor.get(min);
            T high = contractor.get(max);
            Interval<T> range = Interval.closed(low, high);
            return getPermittedValues(a, range, bag, contractor, hints);
        } else {
            return super.getPermittedValues(a, bag, contractor, hints);
        }

    }

}
