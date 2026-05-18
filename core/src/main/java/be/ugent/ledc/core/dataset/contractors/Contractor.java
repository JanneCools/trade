package be.ugent.ledc.core.dataset.contractors;

import java.util.function.Predicate;

public class Contractor
{
    private Predicate<Object> embeddedPredicate;

    public Contractor(Predicate<Object> embeddedPredicate)
    {
        this.embeddedPredicate = embeddedPredicate;
    }

    public Predicate<Object> getEmbeddedPredicate()
    {
        return embeddedPredicate;
    }
    
    public boolean test(Object o)
    {
        return embeddedPredicate == null || embeddedPredicate.test(o);
    }
    
    protected void and(Predicate other)
    {
        if(embeddedPredicate == null)
            embeddedPredicate = other;
        
        this.embeddedPredicate = embeddedPredicate.and(other);
                    
    }
    
    protected void or(Predicate other)
    {
        if(embeddedPredicate == null)
            embeddedPredicate = other;
        
        this.embeddedPredicate = embeddedPredicate.or(other);
                    
    }
    
    protected void negate()
    {
        if(embeddedPredicate != null)
            this.embeddedPredicate = embeddedPredicate.negate();    
    }
}
