package be.ugent.ledc.chronos.algorithms.transform.summarisation;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SignalSummarizer<I extends Comparable<? super I>, T, S>
{
    private final int windowSizeInUnits;
    
    private final Function<List<T>, S> aggregator;

    public SignalSummarizer(Function<List<T>, S> aggregator, int windowSizeInUnits)
    {
        this.aggregator = aggregator;
        this.windowSizeInUnits = windowSizeInUnits;
    }   
    
    public Map<Interval<I>, S> summarize(Signal<I, T> original) throws ChronosException
    {
        boolean stop = false;
        
        TreeMap<Interval<I>, S> summary = new TreeMap<>(Interval.leftBoundComparator());
                
        OrdinalContractor<I> indexContractor = original.getIndexContractor();
        
        while(!stop)
        {
            I start = summary.isEmpty()
                ? original.start()
                : summary.lastKey().getRightBound();
            
            I end = indexContractor.add(start, windowSizeInUnits);
            
            Interval<I> window = new Interval<>(start, end, false, true);
            
            S summaryValue = aggregator.apply(
                original
                    .subSignal(start, end)
                    .indexStream()
                    .map(i -> original.get(i))
                    .collect(Collectors.toList()));
            
            //Add the new interval with summary value to the summary signal
            summary.put(window, summaryValue);
            
            if(end.compareTo(original.end()) > 0)
                stop = true;
        }
        
        return summary;
    }
}
