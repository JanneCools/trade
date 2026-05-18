package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;

public interface Distribution<E>
{    
    public UnitScore probability(E event);
}
