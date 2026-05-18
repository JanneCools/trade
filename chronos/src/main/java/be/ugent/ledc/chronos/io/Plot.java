package be.ugent.ledc.chronos.io;

import be.ugent.ledc.chronos.datastructures.Signal;

public class Plot<I extends Comparable<? super I>, N extends Number>
{
    private final Signal<I,N> signal;
    
    private final PlotOptions options;

    public Plot(Signal<I, N> signal, PlotOptions options)
    {
        this.signal = signal;
        this.options = options;
    }

    public Signal<I, N> getSignal() {
        return signal;
    }

    public PlotOptions getOptions() {
        return options;
    }
    
}
