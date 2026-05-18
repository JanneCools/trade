package be.ugent.ledc.core.operators.quantifier;

import be.ugent.ledc.core.operators.UnitScore;

public class AtLeastQuantifier implements Quantifier
{
    private final UnitScore threshold;

    public AtLeastQuantifier(UnitScore threshold)
    {
        this.threshold = threshold;
    }

    @Override
    public UnitScore apply(UnitScore t)
    {
        return t.compareTo(threshold) >= 0 ? UnitScore.ONE : UnitScore.ZERO;
    }
}
