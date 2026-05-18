package be.ugent.ledc.chronos.algorithms.conflict;

import be.ugent.ledc.core.datastructures.Interval;
import java.util.Objects;

public class Fusion<T>
{
    private final T value;
    private final Interval<Long> first;
    private final Interval<Long> last;

    public Fusion(T value, Interval<Long> first, Interval<Long> last)
    {
        this.value = value;
        this.first = first;
        this.last = last;
    }

    public T getValue()
    {
        return value;
    }

    public Interval<Long> getFirst()
    {
        return first;
    }

    public Interval<Long> getLast()
    {
        return last;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.value);
        hash = 61 * hash + Objects.hashCode(this.first);
        hash = 61 * hash + Objects.hashCode(this.last);
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
        final Fusion<?> other = (Fusion<?>) obj;
        if (!Objects.equals(this.value, other.value))
        {
            return false;
        }
        if (!Objects.equals(this.first, other.first))
        {
            return false;
        }
        if (!Objects.equals(this.last, other.last))
        {
            return false;
        }
        return true;
    }
    
    
}
