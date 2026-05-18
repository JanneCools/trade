package be.ugent.ledc.core.statistics;

import be.ugent.ledc.core.dataset.ContractedDataset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class Statistics
{
    public static Double mean(List<Double> values)
    {
        return values
            .stream()
            .mapToDouble(d->d)
            .summaryStatistics()
            .getAverage();
    }
    
    public static Double median(List<Double> data)
    {
        Collections.sort(data);
        
        return (data.size() % 2 == 1)
            ? data.get(data.size()/2)
            : (data.get(data.size()/2) + data.get(data.size()/2 - 1))/2;
    }
    
    public static long longMedian(List<Long> data)
    {
        Collections.sort(data);
        
        return (data.size() % 2 == 1)
            ? data.get(data.size()/2)
            : (data.get(data.size()/2) + data.get(data.size()/2 - 1))/2;
    }
     
    public static Double variance(List<Double> values)
    {
        double mean = mean(values);
        
        return values
            .stream()
            .mapToDouble(v -> Math.pow(v - mean, 2.0))
            .sum() / ((double)values.size() - 1.0);
    }

    public static Double stdDev(List<Double> values)
    {
        return Math.sqrt(variance(values));
    }
    
    public static double log(double d, double base)
    {
        return Math.log10(d)/Math.log10(base);
    }
    
    public static double log2(double d)
    {
        return log(d, 2.0);
    }
    
    public static HashMap<String, Double> quartiles(List<Double> values)
    {
        HashMap<String, Double> quartileMap = new HashMap<>();
        
        List<Double> sorted = values.stream().sorted().toList();
        
        int q1 = (int)((values.size()-1.0)/ 4.0);
        int q2 = (int)((values.size()-1.0)/ 2.0);
        int q3 = (int)((values.size()-1.0) * 3.0 / 4.0);
        
        quartileMap.put("Q1", sorted.get(q1));
        quartileMap.put("Q2", sorted.get(q2));
        quartileMap.put("Q3", sorted.get(q3));
        
        return quartileMap;
    }
    
    public static Double interQuartileRange(List<Double> values)
    {
        Map<String, Double> quartiles = quartiles(values);
        if(quartiles.isEmpty())
            return null;
        
        return quartiles.get("Q3") - quartiles.get("Q1");
    }
    
    //
    // Methods acting on contracted datasets
    //
    
    public static Double range(ContractedDataset data, String a)
    {
        List<Double> values = data
        .stream()
        .filter(o -> o.get(a) != null)
        .map(o -> ((Number)o.get(a)).doubleValue())
        .sorted()
        .collect(Collectors.toList());
        
        if(values.isEmpty())
            return null;
                    
        return values.get(values.size()-1) - values.get(0);
    }
    
    public static Double interQuartileRange(ContractedDataset data, String a)
    {
        Map<String, Double> quartiles = quartiles(data, a);
        if(quartiles.isEmpty())
            return null;
        
        return quartiles.get("Q3") - quartiles.get("Q1");
    }
    
    public static Map<String, Double> quartiles(ContractedDataset data, String a)
    {
        List<Double> values = data
            .stream()
            .filter(o -> o.get(a) != null)
            .map(o -> ((Number)o.get(a)).doubleValue())
            .sorted()
            .collect(Collectors.toList());
        
        if(values.isEmpty())
            return new HashMap<>();
        
        return quartiles(values);
    }
    
    public static long factorial(long n)
    {
        
        return n == 0
            ? 1l
            : LongStream
            .rangeClosed(1, n)
            .reduce((k,m)->k*m)
            .getAsLong();
    }
}
