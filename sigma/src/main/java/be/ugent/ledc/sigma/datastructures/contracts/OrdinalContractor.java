package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.Set;

public abstract class OrdinalContractor<T extends Comparable<? super T>> extends SigmaContractor<T> implements
        ForwardNavigator<T>,
        BackwardNavigator<T>,
        Additive<T>
{   

    public OrdinalContractor(TypeContractor<T> embeddedContractor)
    {
        super(embeddedContractor);
    }

    public abstract long cardinality(Interval<T> range);

    public Long differenceInUnits(T a, T b) {
        if (a == null || b == null)
            return null;

        return isBefore(a, b)
                ? - (cardinality(Interval.closed(get(a), get(b))) - 1L)
                : cardinality(Interval.closed(get(b), get(a))) - 1L;
    }

    public Long absDifferenceInUnits(T a, T b)
    {
        if(a == null || b == null)
            return null;

        return isBefore(a, b)
            ? cardinality(Interval.closed(get(a), get(b))) - 1L
            : cardinality(Interval.closed(get(b), get(a))) - 1L;
    }

    @Override
    public String printConstant(T value)
    {
        return value == null ? "" : get(value).toString();
    }

    @Override
    public SetOrdinalAtom<T> buildSetAtom(String attribute, SetOperator op, Set<T> constants)
    {
        return new SetOrdinalAtom<>(this, attribute, op, constants);
    }

    @Override
    public ConstantAtom<T, ? extends SigmaContractor<T>, ? extends ComparableOperator> buildConstantAtom(String attribute, ComparableOperator op, T constant)
    {
        return new ConstantOrdinalAtom<>(this, attribute, op, constant);
    }

    @Override
    public VariableAtom<T, ? extends SigmaContractor<T>, ? extends ComparableOperator> buildVariableAtom(String leftAttribute, ComparableOperator op, String rightAttribute)
    {
        return new VariableOrdinalAtom<>(this, leftAttribute, op, rightAttribute);
    }

    public VariableAtom<T, ? extends SigmaContractor<T>, ? extends ComparableOperator> buildVariableAtom(String leftAttribute, ComparableOperator op, String rightAttribute, Integer offset)
    {
        return new VariableOrdinalAtom<>(this, leftAttribute, op, rightAttribute, offset);
    }


}
