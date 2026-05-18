package be.ugent.ledc.chronos.algorithms.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FifoOptimizer<T> implements FusionOptimizer<T>{

    @Override
    public List<Fusion<T>> getOptimalFusions(ConflictGraph<T> graph)
    {
        //Get all fusions
        List<Fusion<T>> optimalOrder = new ArrayList<>(graph.fusionSet());

        //Sort fusions
        Collections.sort(
            optimalOrder,
            (Fusion o1, Fusion o2) -> o1
                    .getLast()
                    .getLeftBound()
                    .compareTo(o2.getLast().getLeftBound()));
            
        Set<Fusion<T>> taboo = new HashSet<>();
        
        //Truncate optimal order
        for (int i = 0; i < optimalOrder.size(); i++)
        {
            Fusion<T> current = optimalOrder.get(i);
            
            if(!taboo.contains(current))
            {
                taboo.addAll(
                    graph
                    .conflicts(current)
                    .stream()
                    .collect(Collectors.toSet())
                );
            }
        }
        
        optimalOrder.removeAll(taboo);

        return optimalOrder;
    }
}
