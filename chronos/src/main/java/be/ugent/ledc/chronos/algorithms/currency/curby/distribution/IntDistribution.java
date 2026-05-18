package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.stream.IntStream;

public interface IntDistribution extends Distribution<Integer>{
    
    public default UnitScore tail(Integer event)
    {
        if(event == null)
            return UnitScore.ZERO;
        
        return IntStream
            .rangeClosed(0, event)
            .mapToObj(this::probability)
            .reduce(UnitScore.ZERO, UnitScore::plus)
            .getComplement();
    }
    
}
