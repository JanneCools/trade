package be.ugent.ledc.chronos.algorithms.transform.summarisation;

public class SampleCounter<I extends Comparable<? super I>, T> extends SignalSummarizer<I, T, Long>
{
    public SampleCounter(int windowSizeInUnits)
    {
        super((list) -> (long)list.size(), windowSizeInUnits);
    }
    
}
