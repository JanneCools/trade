package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint.poisson.PoissonCusum;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.util.SetOperations;
import java.util.Set;
import java.util.TreeMap;

/**
 * An age node that draws information from a parent CUSUM node to model the
 * belief of change.
 * @author abronsel
 */
public class CUSUMAge extends AgeNode<Integer>
{
    private final AbstractCUSUMNode cusumParent;

    public CUSUMAge(AbstractCUSUMNode cusumParent, String attribute)
    {
        super(attribute);
        this.cusumParent = cusumParent;
    }
    
    @Override
    public String getType()
    {
        return "cusum-age";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return SetOperations.set(cusumParent);
    }
    
    @Override
    public void updateBelief()
    {
        if(getAgeMap().isEmpty())
        {
            getAgeMap().put(0, UnitScore.ONE);
            return;
        }

        //Shift previous ages
        TreeMap<Integer, UnitScore> updatedAgeMap = shift();
        
        updatedAgeMap.put(0, UnitScore.ZERO);
        
        UnitScore pCumul = updatedAgeMap
            .entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(getAge()))
            .map(e -> e.getValue())
            .reduce(UnitScore.ZERO, UnitScore::plus);
        
        UnitScore nCumul = cusumParent
            .belief()
            .probability(PoissonCusum.ALTERNATIVE);
        
        if(nCumul.compareTo(pCumul) > 0)
        {
            updatedAgeMap.put(0, UnitScore.minus(nCumul, pCumul));
            int age = getAge();
            
            updatedAgeMap.put(
                age,
                UnitScore.minus(
                    updatedAgeMap.get(age),
                    updatedAgeMap.get(0)
                )
            );
        }
        
//        System.out.println(updatedAgeMap);
        
        getAgeMap().clear();
        getAgeMap().putAll(updatedAgeMap);
    }
}

