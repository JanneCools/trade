package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiFunction;

public class TimeContractor extends OrdinalContractor<LocalTime>
{
    private final BiFunction<LocalTime, Long, LocalTime> adder;
    
    private final ChronoUnit truncationLevel;

    public TimeContractor(BiFunction<LocalTime, Long, LocalTime> adder, ChronoUnit truncationLevel)
    {
        super(TypeContractorFactory.TIME);
        this.adder = adder;
        this.truncationLevel = truncationLevel;
    }

    @Override
    public boolean hasNext(LocalTime current)
    {
        return current != null && !get(current).isAfter(adder.apply(last(), -1L));
    }

    @Override
    public LocalTime next(LocalTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no next value.");

        if (hasNext(current)) {
            return adder.apply(get(current), 1L);
        }

        throw new SigmaContractException("LocalTime has no next value for value " + current + " (overflow detected).");
    }

    @Override
    public boolean hasPrevious(LocalTime current) {
        return current != null && !get(current).isBefore(adder.apply(first(), 1L));
    }

    @Override
    public LocalTime previous(LocalTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no previous value.");

        if (hasPrevious(current)) {
            return adder.apply(get(current), -1L);
        }

        throw new SigmaContractException("LocalTime has no previous value for value " + current + " (overflow detected).");
    }

    @Override
    public LocalTime get(LocalTime current)
    {
        return current.truncatedTo(truncationLevel);
    }

    @Override
    public String name()
    {
        return "time_in_" + truncationLevel.toString().toLowerCase();
    }

    @Override
    public long cardinality(Interval<LocalTime> range)
    {       
        if(range.getLeftBound() == null)
            range.setLeftBound(LocalTime.MIN);
        
        if(range.getRightBound() == null)
            range.setRightBound(LocalTime.MAX);
        
        LocalTime low = range.isLeftOpen() ? next(range.getLeftBound()) : get(range.getLeftBound());
        LocalTime high = range.isRightOpen() ? previous(range.getRightBound()) : get(range.getRightBound());

        if(low.isAfter(high))
            return 0;

        return truncationLevel.between(low, high) + 1;
    }
        
    @Override
    public LocalTime first()
    {
        return get(LocalTime.MIN);
    }

    @Override
    public LocalTime last()
    {
        return get(LocalTime.MAX);
    }

    @Override
    public LocalTime add(LocalTime value, long units) throws SigmaContractException
    {
        Objects.requireNonNull(value, "Cannot add units to null value.");

        if(units == 0 ||
           (units > 0 && !get(value).isAfter(adder.apply(get(LocalTime.MAX), -units))) ||
           (units < 0 && !get(value).isBefore(adder.apply(get(LocalTime.MIN), -units))))
            return adder.apply(get(value), units);

        throw new SigmaContractException("Overflow detected in addition of LocalTime " + value + " with " + units + " " + truncationLevel.toString().toLowerCase() + ".");
    }

    @Override
    public LocalTime parseConstant(String constantAsString)
    {
        return get(LocalTime.parse(constantAsString));
    }
}
