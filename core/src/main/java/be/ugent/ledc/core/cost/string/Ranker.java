package be.ugent.ledc.core.cost.string;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Ranker implements CostFunction<String>
{
    private final Map<String,Integer> ranks;
    
    private Ranker(List<String> rankList)
    {
        this.ranks = new HashMap<>();
        for(int i=0;i<rankList.size();i++)
        {
            ranks.put(rankList.get(i), i);
        }
    }   
    
    public Ranker(Map<String,Integer> ranks)
    {
        this.ranks = ranks;
    }   
    
    @Override
    public int computeCost(String originalValue, String repairedValue, DataObject originalObject)
    {
        if(repairedValue == null)
            return 10000;
        
        Integer index = ranks.get(repairedValue);
        
        return 1 + (index == null ? ranks.size() : index);
    }
    
    public static Ranker createRankerByFrequency(ContractedDataset dirty, String a)
    {
        return new Ranker(dirty
            .project(a)
            .select(o -> o.get(a) != null)
            .stream()
            .map(o -> o.getString(a))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(Map.Entry<String,Long>::getValue).reversed())
            .map(Map.Entry::getKey)
            .toList());
    }
    
    public static Ranker createRankerByInverseFrequency(ContractedDataset dirty, String a)
    {
        return new Ranker(dirty
            .project(a)
            .select(o -> o.get(a) != null)
            .stream()
            .map(o -> o.getString(a))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(Map.Entry<String,Long>::getValue))
            .map(Map.Entry::getKey)
            .toList());
    }
    
    public static Ranker createNaturalRanker(ContractedDataset dirty, String a)
    {
        return new Ranker(dirty
            .project(a)
            .select(o -> o.get(a) != null)
            .stream()
            .map(o -> o.getString(a))
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList());
    }
    
    public static Ranker createInverseNaturalRanker(ContractedDataset dirty, String a)
    {
        return new Ranker(dirty
            .project(a)
            .select(o -> o.get(a) != null)
            .stream()
            .map(o -> o.getString(a))
            .distinct()
            .sorted(Comparator.<String>naturalOrder().reversed())
            .toList());
    }

    public Map<String, Integer> getRanks() {
        return ranks;
    }
    
    
}
