package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.operators.quantifier.Quantifier;
import java.util.Objects;

public abstract class QuantifiedAggregation implements Aggregator<UnitScore>
{
    private final Quantifier quantifier;

    public QuantifiedAggregation(Quantifier quantifier)
    {
        this.quantifier = quantifier;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.quantifier);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QuantifiedAggregation other = (QuantifiedAggregation) obj;
        return Objects.equals(this.quantifier, other.quantifier);
    }
    
    
}
