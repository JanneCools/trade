package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.metric.LevenshteinDistance;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TwinError implements ErrorMechanism<Integer>
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
                    o.charAt(i) != r.charAt(i)
                 && o.charAt(i) == o.charAt(i+1)
                 && r.charAt(i) == r.charAt(i+1)
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
                int digit1 = Integer.parseInt(String.valueOf(o.charAt(o.length() - i - 1)));
                int digit2 = Integer.parseInt(String.valueOf(o.charAt(o.length() - i - 2)));

                if(digit1 == digit2)
                {
                    String prefix = o.substring(0, o.length() - i - 2);
                    String suffix = o.substring(o.length() - i);

                    int start = (i == o.length()-2) ? 1 : 0;
                    
                    IntStream
                    .rangeClosed(start, 9)
                    .filter(d-> d != digit1)
                    .map(d -> Integer.parseInt(prefix + d + d + suffix))
                    .forEach(explanations::add);
                }
            }
        }
        
        return explanations;
            
    }
    
}
