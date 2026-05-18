package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.shelflife;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.GeometricDistribution;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.DataNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.repair.cost.iterators.CrossProductIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A node that uses a Geometric prior, but the parameter is adapted based
 * on the data.
 * 
 * More precisely, a co-variate variable determines which values the parameter takes through
 * a simple generator function.
 * 
 * @author abronsel
 * @param <T>
 */
public class ConditionalShelfLife <T> extends AgeNode<T>
{        
    private final Function<DataObject,UnitScore> generator;
    
    private final Set<DataNode> parents;

    public ConditionalShelfLife(String attribute, Function<DataObject, UnitScore> generator, Set<DataNode> parents) {
        super(attribute);
        this.generator = generator;
        this.parents = parents;
    }
    
    public ConditionalShelfLife(String attribute, Function<DataObject, UnitScore> generator, DataNode... parents) {
        this(attribute, generator, SetOperations.set(parents));
    }
    
    public ConditionalShelfLife(String attribute, Map<DataObject, UnitScore> map, DataNode... parents) {
        this(attribute, (o) -> map.get(o), SetOperations.set(parents));
    }

    @Override
    public Set<INode<?>> getParents()
    {
        //A prior model has no parents
        return parents
            .stream()
            .map(n -> (INode<String>)n)
            .collect(Collectors.toSet());
    }

    @Override
    public String getType()
    {
        return "geometric-conditional";
    }

    @Override
    public void clear()
    {
        super.clear();
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
            
            UnitScore changeProb = UnitScore.ZERO;
            
            CrossProductIterator iterator = new CrossProductIterator(
                parents
                    .stream()
                    .collect(Collectors.toMap(
                        p -> p.getAttribute(),
                        p -> new ArrayList<>(p.belief().support()))
                    )
            );
            
            while(iterator.hasNext())
            {
                DataObject o = iterator.next();
                
                UnitScore prior = parents
                    .stream()
                    .map(p -> p.belief().probability(o.getString(p.getAttribute())))
                    .reduce(UnitScore.ONE, UnitScore::times);
                
                UnitScore prob = new GeometricDistribution(generator.apply(o)).probability(getAge());

                changeProb = UnitScore.plus(changeProb, UnitScore.times(prior, prob));
            }
            
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
}
