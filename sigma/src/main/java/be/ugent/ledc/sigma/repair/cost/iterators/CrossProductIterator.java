package be.ugent.ledc.sigma.repair.cost.iterators;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An iterator that produces DataObjects from a cross product of value sets for individual attributes.
 * The values for each attribute are given at construction time and the iterator
 * produces cross-product combinations in no particular order.
 * @author abronsel
 */
public class CrossProductIterator implements Iterator<DataObject>, Iterable<DataObject>
{
    private final Map<String, List<?>> attributeValueMap;
    private final List<Pair<String,Integer>> mask;
    
    /**
     * A constructor that accepts, for each attribute, a list of alternatives
     * @param attributeValueMap The mapping from attributes to lists of acceptable values.
     */
    public CrossProductIterator(Map<String, List<?>> attributeValueMap)
    {
        this.attributeValueMap = attributeValueMap;
        mask = this.attributeValueMap
        .keySet()
        .stream()
        .map(objects -> new Pair<>(objects, 0))
        .collect(Collectors.toList());
    }
    
    /**
     * A helper constructor that creates a degenerate iterator in the sense it will
     * produce only the object passed at construction time.
     * @param o 
     */
    public CrossProductIterator(DataObject o)
    {
        this.attributeValueMap = new HashMap<>();
        
        this.attributeValueMap.putAll(
            o.getAttributes()
            .stream()
            .filter(a -> o.get(a) != null)
            .collect(Collectors.toMap(
                a -> a,
                a -> Stream.of(o.get(a)).collect(Collectors.toList()))
            ));
        
        mask = this.attributeValueMap
        .keySet()
        .stream()
        .map(objects -> new Pair<>(objects, 0))
        .collect(Collectors.toList());
    }

    @Override
    public boolean hasNext()
    {
        return !mask.isEmpty();
    }

    @Override
    public DataObject next()
    {
        //If the mask is null, we return null;
        if(mask.isEmpty())
            return null;

        //Prepare the next object
        DataObject nextObject = new DataObject();

        for(Pair<String,Integer> p : mask)
        {
            nextObject.set(
                p.getFirst(),
                attributeValueMap.get(p.getFirst()).isEmpty()
                    ? null
                    : attributeValueMap.get(p.getFirst()).get(p.getSecond()
                )
            );
        }

        //Before we return it, we progress the mask
        adaptMask();

        //Return the next object
        return nextObject;
    }
            
    private void adaptMask()
    {
        for(int i = 0; i<mask.size(); i++)
        {
            Pair<String, Integer> p = mask.get(i);
            if(p.getSecond() < attributeValueMap.get(p.getFirst()).size()-1)
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
    public Iterator<DataObject> iterator()
    {
        return this;
    }
}
