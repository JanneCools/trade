package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.dependency;

import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Conjunction<T> extends AgeNode<T>
{
    private final Set<AgeNode<?>> parents;

    public Conjunction(Set<AgeNode<?>> parents, String attribute) {
        super(attribute);
        this.parents = parents;
    }

    @Override
    public String getType() {
        return "change-conjunction";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return parents
            .stream()
            .map(n->n)
            .collect(Collectors.toSet());
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
            getAgeMap().clear();
            
            int maxAge = parents
                .stream()
                .mapToInt(p -> p.belief().support().stream().mapToInt(i->i).max().orElse(0))
                .max()
                .orElse(0);
            
            UnitScore running = UnitScore.ONE;
            
            Map<String, UnitScore> tailMap = parents
                    .stream()
                    .collect(Collectors.toMap(
                        p -> p.getAttribute(),
                        p -> UnitScore.ONE));
            
            for(int i=0;i<=maxAge;i++)
            {
                for(AgeNode<?> parent: parents)
                {
                    tailMap.merge(
                        parent.getAttribute(),
                        parent.belief().probability(i),
                        UnitScore::minus);
                }
                
                UnitScore prod = tailMap.values().stream().reduce(UnitScore.ONE,UnitScore::times);
                
                getAgeMap().put(i, UnitScore.minus(running, prod));
                
                running = new UnitScore(prod.getValue());
            }
        }
    }
}
