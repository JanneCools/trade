package be.ugent.ledc.core.datastructures.kdtree;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NearestNeighbours
{
    private final TreeMap<Pair<DataObject, Double>, Integer> nearestNeighbourMap;
    
    private final int k;

    public NearestNeighbours(int k)
    {
        this.nearestNeighbourMap = new TreeMap<>(Comparator.comparing(Pair::getSecond));
        this.k = k;
    }
    
    public int neighbourhoodSize()
    {
        return nearestNeighbourMap
        .values()
        .stream()
        .mapToInt(i->i)
        .sum();
    }
    
    public Double maxDistance()
    {
        return nearestNeighbourMap.lastKey().getSecond();
    }
    
    public void update(DataObject neighbour, Double neighbourDistance, int count)
    {
        if(neighbourhoodSize() < k || neighbourDistance < maxDistance())
        {
            //Add
            this
            .nearestNeighbourMap
            .put(new Pair<>(neighbour, neighbourDistance), count);
            
            //Check if there are too many elements
            int overflow = neighbourhoodSize() - k;
            
            while(overflow > 0)
            {
                //If overflow is more than the last value's count
                if(overflow >= nearestNeighbourMap.lastEntry().getValue())
                {
                    //Subtract the count from overflow
                    overflow -= nearestNeighbourMap.lastEntry().getValue();
                    
                    //Remove the last value
                    nearestNeighbourMap.remove(nearestNeighbourMap.lastKey());
                }
                else
                {
                    //Adjust the count of the last value
                    Pair<DataObject, Double> last = nearestNeighbourMap.lastKey();
                    
                    nearestNeighbourMap.put(last, nearestNeighbourMap.get(last) - overflow);
                    
                    overflow = 0;
                }
            }
        }
    }
    
    public List<DataObject> getNeighbours()
    {
        return 
            this.nearestNeighbourMap.entrySet()
            .stream()
            .flatMap(e -> IntStream
                .range(0, e.getValue())
                .mapToObj(i -> e.getKey().getFirst()))
            .collect(Collectors.toList());
    }
    
}
