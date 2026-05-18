package be.ugent.ledc.core.operators.quantifier;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.function.Function;

public enum BasicQuantifier implements Quantifier
{
    ALL(x -> UnitScore.isOne(x) ? UnitScore.ONE : UnitScore.ZERO),
    MOST(x -> new UnitScore(Math.pow(x.getValue(), 4))),
    MANY (UnitScore::squared),
    AVERAGE (x -> x),
    SOME(UnitScore::sqrt),
    FEW(x -> new UnitScore(Math.pow(x.getValue(), 0.25))),
    ANY(x -> UnitScore.isZero(x) ? UnitScore.ZERO : UnitScore.ONE);

    private final Function<UnitScore, UnitScore> embeddedFunction;

    private BasicQuantifier(Function<UnitScore, UnitScore> embeddedFunction)
    {
        this.embeddedFunction = embeddedFunction;
    }

    @Override
    public UnitScore apply(UnitScore t)
    {
        return this.embeddedFunction.apply(t);
    }
    
}
