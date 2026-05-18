package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.datastructures.Interval;
import java.util.Map;

public class RealHistogram extends Histogram<Double>
{
    public RealHistogram(Map<Interval<Double>, Long> binningMap)
    {
        super(binningMap);
    }

    @Override
    public Double binHeight(Interval<Double> bin)
    {
        if(bin == null)
            return null;
        
        return countFor(bin).doubleValue() /
               (bin.getRightBound() - bin.getLeftBound());
    }
    
}
