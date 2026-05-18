package be.ugent.ledc.core.dataset.contractors;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.function.Predicate;

public abstract class TypeContractor<T> extends Contractor
{
    public TypeContractor(Predicate<Object> embeddedPredicate)
    {
        super(embeddedPredicate);
    }

    public abstract T getFromDataObject(DataObject o, String attribute);

    public T get(T current) {
        return current;
    }
    
    public abstract String name();
    
    public abstract String toPersistentDatatype(boolean verbose);
    
    public void refine(Predicate refinement)
    {
        super.and(refinement);
    }
}
