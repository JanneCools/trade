package core.cost.errormechanism.decimal;

import core.dataset.DataObject;
import core.cost.errormechanism.ErrorMechanism;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DecimalSignError implements ErrorMechanism<BigDecimal>
{
    @Override
    public boolean explains(BigDecimal originalValue, BigDecimal repairedValue, DataObject originalObject) {

        if(originalValue == null || repairedValue == null)
            return false;
        
        return 
            originalValue.scale() == repairedValue.scale()
        &&  originalValue.multiply(BigDecimal.valueOf(-1)).equals(repairedValue);

    }

    @Override
    public List<BigDecimal> explanations(BigDecimal originalValue, DataObject originalObject) {

        List<BigDecimal> explanations = new ArrayList<>();

        if(originalValue != null)
        {
            explanations.add(originalValue.multiply(BigDecimal.valueOf(-1)));
        }

        return explanations;

    }
    
}
