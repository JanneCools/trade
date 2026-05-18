package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiFunction;

public class DateTimeContractor extends OrdinalContractor<LocalDateTime>
{
    private final BiFunction<LocalDateTime, Long, LocalDateTime> adder;
    
    private final ChronoUnit truncationLevel;

    public DateTimeContractor(BiFunction<LocalDateTime, Long, LocalDateTime> adder, ChronoUnit truncationLevel)
    {
        super(TypeContractorFactory.DATETIME);
        this.adder = adder;
        this.truncationLevel = truncationLevel;
    }

    @Override
    public boolean hasNext(LocalDateTime current) {
        return current != null &&
            !get(current).isAfter(adder.apply(last(), -1L));
    }

    @Override
    public LocalDateTime next(LocalDateTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no next value.");

        if (hasNext(current)) {
            return adder.apply(get(current), 1L);
        }

        throw new SigmaContractException("LocalDateTime has no next value for value " + current + " (overflow detected).");
    }

    @Override
    public boolean hasPrevious(LocalDateTime current) {
        return current != null &&
                !get(current).isBefore(adder.apply(first(), 1L));
    }

    @Override
    public LocalDateTime previous(LocalDateTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no previous value.");

        if (hasPrevious(current)) {
            return adder.apply(get(current), -1L);
        }

        throw new SigmaContractException("LocalDateTime has no previous value for value " + current + " (overflow detected).");
    }

    @Override
    public LocalDateTime get(LocalDateTime current)
    {
        return current == null ? null : current.truncatedTo(truncationLevel);
    }

    @Override
    public String name()
    {
        return "datetime_in_" + truncationLevel.toString().toLowerCase();
    }

    @Override
    public long cardinality(Interval<LocalDateTime> range)
    {
        if(range.getLeftBound() == null
            || range.getRightBound() == null
            || get(range.getLeftBound()).equals(first())
            || get(range.getRightBound()).equals(last()))
                return Long.MAX_VALUE;
        
        LocalDateTime low = range.isLeftOpen()
            ? next(range.getLeftBound())
            : get(range.getLeftBound());
        
        LocalDateTime high = range.isRightOpen()
            ? previous(range.getRightBound())
            : get(range.getRightBound());

        if(low.isAfter(high))
            return 0;

        return truncationLevel.between(low, high) + 1;
    }
        
    @Override
    public LocalDateTime first()
    {
        return get(LocalDateTime.MIN);
    }

    @Override
    public LocalDateTime last()
    {
        return get(LocalDateTime.MAX);
    }

    @Override
    public LocalDateTime add(LocalDateTime value, long units) throws SigmaContractException
    {
        Objects.requireNonNull(value, "Cannot add units to null value.");

        if(units == 0 ||
          (units > 0 && !get(value).isAfter(adder.apply(get(LocalDateTime.MAX), -units))) ||
          (units < 0 && !get(value).isBefore(adder.apply(get(LocalDateTime.MIN), -units))))
            return adder.apply(get(value), units);

        throw new SigmaContractException("Overflow detected in addition of LocalDateTime " + value + " with " + units + " " + truncationLevel.toString().toLowerCase() + ".");
    }
    
    @Override
    public LocalDateTime parseConstant(String constantAsString)
    {
        return get(LocalDateTime.parse(constantAsString));
    }
}
