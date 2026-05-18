package be.ugent.ledc.chronos.algorithms.currency.curby.nodes;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.ProbabilityTable;
import be.ugent.ledc.core.dataset.DataObject;
import java.util.Set;

/**
 * A generic interface for a INode in a Curby network.
 * @param <D> Type of data to which this node applies.
 */
public interface INode<D>
{
    /**
     * Type of belief node
     * @return 
     */
    public String getType();
    
    /**
     * Parents of the node in the network
     * @return 
     */
    public Set<INode<?>> getParents();
    
    /**
     * Queries the belief currently held by this node
     * @return 
     */
    public ProbabilityTable<D> belief();
    
    /**
     * Updates the node state based on new evidence
     * @param current 
     */
    public void updateState(DataObject current);
    
    /**
     * Updates the node belief
     */
    public void updateBelief();

    
    public void clear();
}
