package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DateContractor extends OrdinalContractor<LocalDate>
{
    private final BiFunction<LocalDate, Long, LocalDate> adder;
    
    private final ChronoUnit truncationLevel;
    
    private final Function<LocalDate,LocalDate> truncator;

    public DateContractor(BiFunction<LocalDate, Long, LocalDate> adder, ChronoUnit truncationLevel, Function<LocalDate, LocalDate> truncator)
    {
        super(TypeContractorFactory.DATE);
        this.adder = adder;
        this.truncationLevel = truncationLevel;
        this.truncator = truncator;
    }

    @Override
    public boolean hasNext(LocalDate current)
    {
        return current != null && !get(current).isAfter(adder.apply(last(), -1L));
    }

    @Override
    public LocalDate next(LocalDate current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no next value.");

        if (hasNext(current)) {
            return adder.apply(get(current), 1L);
        }

        throw new SigmaContractException("LocalDate has no next value for value " + current + " (overflow detected).");
    }

    @Override
    public boolean hasPrevious(LocalDate current)
    {
        return current != null && !get(current).isBefore(adder.apply(first(), 1L));
    }

    @Override
    public LocalDate previous(LocalDate current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no previous value.");

        if (hasPrevious(current)) {
            return adder.apply(get(current), -1L);
        }

        throw new SigmaContractException("LocalDate has no previous value for value " + current + " (overflow detected).");
    }

    @Override
    public LocalDate get(LocalDate current) {
        return truncator.apply(current);
    }

    @Override
    public String name() {
        return "date_in_"+truncationLevel.toString().toLowerCase();
    }

    @Override
    public long cardinality(Interval<LocalDate> range)
    {
         if(range.getLeftBound() == null
            || range.getRightBound() == null
            || get(range.getLeftBound()).equals(first())
            || get(range.getRightBound()).equals(last()))
                return Long.MAX_VALUE;
         
        LocalDate low = range.isLeftOpen() ? next(range.getLeftBound()) : get(range.getLeftBound());
        LocalDate high = range.isRightOpen() ? previous(range.getRightBound()) : get(range.getRightBound());

        if(low.isAfter(high))
            return 0;

        return truncationLevel.between(low, high) + 1;
    }
        
    @Override
    public LocalDate first()
    {
        return get(LocalDate.MIN);
    }

    @Override
    public LocalDate last()
    {
        return get(LocalDate.MAX);
    }

    @Override
    public LocalDate add(LocalDate value, long units) throws SigmaContractException
    {
        Objects.requireNonNull(value, "Cannot add units to null value.");

        if(units == 0 ||
          (units > 0 && !get(value).isAfter(adder.apply(get(LocalDate.MAX), -units))) ||
          (units < 0 && !get(value).isBefore(adder.apply(get(LocalDate.MIN), -units))))
            return adder.apply(get(value), units);

        throw new SigmaContractException("Overflow detected in addition of LocalDate " + value + " with " + units + " " + this.truncationLevel.toString().toLowerCase() + ".");
    }

    @Override
    public LocalDate parseConstant(String constantAsString)
    {
        return get(LocalDate.parse(constantAsString));
    }
}
