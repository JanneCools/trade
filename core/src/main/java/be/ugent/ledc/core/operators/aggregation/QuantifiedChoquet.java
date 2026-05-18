package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.operators.quantifier.Quantifier;
import java.util.Comparator;
import java.util.List;

public class QuantifiedChoquet extends QuantifiedAggregation
{
    public QuantifiedChoquet(Quantifier quantifier)
    {
        super(quantifier);
    }

    @Override
    public UnitScore aggregate(List<UnitScore> values)
    {
        values.sort(Comparator.reverseOrder());
        
        //Number of parents
        int n = values.size();
        
        UnitScore output = UnitScore.ZERO;
        
        for(int i=0;i<values.size();i++)
        {     
            UnitScore upper = getQuantifier().apply(UnitScore.fromFraction(i+1, n));
            UnitScore lower = getQuantifier().apply(UnitScore.fromFraction(i, n));
            
            UnitScore delta = UnitScore.minus(upper, lower);
            
            output = UnitScore.plus(output, UnitScore.times(values.get(i), delta));
            
        }
 
        return output;
    }
    
}
