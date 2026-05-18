package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;

/**
 * Models geometric distribution, typically used as age prior.
 * @author abronsel
 */
public class GeometricDistribution implements IntDistribution
{
    /**
     * Models the parameter of the geometric distribution
     */
    private final double p;

    public GeometricDistribution(UnitScore p)
    {
        this.p = p.getValue();
    }

    @Override
    public UnitScore probability(Integer k)
    {
        if(k == null || k<=0)
            return UnitScore.ZERO;
        
        return new UnitScore(p * Math.pow(1.0 - p, k-1));
    }
    
}
