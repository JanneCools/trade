package core.datastructures.histogram;

import core.dataset.contractors.TypeContractor;
import core.datastructures.Interval;
import java.util.TreeMap;

public class RealDynamicHistogramBuilder extends DynamicHistogramBuilder<Double, TypeContractor<Double>>
{

    @Override
    protected Histogram<Double> createHistogram(TreeMap<Interval<Double>, Long> binningMap, TypeContractor<Double> contractor)
    {
        return new RealHistogram(binningMap);
    }
    
}
