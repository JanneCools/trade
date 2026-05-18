package be.ugent.ledc.core.operators.aggregation.tnorm;

import be.ugent.ledc.core.operators.UnitScore;

/**
 * A factory that can produce the Schweizer-Sklar family of triangular norms.
 * @author abronsel
 */
public class SchweizerSklarFactory
{
    public static TNorm create(Double p)
    {
        if(p == null)
            return null;
        
        if(p.equals(Double.NEGATIVE_INFINITY))
            return BasicTNorm.MINIMUM;
        
        if(p.equals(Double.POSITIVE_INFINITY))
            return BasicTNorm.DRASTIC;
        
        if(Math.abs(p) < Double.MIN_VALUE)
            return BasicTNorm.PRODUCT;
        
        if(p < 0.0)
            return (UnitScore a, UnitScore b) -> new UnitScore(Math.pow( Math.pow(a.getValue(), p) + Math.pow(b.getValue(), p) - 1.0, 1.0/p));
        else
            return (UnitScore a, UnitScore b) -> new UnitScore(  Math.pow( Math.max(0.0, Math.pow(a.getValue(), p) + Math.pow(b.getValue(), p) - 1.0), 1.0/p ));
    }
}
