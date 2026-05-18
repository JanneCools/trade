package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.shelflife;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.IntDistribution;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * A node that models age based on a shelf life model. This model gives, for each
 * time, the probability that data becomes outdated at that exact time. Currency
 * can be derived by the tail distribution of that model.
 * @author abronsel
 * @param <T> 
 */
public class ShelfLife<T> extends AgeNode<T>
{   
    private final IntDistribution priorModel;

    public ShelfLife(String attribute, IntDistribution priorModel)
    {
        super(attribute);
        this.priorModel = priorModel;
    }

    @Override
    public Set<INode<?>> getParents()
    {
        //A prior model has no parents
        return new HashSet<>();
    }

    @Override
    public void updateBelief()
    {           
        //Was there a change at time t?
        if(getAge() == 0)
        {
            getAgeMap().clear();
            getAgeMap().put(0, UnitScore.ONE);
        }
        else
        {           
            TreeMap<Integer, UnitScore> updatedAgeMap = shift();
            
            UnitScore changeProb = getPriorModel().probability(getAge());
            
            updatedAgeMap.put(0, changeProb);
            updatedAgeMap.merge(
                getAge(),
                changeProb,
                UnitScore::minus
            );
            
            getAgeMap().clear();
            getAgeMap().putAll(updatedAgeMap);
        }
    }

    public IntDistribution getPriorModel()
    {
        return priorModel;
    }
    
    @Override
    public String getType()
    {
        return "shelf-life";
    }
}
