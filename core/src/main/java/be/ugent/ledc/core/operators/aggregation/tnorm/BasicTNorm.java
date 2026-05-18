package be.ugent.ledc.core.operators.aggregation.tnorm;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.function.BiFunction;

/**
 * An enum that lists the most common triangular norms.
 * @author abronsel
 */
public enum BasicTNorm implements TNorm
{
    /**
     * The minimum T-norm: T(a,b) = min(a,b)
     */
    MINIMUM((UnitScore a, UnitScore b) -> UnitScore.min(a,b)),
    /**
     * The Hamacher product: T(a,b) = a*b /( a + b - a*b)
     */
    HAMACHER_PRODUCT((UnitScore a, UnitScore b) -> UnitScore.max(a,b).compareTo(UnitScore.ZERO) == 0 ? UnitScore.ZERO : new UnitScore( (a.getValue() * b.getValue()) / (a.getValue() + b.getValue() - a.getValue() * b.getValue()))),
    /**
     * The product T-norm: T(a,b) = a*b
     */
    PRODUCT((UnitScore a, UnitScore b) -> UnitScore.times(a, b)), 
    /**
     * The Lukasiewics T-norm: T(a,b) = max(0, a + b - 1)
     */
    LUKASIEWICS((UnitScore a, UnitScore b) -> new UnitScore(Math.max(a.getValue() + b.getValue() - 1.0, 0.0))),
    /**
     * The drastic T-norm: T(a,b) = max(0, a + b - 1)
     */
    DRASTIC((UnitScore a, UnitScore b) ->  UnitScore.isOne(UnitScore.max(a,b)) ? UnitScore.min(a, b) : UnitScore.ZERO);
    
    private final BiFunction<UnitScore, UnitScore, UnitScore> calculator;

    private BasicTNorm(BiFunction<UnitScore, UnitScore, UnitScore> calculator)
    {
        this.calculator = calculator;
    }

    @Override
    public UnitScore aggregate(UnitScore argument1, UnitScore argument2) {
        return this.calculator.apply(argument1, argument2);
    }
}
