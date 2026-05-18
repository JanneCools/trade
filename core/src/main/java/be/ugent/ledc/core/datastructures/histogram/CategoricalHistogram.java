package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.datastructures.Interval;
import java.util.Map;

public class CategoricalHistogram<D extends Comparable<? super D>> extends Histogram<D>
{

    public CategoricalHistogram(Map<Interval<D>, Long> binningMap)
    {
        super(binningMap);
    }

    @Override
    public Double binHeight(Interval<D> bin)
    {
        return countFor(bin).doubleValue(); 
    }
    
}
