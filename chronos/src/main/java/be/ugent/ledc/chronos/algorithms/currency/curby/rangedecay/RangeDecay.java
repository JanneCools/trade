package be.ugent.ledc.chronos.algorithms.currency.curby.rangedecay;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.stream.LongStream;

/**
 * A RangeDecay model describes the distribution of changes
 * in the range of a change constraint.
 * @author abronsel
 */
public interface RangeDecay
{
    /**
     * Gives the probability that a change happens at time t in an interval [0,r]
     * @param t Time of the change
     * @param r Size of the interval
     * @return Probability that the change happens at time t.
     */
    public UnitScore probability(long t, long r);
    
    /**
     * Gives the probability that a change has not yet happened at time t in an interval [0,r].
     * This thus gives the value of the tail function for the corresponding probability model.
     * @param t Time of interest
     * @param r Size of the interval
     * @return Probability that the change has not yet happened at time t.
     */
    public default UnitScore currentAt(long t, long r)
    {
        if(t<0)
            return UnitScore.ONE;
        if(t>r)
            return UnitScore.ZERO;
        return 
            LongStream
                .rangeClosed(0, t)
                .mapToObj(l -> probability(l, r))
                .reduce(UnitScore.ZERO, UnitScore::plus)
                .getComplement();
    }
}

