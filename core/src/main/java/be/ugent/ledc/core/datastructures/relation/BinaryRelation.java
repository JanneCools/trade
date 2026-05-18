package be.ugent.ledc.core.datastructures.relation;

import be.ugent.ledc.core.datastructures.Couple;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A standard implementation of a binary relation that is backed by
 * a HashMap from T elements to sets of T elements.
 * @author abronsel
 * @param <T> 
 */
public class BinaryRelation<T> implements IBinaryRelation<T>
{
    /**
     * Mapping that represents the binary relation
     */
    private final Map<T, Set<T>> mapping;
    
    public BinaryRelation(Set<T> elements)
    {
        this.mapping = elements
            .stream()
            .collect(Collectors.toMap(
                t -> t,
                t -> new HashSet<>())
            );
    }
    
    public BinaryRelation()
    {
        this(new HashSet<>());
    }
    
    /**
     * Copy constructor
     * @param elements 
     */
    public BinaryRelation(Map<T, Set<T>> elements)
    {
        this.mapping = elements
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey(),
                e -> new HashSet<>(e.getValue()))
            );
    }
    
    @Override
    public boolean contains(Couple<T> couple)
    {
        if(!mapping.containsKey(couple.getFirst()))
            return false;
        
        return mapping
            .get(couple.getFirst())
            .contains(couple.getSecond());
    }

    @Override
    public void add(Couple<T> couple)
    {
        if(!mapping.containsKey(couple.getFirst()))
            mapping.put(couple.getFirst(), new HashSet<>());
        
        if(!mapping.containsKey(couple.getSecond()))
            mapping.put(couple.getSecond(), new HashSet<>());

        mapping.get(couple.getFirst()).add(couple.getSecond());
    }
    
    @Override
    public void delete(Couple<T> couple)
    {
        if(!mapping.containsKey(couple.getFirst()))
            return;
        
        if(!mapping.containsKey(couple.getSecond()))
            return;

        mapping.get(couple.getFirst()).remove(couple.getSecond());
    }

    @Override
    public Stream<T> elementStream()
    {
        return mapping.keySet().stream();
    }

    @Override
    public Stream<Couple<T>> stream()
    {
        return mapping
            .keySet()
            .stream()
            .flatMap(t1 -> mapping
                .get(t1)
                .stream()
                .map(t2 -> new Couple<>(t1,t2)));
    }
    
    /**
     * Returns the transitive closure of the BinaryRelation, computed with
     * Warren's algorithm.
     * @return 
     */
    @Override
    public BinaryRelation<T> transitiveClosure()
    {
        //Start with construction of reflexive closure
        BinaryRelation<T> transClosure = reflexiveClosure();

        List<T> tList = transClosure.elementStream().toList();
        
        //First Pass of Warrens algorithm
        for(int i=1; i<tList.size(); i++)
        {
            for(int k=0; k<i; k++)
            {
                //If M[i][k]
                if(transClosure.contains(tList.get(i),tList.get(k)))
                {
                    for(int j=0;j<tList.size(); j++)
                    {
                        //M[i][j] = M[i][j] || M[k][j];
                        if(transClosure.contains(tList.get(k), tList.get(j)))
                        {
                            transClosure.add(tList.get(i), tList.get(j));
                        }
                    }
                }
            }
        }
        
        //Second Pass of Warrens algorithm
        for(int i=0; i<tList.size()-1; i++)
        {
            for(int k=i+1; k<tList.size(); k++)
            {
                //If M[i][k]
                if(transClosure.contains(tList.get(i),tList.get(k)))
                {
                    for(int j=0;j<tList.size(); j++)
                    {
                        //M[i][j] = M[i][j] || M[k][j];
                        if(transClosure.contains(tList.get(k), tList.get(j)))
                        {
                            transClosure.add(tList.get(i), tList.get(j));
                        }
                    }
                }
            }
        }

        
        //Return
        return transClosure;
    }
    
    @Override
    public BinaryRelation<T> reflexiveClosure()
    {
        //Make a new relation
        BinaryRelation reflexiveClosure = new BinaryRelation(this.mapping);

        for(T t: this.mapping.keySet())
        {
            reflexiveClosure.add(t, t);
        }
        
        return reflexiveClosure;
    }
}