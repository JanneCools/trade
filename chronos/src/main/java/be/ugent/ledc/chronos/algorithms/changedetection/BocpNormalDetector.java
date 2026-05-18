package be.ugent.ledc.chronos.algorithms.changedetection;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.statistics.ProbabilityDistributionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * A change point detector based on the Bayesian Online Change Point technique (BOCP).
 * 
 * Specifically, this detector assumes:
 
 (1) a normal likelihood with an unknown mean and fixed variance
 (2) a normal prior 
 
 * 
 * @author abronsel
 * @param <I>
 */
public class BocpNormalDetector <I extends Comparable<? super I>>  implements ChangePointDetector<I, Double>
{
    private final double meanPrior;
    
    private final double varPrior;
    
    private final double variance;
    
    private final Function<Integer, UnitScore> hazard;

    public BocpNormalDetector(double meanPrior, double varPrior, double variance, Function<Integer,UnitScore> hazard)
    {
        this.meanPrior = meanPrior;
        this.varPrior = varPrior;
        this.variance = variance;
        this.hazard = hazard;
    }
    
    @Override
    public List<I> changes(Signal<I, Double> signal) throws ChronosException
    {
        List<I> changePoints = new ArrayList<>();
        
        //Array to store previous run length (allows message passing)
        List<Double> messages = new ArrayList<>();
        messages.add(1.0);
        
        //Array to store new run length beliefs
        List<Double> runLengthBelief = new ArrayList<>();
        
        //This list keeps, at position i, the variance to use under the hypothesis
        //that the runlength is i
        List<Double> invVariances = new ArrayList<>();
        
        //This list keeps, at position i, the mean to use under the hypothesis
        //that the runlength is i
        List<Double> means = new ArrayList<>();
        
        //Init t at 0
        int t = 0;
        
        int pAge = 0;
        
        for(I idx: signal.indexSet())
        {
            //Get next value
            double v = signal.valueAt(idx);
            
            //Increase t
            t++;

            
            //
            //1. Update variances
            //
            
            //1a. insert prior variance at position 0
            invVariances.add(0, 1.0 / varPrior);
            
            //1b. for each position from 1 to t, add fixed variance
            IntStream
                .range(1, t)
                .forEach(i -> invVariances.set(i, invVariances.get(i) + 1/variance));
            
            //
            //2. Updates means
            //
            
            //2a. insert prior mean at position 0
            means.add(0, meanPrior);
            
            //2b. updates means from 1 to t
            for(int i=1; i<t; i++)
            {
                double m = (means.get(i) * invVariances.get(i-1) + v / variance) / invVariances.get(i);
                means.set(i, m);
            }
                
            
            //            
            //3. Compute the posteriors, depending on the assumed run length
            //
            
            List<Double> posterior = new ArrayList<>();
            for(int len=0; len<t; len++)
            {
                posterior.add(
                    ProbabilityDistributionFactory.<Double>createNormalDistribution(
                        means.get(len),                                 //Mean
                        Math.sqrt(variance + 1.0/invVariances.get(len)) //Std. dev.
                    ).probability(v)
                );
            }
            
            double sum = 0;
            
            //4. Compute new run length beliefs           
            for(int len=0; len<t+1; len++)
            {
                //Change probability
                if(len == 0)
                {
                    double changeProb = 0;
                    
                    for(int i=0;i<messages.size();i++)
                    {
                        changeProb += hazard.apply(i).getValue()
                            * posterior.get(0)
                            * messages.get(i);
                    }
                    
                    runLengthBelief.add(changeProb);
                    
                    sum += changeProb;
                }
                else
                {
                    double growthProb = hazard.apply(len).getComplement().getValue()
                    *   posterior.get(len)    
                    *   messages.get(len-1);
                    
                    runLengthBelief.add(growthProb);
                    
                    sum += growthProb;
                }
            }
            
            //normalize
            for(int i=0;i<runLengthBelief.size();i++)
            {
                runLengthBelief.set(i, runLengthBelief.get(i) / sum);
            }

//            System.out.println("t="+t + " -> " + runLengthBelief);
            
            //Decide
            int maxProbAge = 0;
            double maxProb = 0.0;
            
            for(int i=0;i<runLengthBelief.size();i++)
            {
                if(runLengthBelief.get(i) > maxProb)
                {
                    maxProb = runLengthBelief.get(i);
                    maxProbAge = i;
                }
            }
            
            if(maxProbAge < pAge)
            {
                changePoints.add(idx);
            }
            
            //Update previous age
            pAge = maxProbAge;
            
            //Pass on messages
            messages.clear();
            messages.addAll(runLengthBelief);
            runLengthBelief.clear();
        }
        
        return changePoints;
    }
}
