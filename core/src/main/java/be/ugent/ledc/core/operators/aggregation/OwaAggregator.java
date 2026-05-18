package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.operators.quantifier.BasicQuantifier;
import be.ugent.ledc.core.operators.quantifier.Quantifier;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OwaAggregator extends QuantifiedAggregation implements WeightedAggregator<UnitScore> 
{
    public OwaAggregator(Quantifier quantifier)
    {
        super(quantifier);
    }
    
    public OwaAggregator()
    {
        this(BasicQuantifier.AVERAGE);
    }
    
    
    @Override
    public UnitScore weight(int i, int n)
    {
        return i == 1
            ? UnitScore.minus(
                getQuantifier().apply(UnitScore.fromFraction(i, n)),
                getQuantifier().apply(UnitScore.ZERO)
            )
            : UnitScore.minus(
                getQuantifier().apply(UnitScore.fromFraction(i, n)),
                getQuantifier().apply(UnitScore.fromFraction(i-1, n)
            ));
    }

    @Override
    public UnitScore aggregate(List<UnitScore> values)
    {
        //Sort the scores, high to low
        List<UnitScore> sortedValues = values
            .stream()
            .sorted(Comparator.<UnitScore>naturalOrder().reversed())
            .collect(Collectors.toList());
        
        double owa = 0.0;
        
        for(int i=0; i<values.size(); i++)
        {
            owa += sortedValues.get(i).getValue() * weight(i+1, sortedValues.size()).getValue();
        }
        
        return new UnitScore(owa);
        
    }
    
}
