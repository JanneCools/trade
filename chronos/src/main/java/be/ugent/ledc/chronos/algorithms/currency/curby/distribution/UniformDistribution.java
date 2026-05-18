package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;

public class UniformDistribution implements IntDistribution
{
    private final int lowerBound;
    
    private final int upperBound;

    public UniformDistribution(int lowerBound, int upperBound)
    {
        if(upperBound < lowerBound)
            throw new RuntimeException("Invalid uniform distribution for lower bound "
                + lowerBound
                + " and upper bound "
                + upperBound);
        
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
    
    @Override
    public UnitScore probability(Integer event)
    {
        if(event == null || event < lowerBound || event > upperBound)
            return UnitScore.ZERO;
        
        return UnitScore.fromFraction(1, upperBound - lowerBound + 1);
    }
    
}
