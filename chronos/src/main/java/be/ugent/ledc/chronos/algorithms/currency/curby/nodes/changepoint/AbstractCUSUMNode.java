package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.changepoint;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.ProbabilityTable;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.Node;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * An abstract node that implements the logic common in any scenario where
 * the Cumulative Sum (CUSUM) of log-ratios is used to detect change points.
 * @author abronsel
 */
public abstract class AbstractCUSUMNode extends Node<String>
{
    /**
     * A string symbol that represents the ground state.
     */
    public static final String GROUND       = "ground";
    
    /**
     * A string symbol that represents the alternative state.
     */
    public static final String ALTERNATIVE  = "alternative";

    /**
     * The belief in the ground state.
     */
    private UnitScore groundBelief;
    
    /**
     * A running variable to keep the CUSUM over time. 
     */
    private double cusum;
    
    /**
     * A parameter for the activation function that converts the CUSUM stat into
     * a probability.
     */
    private final double a;
    
    /**
     * A parameter for the activation function that converts the CUSUM stat into
     * a probability.
     */
    private final double b;
    
    public AbstractCUSUMNode(String attribute, double a, double b) throws ChronosException
    {
        super(attribute);
        
        this.cusum = 0.0;
        this.a = a;
        this.b = b;
    }
    
    public AbstractCUSUMNode(String attribute) throws ChronosException
    {
        this(attribute, 1.0, 5.0);
    }

    @Override
    public String getType()
    {
        return "cusum";
    }

    @Override
    public Set<INode<?>> getParents()
    {
        return new HashSet<>();
    }

    @Override
    public ProbabilityTable<String> belief()
    {
        return new ProbabilityTable<>(
            Stream
                .of(GROUND,ALTERNATIVE)
                .collect(Collectors.toMap(
                    s -> s,
                    s -> s.equals(GROUND)
                            ? groundBelief
                            : groundBelief.getComplement())
                )
        );
    }


    @Override
    public void updateBelief()
    {
        double prob = Math.tanh(Math.pow(cusum, a) / b);
        
        this.groundBelief = new UnitScore(prob).getComplement();
    }

    @Override
    public void clear()
    {
        this.cusum = 0.0;
    }

    public double getCusum()
    {
        return cusum;
    }

    public void setCusum(double cusum)
    {
        this.cusum = cusum;
    }
}