package core.operators.aggregation;

import core.operators.UnitScore;

public interface WeightedAggregator<T extends Comparable<? super T>> extends Aggregator<T>
{
    public UnitScore weight(int i, int n);
}
