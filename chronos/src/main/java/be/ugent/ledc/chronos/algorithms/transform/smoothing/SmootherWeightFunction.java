package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import java.util.function.Function;

/**
 * Computes the weights of smoothing functions.
 * @author abronsel
 */
public interface SmootherWeightFunction extends Function<Double, Double>{}
