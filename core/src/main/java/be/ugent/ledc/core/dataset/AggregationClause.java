package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.operators.aggregation.Aggregator;

public class AggregationClause<C extends Comparable<? super C>>
{
    private final String inputAttribute;
    private final String outputAttribute;
    private final Aggregator<C> aggregator;

    public AggregationClause(String inputAttribute, String outputAttribute, Aggregator<C> aggregator)
    {
        this.inputAttribute = inputAttribute;
        this.outputAttribute = outputAttribute;
        this.aggregator = aggregator;
    }
    
    public AggregationClause(String inputAttribute, Aggregator<C> aggregator)
    {
        this(inputAttribute, inputAttribute, aggregator);
    }

    public String getInputAttribute()
    {
        return inputAttribute;
    }

    public String getOutputAttribute()
    {
        return outputAttribute;
    }

    public Aggregator<C> getAggregator()
    {
        return aggregator;
    }
    
    
}
