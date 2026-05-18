package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.TreeMap;

public class RealDynamicHistogramBuilder extends DynamicHistogramBuilder<Double, TypeContractor<Double>>
{

    @Override
    protected Histogram<Double> createHistogram(TreeMap<Interval<Double>, Long> binningMap, TypeContractor<Double> contractor)
    {
        return new RealHistogram(binningMap);
    }
    
}
