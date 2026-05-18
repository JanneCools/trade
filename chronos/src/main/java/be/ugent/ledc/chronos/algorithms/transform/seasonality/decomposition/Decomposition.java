package be.ugent.ledc.chronos.algorithms.transform.seasonality.decomposition;

import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.Objects;

public class Decomposition<I extends Comparable<? super I>>
{
    public final Signal<I, Double> trend;
    public final Signal<I, Double> seasonal;
    public final Signal<I, Double> noise;

    public Decomposition(Signal<I, Double> trend, Signal<I, Double> seasonal, Signal<I, Double> noise)
    {
        this.trend = trend;
        this.seasonal = seasonal;
        this.noise = noise;
    }

    public Signal<I, Double> getTrend()
    {
        return trend;
    }

    public Signal<I, Double> getSeasonal()
    {
        return seasonal;
    }

    public Signal<I, Double> getNoise()
    {
        return noise;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.trend);
        hash = 37 * hash + Objects.hashCode(this.seasonal);
        hash = 37 * hash + Objects.hashCode(this.noise);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Decomposition other = (Decomposition) obj;
        if (!Objects.equals(this.trend, other.trend))
        {
            return false;
        }
        if (!Objects.equals(this.seasonal, other.seasonal))
        {
            return false;
        }
        if (!Objects.equals(this.noise, other.noise))
        {
            return false;
        }
        return true;
    }
    
    
}
