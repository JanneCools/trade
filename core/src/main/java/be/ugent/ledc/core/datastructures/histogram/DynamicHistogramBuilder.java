package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DynamicHistogramBuilder<D extends Comparable<? super D>, C extends TypeContractor<D>> implements HistogramBuilder<D, C>
{

    @Override
    public Histogram<D> buildHistogram(Dataset dataset, int bins, String a, C contractor)
    {
        //Count occurrences of each value
        TreeMap<D, Long> countMap = dataset
            .stream()
            .filter(o -> o.get(a) != null)
            .map(o -> contractor.getFromDataObject(o, a)) //Get value
            .collect(Collectors.groupingBy(
                Function.identity(),
                () -> new TreeMap<>(),
                Collectors.counting())
            );
        
        long binCount = countMap
            .values()
            .stream()
            .mapToLong(l->l)
            .sum() / bins;

        //Initialize
        TreeMap<Interval<D>, Long> binningMap = new TreeMap<>(Interval.leftBoundComparator());
        
        for(D value: countMap.keySet())
        {
            if(binningMap.isEmpty() || binningMap.get(binningMap.lastKey()) > binCount)
            {
                binningMap.put(new Interval<>(value, value, false, false), countMap.get(value));
            }
            else
            {
                Map.Entry<Interval<D>, Long> lastEntry = binningMap.pollLastEntry();
                
                Interval<D> binKey = lastEntry.getKey();
                binKey.setRightBound(value);
                
                binningMap.put(
                    binKey,
                    lastEntry.getValue() + countMap.get(value)
                );
                
            }
        }
        
        return createHistogram(binningMap, contractor);
        
    }

    protected abstract Histogram<D> createHistogram(TreeMap<Interval<D>, Long> binningMap, C contractor);    
}
