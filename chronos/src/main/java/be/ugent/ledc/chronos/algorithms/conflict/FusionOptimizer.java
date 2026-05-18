package be.ugent.ledc.chronos.algorithms.conflict;

import java.util.List;

public interface FusionOptimizer<T>
{
    public List<Fusion<T>> getOptimalFusions(ConflictGraph<T> graph);
}
