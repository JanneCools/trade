package be.ugent.ledc.chronos.datastructures;

import java.util.Objects;

/**
 * A class to model a change point in a signal. Changes happen from one index
 * to a subsequent one and involve two values: a previous value and a new current value.
 * @author abronsel
 * @param <I>
 * @param <T> 
 */
public class ChangePoint<I extends Comparable<? super I>, T> implements Comparable<ChangePoint<I,T>>
{
    private final I indexOfChange;
    private final I indexBeforeChange;
    private final T valueAtChange;
    private final T valueBeforeChange;

    public ChangePoint(I indexOfChange, I indexBeforeChange, T valueAtChange, T valueBeforeChange)
    {
        this.indexOfChange = indexOfChange;
        this.indexBeforeChange = indexBeforeChange;
        this.valueAtChange = valueAtChange;
        this.valueBeforeChange = valueBeforeChange;
    }

    public I getIndexOfChange()
    {
        return indexOfChange;
    }

    public I getIndexBeforeChange()
    {
        return indexBeforeChange;
    }

    public T getValueAtChange()
    {
        return valueAtChange;
    }

    public T getValueBeforeChange()
    {
        return valueBeforeChange;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.indexOfChange);
        hash = 17 * hash + Objects.hashCode(this.indexBeforeChange);
        hash = 17 * hash + Objects.hashCode(this.valueAtChange);
        hash = 17 * hash + Objects.hashCode(this.valueBeforeChange);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ChangePoint<?,?> other = (ChangePoint<?,?>) obj;
        if (!Objects.equals(this.indexOfChange, other.indexOfChange))
        {
            return false;
        }
        if (!Objects.equals(this.indexBeforeChange, other.indexBeforeChange))
        {
            return false;
        }
        if (!Objects.equals(this.valueAtChange, other.valueAtChange))
        {
            return false;
        }
        if (!Objects.equals(this.valueBeforeChange, other.valueBeforeChange))
        {
            return false;
        }
        return true;
    }

    @Override
    /**
     * Change points are sorted by the time at which the change occurs.
     */
    public int compareTo(ChangePoint<I, T> o)
    {
        return indexOfChange.compareTo(o.getIndexOfChange());
    }

    @Override
    public String toString()
    {
        return "Change from " + valueBeforeChange
            + " at time " + indexBeforeChange
            + " into " + valueAtChange
            + " at time " + indexOfChange;
    }
}
