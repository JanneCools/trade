package be.ugent.ledc.chronos.algorithms.conflict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MajorityOptimizer<T> implements FusionOptimizer<T>
{
    @Override
    public List<Fusion<T>> getOptimalFusions(ConflictGraph<T> graph)
    {
        //Initialize an empty
        List<Fusion<T>> optimalOrder = new ArrayList<>();

        //Iteratively find the fusion with least neighbours
        while (!graph.fusionSet().isEmpty())
        {
            Map<T, Integer> countMap = new HashMap<>();
            
            //Count fusion values that remain in graph
            for(Fusion<T> fusion: graph.fusionSet())
            {
                if(!countMap.containsKey(fusion.getValue()))
                {
                    countMap.put(fusion.getValue(), 0);
                }
                
                countMap.put(fusion.getValue(), countMap.get(fusion.getValue()) + 1);
            }
            
            int maxCount = 0;
            T best = null;
            
            for(T o: countMap.keySet())
            {
                if(countMap.get(o) > maxCount)
                {
                    maxCount = countMap.get(o);
                    best = o;
                }
            }
            
            Set<Fusion<T>> toRemove = new HashSet<>();
            
            //Remove
            for (Fusion<T> candidate: graph.fusionSet())
            {
                if(candidate.getValue().equals(best))
                {
                    optimalOrder.add(candidate);
                    
                    for (Fusion<T> fusion: graph.fusionSet())
                    {
                        if(graph.inConflict(fusion, candidate))
                        {
                            toRemove.add(fusion);
                        }    
                    }
                    
                    toRemove.add(candidate);
                }
            }
            
            graph.remove(toRemove);
        }

        return optimalOrder;
    }

}
