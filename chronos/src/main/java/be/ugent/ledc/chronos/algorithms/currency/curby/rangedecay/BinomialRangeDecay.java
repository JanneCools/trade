package be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.Objects;

/**
 * A decay model assuming that a change in [0,r] is binomially distributed
 * with parameter p.
 * @author abronsel
 */
public class BinomialRangeDecay implements RangeDecay
{
    private final UnitScore p;

    public BinomialRangeDecay(UnitScore p)
    {
        this.p = p;
    }


    @Override
    public UnitScore probability(long k, long n) {
        return k<0 || k>n
            ? UnitScore.ZERO
            : new UnitScore(
                Math.pow(p.getValue(), k) * Math.pow(1.0 - p.getValue(), n-k) * Statistics.factorial(n)
                /
                (Statistics.factorial(k) * Statistics.factorial(n-k))
            );
    }
    
    @Override
    public String toString()
    {
        return "binomial(" + p.toString() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.p);
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
        final BinomialRangeDecay other = (BinomialRangeDecay) obj;
        return Objects.equals(this.p, other.p);
    }
    
    
}