package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.operators.quantifier.Quantifier;
import java.util.Arrays;
import java.util.List;

public class ExpectedQuantity extends QuantifiedAggregation
{

    public ExpectedQuantity(Quantifier quantifier)
    {
        super(quantifier);
    }

    @Override
    public UnitScore aggregate(List<UnitScore> values)
    {       
        int n = values.size();

        UnitScore output = UnitScore.ZERO;
        
//        for(int m=1;m<masks;m++)
//        {
//            BigInteger mask = new BigInteger(""+m);
//            
//            int quantity = 0;
//            
//            double prior = 1;
//            
//            //Compute prior and quantity
//            for(int b=0; b<values.size(); b++)
//            {
//                if(mask.testBit(b))
//                {
//                    quantity++;
//                    prior *= values.get(b).getValue();
//                }
//                else
//                {
//                    prior *= values.get(b).getComplement().getValue();
//                }
//            }
//            
//            output = UnitScore.plus(
//                output,
//                new UnitScore(prior * getQuantifier().apply(UnitScore.fromFraction(quantity, n)).getValue())
//            );
//        }

        //Keep two arrays
        UnitScore[] previous    = new UnitScore[n+1];
        UnitScore[] current     = new UnitScore[n+1];
        
        //Init
        for(int i=0;i<previous.length;i++)
            previous[i] = i==0?UnitScore.ONE:UnitScore.ZERO;
        
        for(int step=1;step<=n;step++)
        {
            //Update current
            for(int i=0;i<=n;i++)
            {
                UnitScore p = values.get(step-1);
                UnitScore q = p.getComplement();
                
                current[i] = UnitScore.times(previous[i], q);
                
                if(i>0)
                {
                    current[i] = UnitScore.plus(current[i], UnitScore.times(previous[i-1], p));
                }
            }
            
            //Update previous
            previous = Arrays.copyOf(current, n+1);
        }
        
        for(int q=0;q<=n;q++)
        {
            
            output = UnitScore.plus(
                output,
                UnitScore.times(current[q], getQuantifier().apply(UnitScore.fromFraction(q, n)))
            );
        }
        
        return output;
    }
    
}
