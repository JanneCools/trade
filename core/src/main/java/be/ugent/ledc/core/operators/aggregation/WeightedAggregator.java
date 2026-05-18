package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;

public interface WeightedAggregator<T extends Comparable<? super T>> extends Aggregator<T>
{
    public UnitScore weight(int i, int n);
}
