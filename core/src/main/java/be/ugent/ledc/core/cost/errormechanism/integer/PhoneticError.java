package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.metric.LevenshteinDistance;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PhoneticError implements ErrorMechanism<Integer>      
{
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    
    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        if(originalValue == null || repairedValue == null)
            return false;
        
        String o = originalValue.toString();
        String r = repairedValue.toString();
        
        return o.length() == r.length()
            && o.length() >= 2 
            && levenshtein.distance(o, r) == 2
            && IntStream
                .range(0, o.length()-1)
                .anyMatch(i ->
                        (Integer.parseInt("" + o.charAt(i)) > 1
                        && o.charAt(i) == r.charAt(i+1)
                        && o.charAt(i+1) == '0'
                        && r.charAt(i) == '1')
                    ||  (Integer.parseInt("" + o.charAt(i+1)) > 1
                        && o.charAt(i+1) == r.charAt(i)
                        && o.charAt(i) == '1'
                        && r.charAt(i+1) == '0')
                );
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        List<Integer> explanations = new ArrayList<>();
        
        if(originalValue != null)
        {
            String o = originalValue.toString();
            
            for(int i=0; i<o.length()-1; i++)
            {
                int digit2 = Integer.parseInt(String.valueOf(o.charAt(o.length() - i - 1)));
                int digit1 = Integer.parseInt(String.valueOf(o.charAt(o.length() - i - 2)));

                if(digit1 >= 2 && digit2 == 0)
                {
                    String prefix = o.substring(0, o.length() - i - 2);
                    String suffix = o.substring(o.length() - i);

                    explanations.add(Integer.parseInt(prefix + "1" + digit1 + suffix));
                }
                if(digit1 == 1 && digit2 >= 2)
                {
                    String prefix = o.substring(0, o.length() - i - 2);
                    String suffix = o.substring(o.length() - i);
                    
                   
                    explanations.add(Integer.parseInt(prefix + digit2 + "0" + suffix));
                }
            }
        }
        
        return explanations;
    }
    
}
