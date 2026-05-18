package be.ugent.ledc.core.statistics;

public interface ProbabilityDistribution<E>
{
    public double probability(E event);
}
