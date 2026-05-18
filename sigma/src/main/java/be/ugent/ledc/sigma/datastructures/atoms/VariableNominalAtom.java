package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import java.util.Set;
import java.util.function.Function;

import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;

public class VariableNominalAtom<T extends Comparable<? super T>> extends VariableAtom<T, NominalContractor<T>, EqualityOperator> {

    /**
     * Create a new VariableNominalAtom object.
     *
     * @param contractor     NominalContractor object used by this atom during evaluation
     * @param leftAttribute  Left attribute of which the value will be used during evaluation
     * @param operator       EqualityOperator object used during evaluation
     * @param rightAttribute Right attribute of which the value will be used during evaluation
     */
    public VariableNominalAtom(NominalContractor<T> contractor, String leftAttribute, EqualityOperator operator, String rightAttribute) {
        super(contractor, leftAttribute, operator, rightAttribute);
    }

    @Override
    public boolean isAlwaysTrue() {
        return getLeftAttribute().equals(getRightAttribute()) && (getOperator().equals(EqualityOperator.EQ));
    }

    @Override
    public boolean isAlwaysFalse() {
        return getLeftAttribute().equals(getRightAttribute()) && getOperator().equals(EqualityOperator.NEQ);
    }

    @Override
    public AbstractAtom<?, ?, ?> fixRight(DataObject o)
    {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getRightAttribute()))
        {
            return new ConstantNominalAtom<>(
                    getContractor(),
                    getLeftAttribute(), //Left attribute
                    getOperator(), //Same operator
                    (T)o.get(getRightAttribute())); //Constant value for right attribute
        }

        return this;
    }

    @Override
    public AbstractAtom<?, ?, ?> fixLeft(DataObject o)
    {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getLeftAttribute()))
        {
            return new ConstantNominalAtom<>(
                    getContractor(),
                    getRightAttribute(), //Right attribute
                    (EqualityOperator)getOperator().getReversedOperator(), //Reverse order operator
                    (T)o.get(getLeftAttribute())); //Constant value for left attribute
        }

        return this;
    }

    @Override
    public AbstractAtom<?, ?, ?> fix(DataObject o)
    {
        Set<String> attr = o.getAttributes();
        
        //If none of the attributes are present in o, we just return this attribute
        if(getAttributes().noneMatch(attr::contains))
            return this;
        else if(!attr.contains(getLeftAttribute()) && attr.contains(getRightAttribute()))
        {
            return new ConstantNominalAtom<>(
                getContractor(),
                getLeftAttribute(), //Left attribute
                getOperator(), //Same operator
                (T)o.get(getRightAttribute())); //Constant value for right attribute
        }
        else if(attr.contains(getLeftAttribute()) && !attr.contains(getRightAttribute()))
        {
            return new ConstantNominalAtom<>(
                getContractor(),
                getRightAttribute(), //Right attribute
                (EqualityOperator)getOperator().getReversedOperator(), //Reverse order operator
                (T)o.get(getLeftAttribute())); //Constant value for left attribute
        }
        else
            return test(o) ? AbstractAtom.ALWAYS_TRUE : AbstractAtom.ALWAYS_FALSE;
    }
    
    @Override
    public VariableNominalAtom<?> getInverse() {
        return new VariableNominalAtom<>(
            this.getContractor(),
            this.getLeftAttribute(),
            (EqualityOperator) this.getOperator().getInverseOperator(),
            this.getRightAttribute());
    }

    @Override
    public VariableNominalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new VariableNominalAtom<>(
                getContractor(),
                nameTransform.apply(getLeftAttribute()),
                getOperator(),
                nameTransform.apply(getRightAttribute())
        );
    }


    @Override
    public VariableAtom<T, NominalContractor<T>, EqualityOperator> flip()
    {
        return new VariableNominalAtom<>(
            getContractor(),
            getRightAttribute(),
            (EqualityOperator)getOperator().getReversedOperator(),
            getLeftAttribute()
        );
    }

}
