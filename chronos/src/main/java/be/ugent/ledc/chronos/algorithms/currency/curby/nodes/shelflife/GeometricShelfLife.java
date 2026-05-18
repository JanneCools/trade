package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.shelflife;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.GeometricDistribution;
import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.IntDistribution;
import be.ugent.ledc.core.operators.UnitScore;

/**
 * A shelf-life node where the underlying age distribution is geometric, with 
 * parameter p.
 * @author abronsel
 * @param <T> 
 */
public class GeometricShelfLife<T> extends ShelfLife<T>
{
    private UnitScore p;
    
    public GeometricShelfLife(String attribute, UnitScore p)
    {
        super(attribute, new GeometricDistribution(p));
        this.p = p;
    }

    @Override
    public IntDistribution getPriorModel()
    {
        return new GeometricDistribution(p);
    }

    public UnitScore getP()
    {
        return p;
    }

    public void setP(UnitScore p)
    {
        this.p = p;
    }

    @Override
    public String getType() {
        return "geometric";
    }
}
