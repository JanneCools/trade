package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.statistics.Statistics;

public class BinomialDistribution implements IntDistribution
{
    private final int n;
    
    private final double p;

    public BinomialDistribution(int n, UnitScore p) {
        this.n = n;
        this.p = p.getValue();
    }

    @Override
    public UnitScore probability(Integer k)
    {
        long coefficient =
            Statistics.factorial(n) /
            (Statistics.factorial(k) * Statistics.factorial(n-k));

        return new UnitScore(coefficient * Math.pow(p, k) * Math.pow(1.0 - p, n-k));
    }
    
}
