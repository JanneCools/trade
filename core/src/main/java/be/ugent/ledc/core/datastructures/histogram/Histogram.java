package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Histogram<D extends Comparable<? super D>> implements Iterable<Interval<D>>
{
    private final TreeMap<Interval<D>, Long> binningMap = new TreeMap<>(Interval.leftBoundComparator());

    public Histogram(Map<Interval<D>, Long> binningMap)
    {
        this.binningMap.putAll(binningMap);
    }
    
    public Interval<D> find(D dataPoint)
    {
        return binningMap
            .keySet()
            .stream()
            .filter(i -> i.contains(dataPoint))
            .findFirst()
            .orElse(null);
    }
    
    public Long countFor(Interval<D> bin)
    {
        return binningMap.get(bin);
    }
    
    public abstract Double binHeight(Interval<D> bin);
    
    public int numberOfBins()
    {
        return binningMap.size();
    }
    
    public Double normalizedBinHeight(D dataPoint)
    {
        double maxBinHeight = (double)binningMap
                .keySet()
                .stream()
                .mapToDouble(bin -> binHeight(bin))
                .max()
                .getAsDouble();
        
        return  binHeight(find(dataPoint)) / maxBinHeight;
    }

    public void fill(Dataset data, String a, TypeContractor<D> contractor)
    {
        List<D> values = data
            .stream()
            .filter(o -> o.get(a) != null)
            .map(o -> contractor.getFromDataObject(o, a))
            .collect(Collectors.toList());
        
        for(D value: values)
        {
            for(Interval<D> interval: binningMap.keySet())
            {
                if(interval.contains(value))
                {
                    binningMap.merge(interval, 1l, Long::sum);
                    break;
                }
            }
        }
    }

    @Override
    public Iterator<Interval<D>> iterator()
    {
        return binningMap.keySet().iterator();
    }
    
    public Stream<Interval<D>> stream()
    {
        return binningMap.keySet().stream();
    }
    
    public Long maxCount()
    {
        return binningMap.values().stream().mapToLong(l->l).max().getAsLong();
    }

}
