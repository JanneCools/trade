package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.normal;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.AbstractBOCPNode;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.statistics.ProbabilityDistributionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NormalBOCP extends AbstractBOCPNode<Double>
{
    private final double meanPrior;
    
    private final double varPrior;
    
    private final double variance;
    
    private final List<Double> posteriorMeans;

    public NormalBOCP(double meanPrior, double varPrior, double variance, Function<Integer, UnitScore> hazard, String attribute) {
        super(hazard, attribute);
        this.meanPrior = meanPrior;
        this.varPrior = varPrior;
        this.variance = variance;
        this.posteriorMeans = new ArrayList<>();
        this.posteriorMeans.add(meanPrior);
    }

    @Override
    public void updatePosteriors(Double value)
    {
        //insert prior mean at position 0
        posteriorMeans.add(0, meanPrior);

        //updates means from 1 to t
        for(int i=1; i<posteriorMeans.size(); i++)
        {
            double m = ((posteriorMeans.get(i) * iVar(i-1)) + value / variance) / iVar(i);
            posteriorMeans.set(i, m);
        }


        //            
        //Compute the posteriors, depending on the assumed run length
        //
        getPosteriors().clear();
        
        for(int len=0; len<posteriorMeans.size(); len++)
        {
            getPosteriors().add(
                ProbabilityDistributionFactory.<Double>createNormalDistribution
                (
                    posteriorMeans.get(len),              //Mean
                    Math.sqrt(variance + 1.0/iVar(len))     //Std. dev.
                )
                .probability(value)
            );
        }
    }

    @Override
    public void clear()
    {
        super.clear();
        this.posteriorMeans.clear();
        this.posteriorMeans.add(meanPrior);
    }
    
    
    
    /**
     * Computes the inverse of the variance at time t
     * @param t
     * @return 
     */
    private double iVar(int t)
    {
        return 1.0/varPrior + (t/variance);
    }
}
