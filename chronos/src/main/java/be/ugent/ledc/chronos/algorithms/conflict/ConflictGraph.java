package be.ugent.ledc.chronos.algorithms.conflict;

import be.ugent.ledc.core.util.SetOperations;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConflictGraph<T>
{
    private final Map<Fusion<T>,Set<Fusion<T>>> vertexMap;

    public ConflictGraph()
    {
        this(new HashMap<>());
    }
    
    public ConflictGraph(Map<Fusion<T>, Set<Fusion<T>>> vertexMap)
    {
        this.vertexMap = vertexMap;
    }
    
    public void addFusion(Fusion<T> fusion)
    {
        this.vertexMap.put(fusion, new HashSet<>());
    }
    
    public void addConflict(Fusion<T> head, Fusion<T> tail)
    {
        if(vertexMap.get(head) == null)
            addFusion(head);
        
        if(vertexMap.get(tail) == null)
            addFusion(tail);
        
        this.vertexMap.get(head).add(tail);
        this.vertexMap.get(tail).add(head);
    }
    
    public Set<Fusion<T>> fusionSet()
    {
        return vertexMap.keySet();
    }
    
    public Set<Fusion<T>> conflicts(Fusion<T> head)
    {
        return vertexMap.get(head) == null
            ? new HashSet<>()
            : vertexMap.get(head);
    }
    
    
    
    public void remove(Set<Fusion<T>> fusions)
    {
        //Clean the list of adjacent nodes
        vertexMap
            .values()
            .forEach(adjList -> adjList
                .removeIf(candidate -> fusions.contains(candidate)));
        
        vertexMap
            .keySet()
            .removeIf(f -> fusions.contains(f));
    }
    
    public void remove(Fusion<T> ... fusions)
    {
        remove(SetOperations.set(fusions));
    }
    
    public int degree(Fusion<T> fusion)
    {
        return vertexMap.get(fusion) == null
            ? 0
            :  vertexMap.get(fusion).size();
    }
    
    public boolean inConflict(Fusion<T> left, Fusion<T> right)
    {
        return vertexMap.get(left) != null && vertexMap.get(left).contains(right);
    }
}
