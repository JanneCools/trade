package be.ugent.ledc.chronos.algorithms.reliability;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.TimePoint;
import be.ugent.ledc.core.RepairException;

import java.time.temporal.Temporal;
import java.util.Set;

public interface RefPointReliabilityEstimator<T extends Temporal> {

    double getReliabilityScore(TimePoint<T> current, Set<TimePoint<T>> refPoints) throws RepairException, ChronosException;

}
