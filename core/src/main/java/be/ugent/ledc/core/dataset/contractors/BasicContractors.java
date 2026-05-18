package be.ugent.ledc.core.dataset.contractors;

import java.util.Objects;
import java.util.function.Predicate;

public enum BasicContractors implements Predicate
{
    NOT_NULL(Objects::nonNull),
    UNBOUND((o) -> true);
    
    private final Predicate embeddedPredicate;
    
    BasicContractors(Predicate embeddedPredicate)
    {
        this.embeddedPredicate = embeddedPredicate;
    }

    @Override
    public boolean test(Object t)
    {
        return embeddedPredicate.test(t);
    }
}
