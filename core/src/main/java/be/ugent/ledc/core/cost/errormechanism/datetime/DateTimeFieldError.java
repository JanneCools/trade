package be.ugent.ledc.core.cost.errormechanism.datetime;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateTimeFieldError implements ErrorMechanism<LocalDateTime>
{  
    private final ChronoField explainableField;
    
    public static final List<ChronoField> FIELDS = Stream.of
    (
        ChronoField.MONTH_OF_YEAR,
        ChronoField.DAY_OF_MONTH,
        ChronoField.HOUR_OF_DAY,
        ChronoField.MINUTE_OF_HOUR,
        ChronoField.SECOND_OF_MINUTE
    ).collect(Collectors.toList());

    public DateTimeFieldError(ChronoField explainableField)
    {
        this.explainableField = explainableField;
        
        if(!FIELDS.contains(explainableField))
            throw new RepairRuntimeException("Explainable field "
                + explainableField.toString()
                + " does not belong to the permitted fields "
                + FIELDS);
    }
    
    @Override
    public boolean explains(LocalDateTime originalValue, LocalDateTime repairedValue, DataObject originalObject)
    {
        if(originalValue == null || repairedValue == null)
            return false;
        
        return
        originalValue.get(explainableField) != repairedValue.get(explainableField) &&
        FIELDS
        .stream()
        .filter(field -> field != explainableField)
        .allMatch(field -> originalValue.get(field) == repairedValue.get(field));
        
    }

    @Override
    public List<LocalDateTime> explanations(LocalDateTime originalValue, DataObject originalObject)
    {
        List<LocalDateTime> explanations = new ArrayList<>();
        
        if(originalValue != null)
        {
            ValueRange range = originalValue.range(explainableField);

            for(long i=range.getMinimum(); i<=range.getMaximum(); i++)
            {
                if(i - originalValue.get(explainableField) != 0)
                {
                    LocalDateTime explanation = LocalDateTime.from(originalValue);

                    explanation = explanation.plus(
                        i - originalValue.get(explainableField),
                        explainableField.getBaseUnit()
                    );
                    explanations.add(explanation);
                }
            }
        }
        
        return explanations;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.explainableField);
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
        final DateTimeFieldError other = (DateTimeFieldError) obj;
        return this.explainableField == other.explainableField;
    }
    
    
}
