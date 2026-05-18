package be.ugent.ledc.core.datastructures;

import java.util.Objects;

/**
 * A generic datatype that models pairs of values of possible different types.
 * @author abronsel
 * @param <F> Type of the first item of this pair.
 * @param <S> Type of the second item of this pair.
 */
public class Pair<F, S>
{
    private final F first;
    private final S second;

    public static <K, V> Pair<K, V> createPair(K first, V second)
    {
        return new Pair<>(first, second);
    }

    public Pair(F first, S second)
    {
        this.first = first;
        this.second = second;
    }

    public F getFirst()
    {
        return first;
    }

    public S getSecond()
    {
        return second;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.first);
        hash = 23 * hash + Objects.hashCode(this.second);
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
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (!Objects.equals(this.first, other.first))
        {
            return false;
        }
        if (!Objects.equals(this.second, other.second))
        {
            return false;
        }
        return true;
    }


    @Override
    public String toString()
    {
        return "(" + first + ", " + second + ")";
    }
}
