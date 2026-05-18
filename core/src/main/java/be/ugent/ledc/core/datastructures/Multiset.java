package be.ugent.ledc.core.datastructures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The class {@link Multiset} is an implementation of the "Bag" datatype equipped
 * with methods that perform (multi)set calculations in the sense of Yager.
 */
public class Multiset<U> extends HashMap<U, Integer>
{
    /**
     * Empty constructor.
     */
    public Multiset(){}
    
    /**
     * Copy constructor.
     * @param multiset
     */
    public Multiset(Multiset<U> multiset)
    {
        if(multiset != null)
        {
            putAll(multiset);
        }
    }
    
    /**
     * Constructor from map.
     * @param map
     */
    public Multiset(Map<U, Integer> map)
    {
        if(map != null)
        {
            putAll(map);
        }
    }
    
    /**
     * Constructor for collections.
     * @param collection
     */
    public Multiset(Collection <U> collection)
    {
        collection.stream().forEach((u) -> merge(u, 1, Integer::sum));
    }

    public Integer multiplicity(U u)
    {
        return get(u) == null ? 0 : get(u);
    }

    /**
     * Increases the multiplicity of an object with the indicated multiplicity.
     * @param u
     * @param multiplicity
     * @return 
     */
    public Multiset add(U u, Integer multiplicity)
    {
        merge(u, multiplicity, Integer::sum);
        return this;
    }
    
    /**
     * Increases the multiplicity of an object with 1.
     * @param u
     */
    public void add(U u)
    {
        add(u, 1);
    }
    
    /**
     * Return the cardinality of the {@link Multiset}, which is the sum of all multiplicities.
     * @return Cardinality of the multiset: sum of all multiplicities
     */
    public int cardinality()
    {
        return values().stream().mapToInt(i->i).sum();
    }
    
    private Multiset<U> merger(Multiset<U> other, BiFunction<Integer, Integer, Integer> multiplicityMerger)
    {       
        
        Map<U, Integer> mergedMap = Stream
            .concat(keySet().stream(), other.keySet().stream())
            .distinct()
            .collect(Collectors.toMap(
                u -> u,
                u -> multiplicityMerger.apply(multiplicity(u), other.multiplicity(u)))
            );
        
        //Clean
        mergedMap.values().removeIf(mult -> mult == null || mult == 0);
        
        return new Multiset(mergedMap);
    }
    
    /**
     * Calculates the sum of two multisets.
     * @param other The {@link Multiset} that is added to the current one.
     * @return The sum of both multisets
     */
    public Multiset<U> sum(Multiset<U> other)
    {
        return merger(other, Integer::sum);
    }
    
    /**
     * Calculates the difference between two multisets.
     * @param other The {@link Multiset} that is distracted of the current one.
     * @return The difference between both multisets
     */
    public Multiset<U> minus(Multiset<U> other)
    {
        return merger(other, (a,b) -> Math.max(0 , a - b));
    }
    
    /**
     * Calculates the union of two multisets.
     * @param other
     * @return The union of both multisets
     */
    public Multiset<U> union(Multiset<U> other)
    {
        return merger(other, Integer::max);
    }
    
    /**
     * Calculates the intersection of two multisets.
     * @param other
     * @return The intersection of both multisets
     */
    public Multiset<U> intersection(Multiset<U> other)
    {
        return merger(other, Integer::min);
    }
    
    /**
     * Gets the maximum multiplicity.
     * @return The maximum multiplicity occuring in the {@link Multiset}
     */
    public int maximum()
    {
        return super.values().isEmpty() ? 0 : Collections.max(super.values());
    }
    
    /**
     * Gets the minimum multiplicity.
     * @return The maximum multiplicity occuring in the {@link Multiset}
     */
    public int minimum()
    {
        return super.values().isEmpty() ? 0 : Collections.min(super.values());
    }

    /**
     * Returns a {@link Multiset} with objects that have multiplicity larger
     * or equal to cutoff in this multiset.
     * Objects in the returned {@link Multiset} have multiplicity one.
     * @param cutoff
     * @return {@link Multiset} after multiplicity cut.
     */
    public Multiset<U> multiplicityCut(int cutoff)
    {
        return new Multiset<>(entrySet()
            .stream()
            .filter(e -> e.getValue() >= cutoff)
            .map(e -> e.getKey())
            .collect(Collectors.toSet()));
    }
    
    /**
     * Returns a {@link Multiset} with objects that have multiplicity larger or equal
     * to cutoff in this multiset.
     * Objects in the returned {@link Multiset} retain their multiplicity.
     * @param cutoff
     * @return Trimmed version of the {@link Multiset}.
     */
    public Multiset<U> trim(int cutoff)
    {
        return new Multiset<>(entrySet()
            .stream()
            .filter(e -> e.getValue() >= cutoff)
            .collect(Collectors.toMap(
                e -> e.getKey(),
                e -> e.getValue()
            )));
    }

    @Override
    public boolean equals(Object other)
    {
        if(other != null && other instanceof Multiset)
        {
            Multiset<U> otherMultiset = (Multiset)other;
            return isSubsetOf(otherMultiset) && otherMultiset.isSubsetOf(this);
        }
        return false;
    }

    /**
     * Returns true if the current {@link Multiset} is a subset of a given {@link Multiset}.
     * @param other
     * @return Boolean that indicates whether the {@link Multiset} is a subset of M.
     */
    public boolean isSubsetOf(Multiset<U> other)
    {
        return entrySet().stream().allMatch(e -> e.getValue() <= other.multiplicity(e.getKey()));
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        return hash;
    }
}

