package be.ugent.ledc.sigma.datastructures.values;


import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * A value iterator for any data type that is based on a simple enumeration of values.
 * This value iterator is backed by a set.
 * 
 * @author abronsel
 * @param <T> Type of data
 */
public class NominalValueIterator<T> implements ValueIterator<T>
{
    private final Set<T> values;

    public NominalValueIterator(Set<T> values)
    {
        this.values = values;
    }
    
    @Override
    public boolean inSelection(T value)
    {
        return values.contains(value);
    }

    @Override
    public long cardinality()
    {
        return values.size();
    }

    @Override
    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    @Override
    public Iterator<T> iterator()
    {
        return values.iterator();
    }

    public Set<T> getValues() { return values; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NominalValueIterator<?> that = (NominalValueIterator<?>) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "NominalValueIterator{" +
                "values=" + values +
                '}';
    }

    @Override
    public NominalValueIterator<T> convert() {
        return this;
    }
}
