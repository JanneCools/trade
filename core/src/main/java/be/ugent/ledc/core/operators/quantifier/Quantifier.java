package be.ugent.ledc.core.operators.quantifier;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.function.Function;

/**
 * A Quantifier is a function that models some notion of quantity by mapping
 * each value in the unit interval to a corresponding membership degree for that quantity.
 * 
 * @author abronsel
 */
public interface Quantifier extends Function<UnitScore, UnitScore>{}
