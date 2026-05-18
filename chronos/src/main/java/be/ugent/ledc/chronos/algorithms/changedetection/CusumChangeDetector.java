package be.ugent.ledc.chronos.algorithms.changedetection;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.transform.cusum.CusumTransform;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CusumChangeDetector<I extends Comparable<? super I>, N extends Number> implements ChangePointDetector<I, N>
{    
    private final double multiplier;
    
    private final CusumTransform transform;
    
    public CusumChangeDetector(CusumTransform transform, double multiplier)
    {
        this.multiplier = multiplier;
        this.transform = transform;
    }
    
    @Override
    public List<I> changes(Signal<I,N> signal) throws ChronosException
    {
        //Retrieve the cusum signal
        Signal<I,Double> cusumSignal = transform.transform(signal);
        
        List<I> peaks = new ArrayList<>();
        
        List<Double> values = cusumSignal
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .map(e -> e.getValue())
            .collect(Collectors.toList());
        
        //Find the quartiles
        HashMap<String, Double> quartiles = Statistics.quartiles(values);
        
        //Get the window used for the transform
        int w = transform.getWindow();
                
        double threshold = quartiles.get("Q2") + (quartiles.get("Q3") - quartiles.get("Q2")) * multiplier;
        
        System.out.println("Threshold: " + threshold);
        
        for(I idx: cusumSignal.indexSet())
        {
            Double v = cusumSignal.valueAt(idx);
            
            //Is the v large enough?
            if(v < threshold)
                continue;
            
            boolean peak = true;
            
            for(int i=1; i<=w && peak; i++)
            {
                I leftIdx = cusumSignal.jumpLeft(idx, i);
                
                if(leftIdx != null && cusumSignal.valueAt(leftIdx) > v)
                    peak = false;
                
                I rightIdx = cusumSignal.jumpRight(idx, i);
                
                if(rightIdx != null && cusumSignal.valueAt(rightIdx) > v)
                    peak = false;
            }
            
            if(peak)
                peaks.add(idx);
        }
        
        return peaks;
    }
}