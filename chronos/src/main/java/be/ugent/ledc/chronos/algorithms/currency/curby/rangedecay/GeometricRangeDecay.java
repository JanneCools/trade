package be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.Objects;

/**
 * A decay model assuming that a change in [0,r] has a truncated geometrical distribution.
 * @author abronsel
 */
public class GeometricRangeDecay implements RangeDecay
{
    private final UnitScore p;

    public GeometricRangeDecay(UnitScore p) throws ChronosException
    {
        this.p = p;
        
        if(p.equals(UnitScore.ZERO))
            throw new ChronosException("Parameter 0 not allowed for a geometric distribution.");
    }
    
    @Override
    public UnitScore probability(long t, long r)
    {
        return t<0 || t>r
            ? UnitScore.ZERO
            : new UnitScore(
                Math.pow(p.getComplement().getValue(), t) * p.getValue()
            / (1.0 - Math.pow(1.0 - p.getValue(), r + 1))
            );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.p);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeometricRangeDecay other = (GeometricRangeDecay) obj;
        return Objects.equals(this.p, other.p);
    }
    
    @Override
    public String toString()
    {
        return "geometric(" + p.toString() + ")";
    }
}
