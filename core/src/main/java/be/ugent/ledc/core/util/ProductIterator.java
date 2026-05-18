package be.ugent.ledc.core.util;

import be.ugent.ledc.core.datastructures.Pair;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An iterator that produces items from a cross product of value sets.
 * @author abronsel
 * @param <K> Type of object used as an index for value sets
 * @param <V> Type of values
 * @param <O> Type of output
 */
public class ProductIterator<K, V, O>  implements Iterator<O>, Iterable<O>
{
    /**
     * An index from key values to a List of options for that key value
     */
    private final Map<K, List<V>> keyValueMap;
    
    /**
     * A mask that keeps the internal state of the cross product
     */
    private final List<Pair<K,Integer>> mask;
    
    /**
     * A generator that converts a mask into an output object
     */
    private final Function<List<Pair<K,V>>, O> generator;
    
    /**
     * A constructor that accepts, for each attribute, a list of alternatives
     * @param attributeValueMap The mapping from attributes to lists of acceptable values.
     * @param generator
     */
    public ProductIterator(Map<K, List<V>> attributeValueMap, Function<List<Pair<K,V>>,O> generator)
    {
        this.keyValueMap = attributeValueMap;
        mask = this.keyValueMap
            .keySet()
            .stream()
            .map(objects -> new Pair<>(objects, 0))
            .collect(Collectors.toList());
        this.generator = generator;
    }
    
    @Override
    public boolean hasNext()
    {
        return !mask.isEmpty();
    }

    @Override
    public O next()
    {
        //If the mask is null, we return null;
        if(mask.isEmpty())
            return null;


        O next = generator.apply(
            mask
                .stream()
                .map(pair -> new Pair<K,V>(
                    pair.getFirst(), 
                    keyValueMap.get(pair.getFirst()).get(pair.getSecond())
                ))
                .toList()
        );

        //Before we return it, we progress the mask
        adaptMask();

        //Return the next object
        return next;
    }
            
    private void adaptMask()
    {
        for(int i = 0; i<mask.size(); i++)
        {
            Pair<K, Integer> p = mask.get(i);
            if(p.getSecond() < keyValueMap.get(p.getFirst()).size()-1)
            {
                mask.set(i, Pair.createPair(p.getFirst(), p.getSecond() + 1));
                return;
            }
            else
            {
                mask.set(i, Pair.createPair(p.getFirst(), 0));
            }
        }

        //If the mask is not set and the loop is finise
        mask.clear();
    } 

    @Override
    public Iterator<O> iterator()
    {
        return this;
    }
}