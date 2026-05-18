package be.ugent.ledc.sigma.datastructures.values;

/**
 * A generic interface that allows to range over values.
 * This can be used to model domains over attributes as well as sets or ranges
 * of values that are permitted for some attribute during a repair step.
 * @author abronsel
 * @param <T> 
 */
public interface ValueIterator<T> extends Iterable<T>
{
    /**
     * Returns true if the given value is in the selection of this value iterator
     * @param value
     * @return 
     */
    boolean inSelection(T value);
    
    /**
     * Returns an estimate of the number of values of this value iterator.
     * @return 
     */
    long cardinality();
    
    /**
     * Returns true if there are no values to iterate over.
     * @return 
     */
    boolean isEmpty();
    
    NominalValueIterator<T> convert();

}
