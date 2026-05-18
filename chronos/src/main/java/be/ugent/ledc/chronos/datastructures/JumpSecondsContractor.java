package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.DateTimeContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A contractor for LocalDateTime, where the 'unit' is an integer amount of seconds
 * that must lie between 1 and 60, bounds inclusive.
 * 
 * The conversion of a given instance of LocalDateTime via the get method, will
 * apply rounding rules such that the seconds field of the converted instance is:
 * (i) a multiple of the unitary amount of seconds 
 * (ii) as close as possible to the original instance.
 * 
 * @author abronsel
 */
public class JumpSecondsContractor extends DateTimeContractor
{
    private final int gap;

    public JumpSecondsContractor(int gap) throws ChronosException
    {
        super(LocalDateTime::plusSeconds, ChronoUnit.SECONDS);
        
        if(gap <= 0)
            throw new ChronosException("Gap size must be strictly greater than zero.");
        if(gap > 60)
            throw new ChronosException("Gap size must be smaller than or equal to 60.");
        
        this.gap = gap;
    }


    @Override
    public LocalDateTime add(LocalDateTime value, long units) throws SigmaContractException
    {
        return super.add(get(value), units * gap);
    }

    @Override
    public long cardinality(Interval<LocalDateTime> range)
    {
        return super.cardinality(
            Interval.closed(
                get(range.getLeftBound()),
                get(range.getRightBound()
            )
        )) / gap;
    }

    @Override
    public String name()
    {
        return "datetime_in_" +gap + "_seconds";
    }

    @Override
    public boolean hasPrevious(LocalDateTime current)
    {
        return current != null && !get(current).isBefore(add(first(), 1L));
    }
    
    @Override
    public LocalDateTime previous(LocalDateTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no previous value.");

        if (hasPrevious(current))
        {
            return this.add(get(current), -1L);
        }

        throw new SigmaContractException("LocalDateTime has no previous value for value " + current + " (overflow detected).");
    }
    
    @Override
    public boolean hasNext(LocalDateTime current)
    {
        return current != null && !get(current).isAfter(add(last(), -1L));
    }
    
    @Override
    public LocalDateTime next(LocalDateTime current) throws SigmaContractException
    {
        Objects.requireNonNull(current, "Null has no next value.");

        if (hasNext(current))
        {
            return add(get(current), 1L);
        }

        throw new SigmaContractException("LocalDateTime has no next value for value "
            + current
            + " (overflow detected).");
    }
    
    @Override
    public LocalDateTime get(LocalDateTime current) throws SigmaContractException
    {        
        int seconds = current.getSecond();
        
        //Integer division gets us the amount of times the gap fits in the current 
        //amount of seconds
        int s = seconds / gap;
        
        //Remainder
        int r = seconds - s*gap;
        
        if(!current.equals(LocalDateTime.MAX) && !current.equals(LocalDateTime.MIN))
        {
            //Check if rounding down or up
            s = r > gap/2 ? s+1 : s;
        }
        
        //Set seconds to zero
        LocalDateTime conv = current.truncatedTo(ChronoUnit.MINUTES);        
        
        //Return with rounded amount of seconds
        return conv.plus(s*gap, ChronoUnit.SECONDS);
    } 
}
