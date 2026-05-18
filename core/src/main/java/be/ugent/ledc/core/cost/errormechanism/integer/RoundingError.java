package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.StringOperations;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rounding error is considered an error where the least significant decimal digits
 * of an integer number are replaced with zeros (e.g. should be 1003 but is 1000)
 * A repaired value explains this error if the original value has a prefix
 * @author abronsel
 */
public class RoundingError implements ErrorMechanism<Integer>
{
    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        if(originalValue == null || repairedValue == null)
            return false;
            
        String o = originalValue.toString();
        String r = repairedValue.toString();
        
        Pattern pattern = Pattern.compile("(\\d*[1-9])(0+)");
        Matcher matcher = pattern.matcher(o);
        
        return matcher.matches() && r.startsWith(matcher.group(1));
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        List<Integer> explanations = new ArrayList<>();
        
        if(originalValue != null)
        {
            String o = originalValue.toString();
            
            Pattern pattern = Pattern.compile("(\\d*[1-9])(0+)");
            Matcher matcher = pattern.matcher(o);
            
            if(matcher.matches())
            {
                String prefix = matcher.group(1);
                
                int rDigits = matcher.group(2).length();
                
                for(int i=1; i<Math.pow(10,rDigits); i++)
                {
                    explanations.add(
                        Integer
                        .valueOf(prefix
                                .concat(StringOperations
                                    .leftPad(""+i, rDigits, '0'))));
                }
            }
        }
        
        return explanations;
        
    }
    
}
