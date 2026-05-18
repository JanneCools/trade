package be.ugent.ledc.core.operators.quantifier;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.stream.Stream;

public class QuantifierFactory
{
    /**
     * Produces a new quantifier that is the pointwise minimal of a series of given
     * quantifiers.
     * @param quantifiers
     * @return 
     */
    public static Quantifier and(Quantifier ...quantifiers)
    {
        return u -> Stream
            .of(quantifiers)
            .map(quan -> quan.apply(u))
            .reduce(UnitScore.ONE, UnitScore::min);
    }
    
    /**
     * Produces a new quantifier that is the pointwise maximal of a series of given
     * quantifiers.
     * @param quantifiers
     * @return 
     */
    public static Quantifier or(Quantifier ...quantifiers)
    {
        return u -> Stream
            .of(quantifiers)
            .map(quan -> quan.apply(u))
            .reduce(UnitScore.ZERO, UnitScore::max);
    }
    
    /**
     * Produces a new quantifier by shifting an existing quantifier to the left.
     * This means, in the interval [1-shift, 1], the quantifier produces 1.
     * If the squeeze flag is true, the original quantifier will be "squeezed" into
     * the interval [0, 1-shift]. Else, the original quantifier is just shifted to the left
     * for the interval [0, 1-shift].
     * @param quantifier
     * @param shift
     * @param squeeze
     * @return 
     */
    public static Quantifier shiftLeft(Quantifier quantifier, UnitScore shift, boolean squeeze)
    {
        return u ->
            UnitScore.isZero(u)
                ? UnitScore.ZERO
                : u.compareTo(shift.getComplement()) >= 0
                    ? UnitScore.ONE
                    : squeeze
                        ? quantifier.apply(new UnitScore(u.getValue()/shift.getComplement().getValue()))
                        : quantifier.apply(UnitScore.plus(u, shift));
            
    }
    
    /**
     * Produces a new quantifier by shifting an existing quantifier to the right.
     * This means, in the interval [0, shift], the quantifier produces 0.
     * If the squeeze flag is true, the original quantifier will be "squeezed" into
     * the interval [shift, 1]. Else, the original quantifier is just shifted to the right
     * for the interval [shift, 1].
     * @param quantifier
     * @param shift
     * @param squeeze
     * @return 
     */
    public static Quantifier shiftRight(Quantifier quantifier, UnitScore shift, boolean squeeze)
    {
        return u ->
            UnitScore.isOne(u)
                ? UnitScore.ONE
                : u.compareTo(shift) <= 0
                    ? UnitScore.ZERO
                    : squeeze
                        ? quantifier.apply(new UnitScore((u.getValue() - shift.getValue())/shift.getComplement().getValue()))
                        : quantifier.apply(UnitScore.minus(u, shift));
            
    }
}
