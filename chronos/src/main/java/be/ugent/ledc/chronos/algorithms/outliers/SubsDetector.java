package be.ugent.ledc.chronos.algorithms.outliers;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.dataset.Contract;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Based on the observations of TimeEval study (VLDB'2022) this detector implements
 * the simple notion of a subsequence-based detector.In simple terms, this
 * detector splits a time series into subsequences and applies an algorithm for
 * outlier detection on these sequences.
 * @author abronsel
 * @param <I>
 */
public abstract class SubsDetector<I extends Comparable<? super I>> implements IOutlierDetector<I,Double>
{
    private final int window;
    
    private final double scoreThreshold;

    public SubsDetector(int window, double scoreThreshold) {
        this.window = window;
        this.scoreThreshold = scoreThreshold;
    }

    public int getWindow() {
        return window;
    }

    @Override
    public List<I> findOutliers(Signal<I, Double> signal) throws ChronosException
    {
        if(window - 1 < signal.size())
            return new ArrayList<>();
        
        //Convert
        I wStart    = signal.start();
        I wEnd      = signal.jumpRight(signal.start(), window - 1);
        
        String time = "time";
        
        Contract.ContractBuilder builder = new Contract.ContractBuilder();
        
        builder.addContractor(time, signal.getIndexContractor());
        
        for(int i=1; i<=window; i++)
        {
            builder.addContractor(name(i), TypeContractorFactory.DOUBLE);
        }
        
        ContractedDataset data = new ContractedDataset(builder.build());
        
        
        //We now convert sub-signals from the given signal to a contracted
        //dataset format
        boolean stop = false;
        
        while(!stop)
        {
            if(wEnd == null)
                stop = true;
            
            Signal<I,Double> subSignal = signal.subSignal(wStart, wEnd);
            
            DataObject o = new DataObject().set(time, subSignal.start());
            int c = 1;
            
            for(I idx: subSignal.indexSet())
            {
                o.set(name(c++), subSignal.get(idx));
            }
            
            data.addDataObject(o);
            
            wStart = signal.nextIndex(wStart);
            wEnd = signal.nextIndex(wEnd);
        }
        
        //Computer scores for the sub signals: this is done by a delegate algorithm
        Map<DataObject, Double> subSignalScores = processSubSignals(
            data,
            IntStream
                .rangeClosed(1, window)
                .mapToObj(this::name)
                .collect(Collectors.toSet())
        );
        
        //Initialize a mapping to keep scores for time values
        Map<I,List<Double>> pointScoreMap = signal
            .indexStream()
            .collect(Collectors.toMap(i -> i,i -> new ArrayList<>()));
        
        for(DataObject o: subSignalScores.keySet())
        {
            //Get the starting point and its index
            I start = signal
                .getIndexContractor()
                .getFromDataObject(o, time);
            
            //Get the outlier score
            Double score = subSignalScores.get(o);
            
            //Add scores
            for(int i=0;i<window;i++)
            {
                pointScoreMap
                    .get(signal.jumpRight(start, i))
                    .add(score);
            }
        }
        
        return pointScoreMap
            .entrySet()
            .stream()
            .filter(e -> e
                .getValue()
                .stream()
                .mapToDouble(d->d)
                .average().getAsDouble() >= getScoreThreshold())
            .map(e->e.getKey())
            .toList();        
    }
 
    private String name(int i)
    {
        return "x" + i;
    }
    
    public abstract Map<DataObject, Double> processSubSignals(ContractedDataset d, Set<String> attributes);

    protected double getScoreThreshold()
    {
        return scoreThreshold;
    }
}
