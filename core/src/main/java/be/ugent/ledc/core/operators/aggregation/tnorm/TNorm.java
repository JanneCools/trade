package be.ugent.ledc.core.operators.aggregation.tnorm;

import be.ugent.ledc.core.operators.aggregation.AssociativeAggregator;
import be.ugent.ledc.core.operators.UnitScore;

/**
 * Models a triangular norm or t-norm aggregator. This is any aggregator
 * operating on the unit interval that is monotone, commutative, associative and where
 * 1 acts as the identity element.
 * @author abronsel
 */
public interface TNorm extends AssociativeAggregator<UnitScore>{}
