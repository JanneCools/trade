package core.statistics;

public interface ProbabilityDistribution<E>
{
    public double probability(E event);
}
