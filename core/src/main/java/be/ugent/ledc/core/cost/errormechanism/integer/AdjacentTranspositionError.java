package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.metric.DamerauDistance;
import be.ugent.ledc.core.operators.metric.LevenshteinDistance;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdjacentTranspositionError implements ErrorMechanism<Integer>
{
    private final DamerauDistance damerau = new DamerauDistance();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    
    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        return originalValue != null
            && repairedValue != null
            && originalValue.toString().length() == repairedValue.toString().length()
            && !repairedValue.toString().startsWith("0")
            && damerau.distance(originalValue.toString(), repairedValue.toString()) == 1
            && levenshtein.distance(originalValue.toString(), repairedValue.toString()) == 2;
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        if(originalValue == null)
            return new ArrayList<>();
        
        String o = originalValue.toString();
        
        return IntStream
        .range(0, o.length() - 1)
        .filter(i -> o.charAt(i) != o.charAt(i+1))
        .filter(i -> i > 0 || o.charAt(i+1) != '0')
        .mapToObj(i -> Integer.parseInt(
                o.substring(0, i) //Prefix
                .concat(String.valueOf(o.charAt(i+1)))  //Char at i+1
                .concat(String.valueOf(o.charAt(i)))    //Char at i
                .concat(i+2 > o.length() ? "" : o.substring(i+2)) // Suffix
            ))
        .collect(Collectors.toList());
    }
    
}
