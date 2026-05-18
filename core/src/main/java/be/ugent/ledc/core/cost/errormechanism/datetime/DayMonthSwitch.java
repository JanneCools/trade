package be.ugent.ledc.core.cost.errormechanism.datetime;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DayMonthSwitch implements ErrorMechanism<LocalDateTime>
{
    public DayMonthSwitch(){}
    
    @Override
    public boolean explains(LocalDateTime originalValue, LocalDateTime repairedValue, DataObject originalObject)
    {
        if(originalValue == null || repairedValue == null)
            return false;
        
        return !originalValue.equals(repairedValue)
        && originalValue.getYear() == repairedValue.getYear()
        && originalValue.getDayOfMonth() == repairedValue.getMonthValue()
        && originalValue.getMonthValue() == repairedValue.getDayOfMonth()
        && originalValue.getHour() == repairedValue.getHour()                
        && originalValue.getMinute() == repairedValue.getMinute()
        && originalValue.getSecond() == repairedValue.getSecond()
        && originalValue.getNano()== repairedValue.getNano();
    }

    @Override
    public List<LocalDateTime> explanations(LocalDateTime originalValue, DataObject originalObject)
    {
        if(originalValue == null || originalValue.getDayOfMonth() > 12)
            return new ArrayList<>();
        
        return Stream.of(LocalDateTime.of
            (originalValue.getYear(),
            originalValue.getDayOfMonth(),
            originalValue.getMonthValue(),
            originalValue.getHour(),
            originalValue.getMinute(),
            originalValue.getSecond(),
            originalValue.getNano())
        ).collect(Collectors.toList());
    }
    
}
