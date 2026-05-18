package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.Map;

public class RealFixedWidthHistogramBuilder implements HistogramBuilder<Double, TypeContractor<Double>>
{

    @Override
    public Histogram<Double> buildHistogram(Dataset dataset, int bins, String a, TypeContractor<Double> contractor)
    {
        DoubleSummaryStatistics stats = dataset
            .stream()
            .filter(o -> o.get(a) != null)
            .mapToDouble(o -> o.getDouble(a))
            .summaryStatistics();
            
        double min = stats.getMin();
        double max = stats.getMax();

        if(Double.compare(min, max) == 0)
        {
            System.out.println("No histogram for '" + a + "': all data points are equal.");
            return null;
        }

        //Compute bin width
        double w = (max - min) / (double)bins;

        Map<Interval<Double>, Long> binningMap = new LinkedHashMap<>();

        for(long i=0; i<bins-1; i++)
        {
            binningMap
                .put(
                    new Interval<>(
                        min + i*w,
                        min + (i+1)*w,
                        false, //Include left bound
                        true), //Exclude right bound
                    0l);
        }

        binningMap
            .put(
                new Interval<>(
                    min + w*(bins-1),
                    max,
                    false,
                    false),
                0l);

        RealHistogram histogram = new RealHistogram(binningMap);
        histogram.fill(dataset, a, contractor);
        return histogram;
    }
    
}
