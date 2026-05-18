package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.poisson;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.AbstractBOCPNode;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A BOCP node in which we assume:
 * (1) a Poisson distribution with parameter lambda as the generating model
 * (2) a prior model Gamma (a,b) for the parameter lambda
 * 
 * @author abronsel
 */
public class PoissonBOCP extends AbstractBOCPNode<Integer>
{
    /**
     * The first parameter of the prior Gamma(a,b)
     */
    private final double a;
    
    /**
     * The first parameter of the prior Gamma(a,b)
     */
    private final double b;

    private final List<Double> aPosterior;

    public PoissonBOCP(double a, double b, Function<Integer, UnitScore> hazard, String attribute)
    {
        super(hazard, attribute);
        this.a = a;
        this.b = b;
        this.aPosterior = new ArrayList<>();
    }
    
    @Override
    public void updatePosteriors(Integer value)
    {
        //Update parameters for different run lengths
        
        //Insert prior value at length 0 (When no data: use the prior only)
        aPosterior.add(0, a);
        
        //Update other parameter values with new data value
        for(int t=1;t<aPosterior.size(); t++)
        {
            aPosterior.set(t, aPosterior.get(t) + value);
        }
        
        //Update the posteriors, depending on the assumed run length
        getPosteriors().clear();
        
        for(int len=0; len<aPosterior.size(); len++)
        {
            double postA = aPosterior.get(len);
            double postB = (len + b) / (len + b + 1);
            
            //The posterior predictive is a Negative binomial with params postA and postB
            double prob = Math.pow(1-postB, len) * Math.pow(postB, postA) * gamma(len+postA)
                /
                    (gamma(postA) * Statistics.factorial(len));
            
            getPosteriors().add(prob);
        }
    }
    
    private double gamma(double x)
    {
      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
      double ser = 1.0 + 76.18009173     / (x + 0)   - 86.50532033    / (x + 1)
                        + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
                        +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);

      double log = tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
      
      return Math.exp(log);
   }
    
}
