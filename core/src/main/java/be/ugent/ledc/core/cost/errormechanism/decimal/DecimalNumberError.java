package be.ugent.ledc.core.cost.errormechanism.decimal;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DecimalNumberError implements ErrorMechanism<BigDecimal>
{   
    private final ErrorMechanism<Integer> embeddedMechanism;

    public DecimalNumberError(ErrorMechanism<Integer> embeddedMechanism)
    {
        this.embeddedMechanism = embeddedMechanism;
    }
        
    @Override
    public boolean explains(BigDecimal originalValue, BigDecimal repairedValue, DataObject originalObject)
    {
        return originalValue != null
            && repairedValue != null
            && embeddedMechanism.explains(
                originalValue.movePointRight(originalValue.scale()).intValue(),
                repairedValue.movePointRight(originalValue.scale()).intValue(),
                originalObject);
    }

    @Override
    public List<BigDecimal> explanations(BigDecimal originalValue, DataObject originalObject)
    {
        if(originalValue == null)
            return new ArrayList<>();
        
        return embeddedMechanism.explanations(
            originalValue.movePointRight(originalValue.scale()).intValue(),
            originalObject)
        .stream()
        .map(i -> new BigDecimal(i).movePointLeft(originalValue.scale()))
        .toList();
    }
    
}
