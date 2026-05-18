package be.ugent.ledc.chronos.algorithms.conflict;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GreedyOptimizer<T> implements FusionOptimizer<T>
{
    @Override
    public List<Fusion<T>> getOptimalFusions(ConflictGraph<T> graph)
    {
        //Initialize an empty
        List<Fusion<T>> optimalOrder = new ArrayList<>();

        //Iteratively find the fusion with least neighbours
        while (!graph.fusionSet().isEmpty())
        {
            int minDegree = Integer.MAX_VALUE;
            Fusion best = null;
            
            for(Fusion<T> fusion: graph.fusionSet())
            {
                if(graph.degree(fusion) < minDegree)
                {
                    minDegree = graph.degree(fusion);
                    best = fusion;
                }
            }
            
            //Add best fusion
            optimalOrder.add(best);

            Set<Fusion<T>> toRemove = new HashSet<>();
            
            //Remove its neighbours
            for (Fusion<T> fusion: graph.fusionSet())
            {
                if(graph.inConflict(best, fusion))
                {
                    toRemove.add(fusion);
                }
            }
            
            graph.remove(toRemove);
            graph.remove(best);
        }

        return optimalOrder;
    }

}
