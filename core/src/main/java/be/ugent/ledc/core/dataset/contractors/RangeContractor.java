package be.ugent.ledc.core.dataset.contractors;


import be.ugent.ledc.core.DataException;
import java.util.Objects;
import java.util.function.Predicate;

public class RangeContractor<C extends Comparable<? super C>> implements Predicate<C>
{
    private final C lowerBound;
    private final C upperBound;

    public RangeContractor(C lowerBound, C upperBound) throws DataException
    {
        if(lowerBound.compareTo(upperBound) > 0)
            throw new DataException("Lower bound in RangeContractor cannot be greater than upper bound.");
        
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    
    @Override
    public boolean test(C value)
    {
        return value != null &&
            (lowerBound == null || lowerBound.compareTo(value) <= 0) &&
            (upperBound == null || upperBound.compareTo(value) >= 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeContractor<?> that = (RangeContractor<?>) o;
        return Objects.equals(lowerBound, that.lowerBound) && Objects.equals(upperBound, that.upperBound);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(lowerBound);
        result = 31 * result + Objects.hashCode(upperBound);
        return result;
    }

    @Override
    public String toString() {
        return "RangeContractor{" +
                "lowerBound=" + lowerBound +
                ", upperBound=" + upperBound +
                '}';
    }
}
