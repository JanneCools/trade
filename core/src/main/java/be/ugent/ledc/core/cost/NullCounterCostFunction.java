package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.Objects;

/**
 * A special kind of constant cost model that estimates reliability of an object
 * by the amount of null values. The idea is that making changes in an object
 * with many null values, has lower cost than an object with few or no null value
 * @author abronsel
 * @param <T> 
 */
public class NullCounterCostFunction<T extends Comparable<? super T>> implements CostFunction<T>
{
    private final int amplifier;

    public NullCounterCostFunction(int amplifier)
    {
        this.amplifier = amplifier;
    }
    
    public NullCounterCostFunction()
    {
        this(1);
    }
    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject o)
    {
        return (int)Math.pow(
            o.getAttributes().size() - //Number of attributes minus
            (int) o
                .getAttributes()
                .stream()
                .filter(a -> o.get(a) == null) //Number of null values.
                .count(),
            amplifier
        );
    }

    public int getAmplifier()
    {
        return amplifier;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.amplifier;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NullCounterCostFunction<?> other = (NullCounterCostFunction<?>) obj;
        return this.amplifier == other.amplifier;
    }
    
}
