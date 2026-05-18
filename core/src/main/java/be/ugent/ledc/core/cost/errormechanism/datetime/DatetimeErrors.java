package be.ugent.ledc.core.cost.errormechanism.datetime;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;

public enum DatetimeErrors implements ErrorMechanism<LocalDateTime>
{
    DAY_MONTH_SWITCH(new DayMonthSwitch(), "DayMonthSwitch", "dms"),
    ONE_MONTH_OFF(new TemporalOffsetError(ChronoUnit.MONTHS, 1), "OneMonthOff", "1mo"),
    ONE_DAY_OFF(new TemporalOffsetError(ChronoUnit.DAYS, 1), "OneDayOff", "1do"),
    ONE_HOUR_OFF(new TemporalOffsetError(ChronoUnit.HOURS, 1), "OneHourOff", "1ho"),
    ONE_MINUTE_OFF(new TemporalOffsetError(ChronoUnit.MINUTES, 1), "OneMinuteOff", "1mio"),
    MONTH_ERROR(new DateTimeFieldError(ChronoField.MONTH_OF_YEAR), "MonthError", "me"),
    DAY_ERROR(new DateTimeFieldError(ChronoField.DAY_OF_MONTH), "DayError", "de"),
    HOUR_ERROR(new DateTimeFieldError(ChronoField.HOUR_OF_DAY), "HourError", "he"),
    MINUTE_ERROR(new DateTimeFieldError(ChronoField.MINUTE_OF_HOUR), "MinuteError", "mie"),
    SECOND_ERROR(new DateTimeFieldError(ChronoField.SECOND_OF_MINUTE), "SecondError", "se")
    ;
        
    private final ErrorMechanism<LocalDateTime> embeddedMechanism;
    
    private final String name;
    
    private final String shortName;

    DatetimeErrors(ErrorMechanism<LocalDateTime> embeddedMechanism, String name, String shortName)
    {
        this.embeddedMechanism = embeddedMechanism;
        this.name = name;
        this.shortName = shortName;
    }

    @Override
    public boolean explains(LocalDateTime originalValue, LocalDateTime repairedValue, DataObject originalObject)
    {
        return this.embeddedMechanism.explains(originalValue, repairedValue, originalObject);
    }

    @Override
    public List<LocalDateTime> explanations(LocalDateTime originalValue, DataObject originalObject)
    {
        return this.embeddedMechanism.explanations(originalValue, originalObject);
    }

    public String getName()
    {
        return name;
    }

    public ErrorMechanism<LocalDateTime> getEmbeddedMechanism()
    {
        return embeddedMechanism;
    }

    public String getShortName()
    {
        return shortName;
    }
}
