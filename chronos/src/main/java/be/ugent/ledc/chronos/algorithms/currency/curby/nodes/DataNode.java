package be.ugent.ledc.chronos.algorithms.currency.curby.nodes;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.ProbabilityTable;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataNode extends Node<String>
{
    private final ProbabilityTable<String> prior;
    
    private ProbabilityTable<String> belief;
    
    private String value;

    public DataNode(String attribute, ProbabilityTable<String> prior) {
        super(attribute);
        this.prior = prior;
    }

    @Override
    public String getType()
    {
        return "data";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return new HashSet<>();
    }

    @Override
    public ProbabilityTable<String> belief()
    {
        return belief;
    }

    @Override
    public void updateState(DataObject current)
    {
        if(current.getString(getAttribute()) != null)
            value = current.getString(getAttribute());
    }

    @Override
    public void updateBelief()
    {
        belief = value == null
            ? prior
            : new ProbabilityTable<>(Stream.of(value)
                .collect(Collectors.toMap(
                    v->v,
                    v->UnitScore.ONE))
            );
    }
    
    @Override
    public void clear()
    {
        this.belief = null;
        this.value = null;
    }
}
