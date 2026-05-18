package be.ugent.ledc.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemSelector
{
    /**
     * Select one item from the given items in the map, with probability proportionate with the count of the item divided by the sum of counts.
     * @param <I>
     * @param itemCounts A mapping of items and their counts
     */
    public static <I> I selectItemByCount(Map<I, Integer> itemCounts)
    {
        //If there are no items, return null
        if(itemCounts.isEmpty())
            return null;
        
        //Calculate total count
        int total = itemCounts.values().stream().filter(i -> i != null).mapToInt(i->i).sum();
        
        
        Map<I, Double> probabilityMap = itemCounts
                .entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(e -> e.getKey(), e -> ((double)e.getValue())/((double)total)));
        
        return selectItemByProbability(probabilityMap);
    }
    
    public static <I> I selectItemAtRandom(Set<I> items)
    {
        return selectItemByCount(items
        .stream()
            .collect(Collectors.toMap(
                i->i,
                i->1)
        ));
    }
    
    /**
     * Select one item from the given items in the map, with probability proportionate with the probability of that item
     * @param <I>
     * @param itemProbabilities A mapping of items and their probability
     * @return An item selected with probability proportionate to its frequency
     */
    public static <I> I selectItemByProbability(Map<I, Double> itemProbabilities)
    {
        //If there are no items, return null
        if(itemProbabilities.isEmpty())
            return null;
            
        List<I> itemList = new ArrayList<>(itemProbabilities.keySet());
        
        double[] cumulativeProbabilities = new double[itemList.size()];
        
        double sum = 0.0;
        
        for(int i=0;i<itemList.size(); i++)
        {
            I item = itemList.get(i);
            sum += itemProbabilities.get(item);
            cumulativeProbabilities[i] = sum;
        }

        double p = new Random().nextDouble();
        
        for(int i=0;i<cumulativeProbabilities.length; i++)
        {
            if(p < cumulativeProbabilities[i])
            {
                return itemList.get(i);
            }
        }
        
        return itemList.get(itemList.size()-1);
    }
}
