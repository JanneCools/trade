package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.metric.LevenshteinDistance;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class SingleDigitError implements ErrorMechanism<Integer>
{
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    
    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        return originalValue != null
            && repairedValue != null
            && originalValue.toString().length() == repairedValue.toString().length()
            && levenshtein.distance(originalValue.toString(), repairedValue.toString()) == 1;
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        if(originalValue == null)
            return new ArrayList<>();
        
        String o = originalValue.toString();
        
        List<Integer> explanations = new ArrayList<>();
        
        for(int i=0; i<o.length(); i++)
        {
            int digit = Integer.parseInt(String.valueOf(o.charAt(o.length() - i - 1)));
            
            String prefix = o.substring(0, o.length() - i - 1);
            String suffix = o.substring(o.length() - i);
            
            int rangeStart = (i != o.length() - 1) ? 0 : 1;
            
            IntStream
            .rangeClosed(rangeStart, 9)
            .filter(d-> d != digit)
            .map(d -> Integer.parseInt(prefix + d + suffix))
            .forEach(explanations::add);
        }
        
        return explanations;
    }
//
//    @Override
//    public int hashCode()
//    {
//        int hash = 7;
//        hash = 43 * hash + Objects.hashCode(this.levenshtein);
//        return hash;
//    }
//
//    @Override
//    public boolean equals(Object obj)
//    {
//        return obj != null && obj instanceof SingleDigitError;
//    }
}
