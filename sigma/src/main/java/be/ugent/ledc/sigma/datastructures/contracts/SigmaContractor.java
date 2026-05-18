package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.ConstantAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.VariableAtom;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SigmaContractor<T extends Comparable<? super T>> extends TypeContractor<T>
{
    private final TypeContractor<T> embeddedContractor;
    
    public static final char CONSTANT_SEPARATOR = ',';

    public SigmaContractor(TypeContractor<T> embeddedContractor)
    {
        super(embeddedContractor.getEmbeddedPredicate());
        this.embeddedContractor = embeddedContractor;
    }

    @Override
    public T getFromDataObject(DataObject o, String attribute)
    {
        return embeddedContractor.getFromDataObject(o, attribute);
    }

    @Override
    public String name()
    {
        return embeddedContractor.name();
    }

    @Override
    public boolean test(Object t)
    {
        return embeddedContractor.test(t);
    }

    @Override
    public String toPersistentDatatype(boolean verbose)
    {
        return embeddedContractor.toPersistentDatatype(verbose);
    }
    
    public abstract T parseConstant(String constantAsString);
    
    public abstract String printConstant(T value);
    
    public Set<T> parseSetOfConstants(String setAsString)
    {
        return Stream.of(setAsString
            .trim()
            .substring(1, setAsString.length()-1)
            .split(""+CONSTANT_SEPARATOR))
            .map(String::trim)
            .map(this::parseConstant)
            .collect(Collectors.toSet());
    }
    
    public String printSetOfConstants(Set<T> setOfConstants)
    {
        return setOfConstants
        .stream()
        .map(this::printConstant)
        .collect(Collectors.joining(""+CONSTANT_SEPARATOR, "{", "}"));
    }

    public SetAtom<T, ? extends SigmaContractor<T>> buildSetAtom(String attribute, SetOperator op, T ... constants)
    {
        return buildSetAtom(attribute, op, SetOperations.set(constants));
    }

    public boolean isBefore(T v1, T v2)
    {
        if(v1 == null || v2 == null)
            throw new NullPointerException("Cannot compare null values");
        
        return get(v1).compareTo(get(v2)) < 0;
    }
    
    public boolean isAfter(T v1, T v2)
    {
        if(v1 == null || v2 == null)
            throw new NullPointerException("Cannot compare null values");
        
        return get(v1).compareTo(get(v2)) > 0;
    }
    
    public boolean isBeforeOrEqual(T v1, T v2)
    {
        if(v1 == null || v2 == null)
            throw new NullPointerException("Cannot compare null values");
        
        return get(v1).compareTo(get(v2)) <= 0;
    }
    
    public boolean isAfterOrEqual(T v1, T v2)
    {
        if(v1 == null || v2 == null)
            throw new NullPointerException("Cannot compare null values");
        
        return get(v1).compareTo(get(v2)) >= 0;
    }

    public abstract SetAtom<T, ? extends SigmaContractor<T>> buildSetAtom(String attribute, SetOperator op, Set<T> constants);

    public abstract ConstantAtom<T, ? extends SigmaContractor<T>, ? extends ComparableOperator> buildConstantAtom(String attribute, ComparableOperator op, T constant);

    public abstract VariableAtom<T, ? extends SigmaContractor<T>, ? extends ComparableOperator> buildVariableAtom(String leftAttribute, ComparableOperator op, String rightAttribute);

}
