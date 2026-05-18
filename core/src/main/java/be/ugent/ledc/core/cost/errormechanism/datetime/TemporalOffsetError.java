package be.ugent.ledc.core.cost.errormechanism.datetime;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class TemporalOffsetError implements ErrorMechanism<LocalDateTime>
{
    private final ChronoUnit temporalUnit;
    
    private final long amount;
    
    /**
     * When ranged is set to true, intermediary offsets will qualify for explanations as well.
     */
    private final boolean ranged;

    public TemporalOffsetError(ChronoUnit temporalUnit, long amount, boolean ranged)
    {
        this.temporalUnit = temporalUnit;
        this.amount = amount;
        this.ranged = ranged;
    }
    
    public TemporalOffsetError(ChronoUnit temporalUnit, long amount)
    {
        this(temporalUnit, amount, false);
    }

    @Override
    public boolean explains(LocalDateTime originalValue, LocalDateTime repairedValue, DataObject originalObject)
    {
        if (originalValue == null || repairedValue == null)
            return false;
        
        LongStream offsets = ranged
            ? LongStream.rangeClosed(1, amount)
            : LongStream.of(amount);
        
        return offsets.anyMatch(a ->
                originalValue.plus(a, temporalUnit).equals(repairedValue)
            ||  originalValue.minus(a, temporalUnit).equals(repairedValue)
        );
    }

    @Override
    public List<LocalDateTime> explanations(LocalDateTime originalValue, DataObject originalObject)
    {
        if(originalValue == null)
            return  new ArrayList<>();
        
        LongStream offsets = ranged
            ? LongStream.rangeClosed(-amount, amount)
            : LongStream.of(amount);
        
        return offsets
                .filter(offset -> offset != 0)
                .mapToObj(a -> originalValue.plus(a, temporalUnit))
                .collect(Collectors.toList());

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.temporalUnit);
        hash = 71 * hash + (int) (this.amount ^ (this.amount >>> 32));
        hash = 71 * hash + (this.ranged ? 1 : 0);
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
        final TemporalOffsetError other = (TemporalOffsetError) obj;
        if (this.amount != other.amount) {
            return false;
        }
        if (this.ranged != other.ranged) {
            return false;
        }
        return this.temporalUnit == other.temporalUnit;
    }
    
    
}
