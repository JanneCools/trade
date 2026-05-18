package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.dependency;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.operators.aggregation.ExpectedQuantity;
import be.ugent.ledc.core.operators.aggregation.QuantifiedAggregation;
import be.ugent.ledc.core.operators.quantifier.BasicQuantifier;
import be.ugent.ledc.core.operators.quantifier.Quantifier;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Aggregator<T> extends AgeNode<T>
{
    private final Set<AgeNode<T>> parents;
    
    private final QuantifiedAggregation qAggregator;
      
    public Aggregator(String attribute, Set<AgeNode<T>> parents, Quantifier quantifier)
    {
        super(attribute);
        this.parents = parents;
        this.qAggregator = new ExpectedQuantity(quantifier);
        setAge(0);
    }
    
    public Aggregator(String attribute, Set<AgeNode<T>> parents, QuantifiedAggregation agg)
    {
        super(attribute);
        this.parents = parents;
        this.qAggregator = agg;
        setAge(0);
    }
    
    public Aggregator(String attribute, Set<AgeNode<T>> parents)
    {
        this(attribute, parents, BasicQuantifier.ALL);
    }

    @Override
    public String getType()
    {
        return "quantified-aggregator";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return parents
            .stream()
            .map(n -> (INode<?>)n)
            .collect(Collectors.toSet());
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
        
        //Present cumulative
        UnitScore currentCumul = updatedAgeMap
            .entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(getAge()))
            .map(e -> e.getValue())
            .reduce(UnitScore.ZERO, UnitScore::plus);
        
        //New cumulative
        UnitScore nextCumul = qAggregator.aggregate(
            parents
                .stream()
                .map(p -> p.currency().getComplement())
                .collect(Collectors.toList())
        );
        
        if(nextCumul.compareTo(currentCumul) > 0)
        {
            updatedAgeMap.put(0, UnitScore.minus(nextCumul, currentCumul));
            int age = getAge();
            
            updatedAgeMap.put(
                age,
                UnitScore.minus(
                    updatedAgeMap.get(age),
                    updatedAgeMap.get(0)
                )
            );
        }
        
        getAgeMap().clear();
        getAgeMap().putAll(updatedAgeMap);
    }
}