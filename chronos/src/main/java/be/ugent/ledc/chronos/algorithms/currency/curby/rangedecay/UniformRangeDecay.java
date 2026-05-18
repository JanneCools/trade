package be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay;

import be.ugent.ledc.core.operators.UnitScore;

/**
* A decay model assuming that a change in [0,r] is uniformly distributed.
*/
public class UniformRangeDecay implements RangeDecay
{
    @Override
    public UnitScore probability(long t, long r)
    {
        return t < 0 || t > r
            ? UnitScore.ZERO
            : UnitScore.fromFraction(1, (int)(r+1));
    }
    
    @Override
    public String toString()
    {
        return "uniform";
    }
    
    @Override
    public int hashCode()
    {
        return 7;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return getClass().equals(obj.getClass());
    }
}
