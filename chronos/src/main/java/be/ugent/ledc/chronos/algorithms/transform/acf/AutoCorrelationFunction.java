package be.ugent.ledc.chronos.algorithms.transform.acf;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.Statistics;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutoCorrelationFunction
{
    /**
     * Computes the auto-correlation function for a given signal.
     * @param <I>
     * @param <N>
     * @param signal
     * @return 
     */
    public static <I extends Comparable<? super I>, N extends Number> Signal<Long,Double> acf(Signal<I, N> signal) throws ChronosException
    {
        //Initiate a signal
        Signal<Long,Double> acf = new Signal(SigmaContractorFactory.LONG);
        
        List<Double> values = signal
            .entrySet()
            .stream()
            .map(e -> e.getValue() == null
                ? 0.0
                : e.getValue().doubleValue())
            .collect(Collectors.toList());
        
        //Estimate mean
        double mu = values
            .stream()
            .mapToDouble(d->d)
            .summaryStatistics()
            .getAverage();
        
        //Estimate variance
        double variance = Statistics.variance(values);
                
        for(int delta=0; delta < values.size(); delta++)
        {
            List<Double> series = new ArrayList<>();
            
            for(int i=0; i < values.size()-delta; i++)
            {
                series.add((values.get(i) - mu) * (values.get(i+delta) - mu));
            }
            
            double acfAtDelta = series
                .stream()
                .mapToDouble(d->d)
                .sum() / (variance * (values.size()-1));
            
            acf.put((long)delta, acfAtDelta);
        }
        
        return acf;
    }
}
