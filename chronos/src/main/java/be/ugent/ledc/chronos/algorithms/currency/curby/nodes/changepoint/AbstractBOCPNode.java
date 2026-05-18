package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

public abstract class AbstractBOCPNode<T> extends AgeNode<T>
{
    /**
     * The Hazard function that models the probability of a change at age t
     */
    private final Function<Integer, UnitScore> hazard;
    
    /**
     * Predictive posterior probabilities.
     */
    private final List<Double> posteriors;

    public AbstractBOCPNode(Function<Integer, UnitScore> hazard, String attribute)
    {
        super(attribute);
        this.hazard = hazard;
        this.posteriors = new ArrayList<>();
        getAgeMap().put(0, UnitScore.ONE);
    }

    
    @Override
    public String getType()
    {
        return "bocp";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return new HashSet<>();
    }

    @Override
    public void updateBelief()
    {
        //Get the previous age map
        TreeMap<Integer, UnitScore> prevAgeMap = getAgeMap();
        
        TreeMap<Integer, UnitScore> newAgeMap = new TreeMap<>();
        
        int t = prevAgeMap.size();
        
        double normFactor = 0;
        
        for(int len=0; len<t+1; len++)
        {
            //Change probability
            if(len == 0)
            {
                double changeProb = 0;

                for(int i=0;i<prevAgeMap.size();i++)
                {
                    changeProb += hazard.apply(i).getValue()
                        * posteriors.get(0)
                        * prevAgeMap.get(i).getValue();
                }

                newAgeMap.put(len, new UnitScore(changeProb));

                normFactor += changeProb;
            }
            else
            {
                double growthProb = hazard.apply(len).getComplement().getValue()
                *   posteriors.get(len)    
                *   prevAgeMap.get(len-1).getValue();

                newAgeMap.put(len, new UnitScore(growthProb));

                normFactor += growthProb;
            }
        }
            
        //normalize
        for(int i=0;i<newAgeMap.size();i++)
        {
            newAgeMap.put(i, new UnitScore(newAgeMap.get(i).getValue() / normFactor));
        }

        //Update
        getAgeMap().clear();
        getAgeMap().putAll(newAgeMap);
            
    }

    @Override
    public void updateState(DataObject current)
    {
        T nValue = (T)current.get(getAttribute());
        
        if(nValue != null)
            updatePosteriors(nValue);
        
    }

    public List<Double> getPosteriors() {
        return posteriors;
    }

    @Override
    public void clear() {
        super.clear();
        getAgeMap().put(0, UnitScore.ONE);
    }

    public abstract void updatePosteriors(T value);
}
