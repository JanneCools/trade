package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.InequalityOperator;
import java.util.function.Function;

import java.util.*;

public class VariableOrdinalAtom<T extends Comparable<? super T>> extends VariableAtom<T, OrdinalContractor<T>, ComparableOperator> {

    private final Integer rightConstant;

    /**
     * Create a new VariableOrdinalAtom object with form leftAttribute operator rightAttribute + constant (e.g. a <= b + 2)
     *
     * @param contractor     OrdinalContractor object used by this atom during evaluation
     * @param leftAttribute  Left attribute of which the value will be used during evaluation
     * @param operator       ComparableOperator object used during evaluation
     * @param rightAttribute Right attribute of which the value will be used during evaluation
     * @param rightConstant  An integer constant that is added as an offset to the value of the right attribute.
     */
    public VariableOrdinalAtom(OrdinalContractor<T> contractor, String leftAttribute, ComparableOperator operator, String rightAttribute, Integer rightConstant) {
        super(contractor, leftAttribute, operator, rightAttribute);
        this.rightConstant = rightConstant;
    }

    /**
     * Create a new VariableOrdinalAtom object with form leftAttribute operator rightAttribute (e.g. a <= b)
     *
     * @param contractor     OrdinalContractor object used by this atom during evaluation
     * @param leftAttribute  Left attribute of which the value will be used during evaluation
     * @param operator       ComparableOperator object used during evaluation
     * @param rightAttribute Right attribute of which the value will be used during evaluation
     */
    public VariableOrdinalAtom(OrdinalContractor<T> contractor, String leftAttribute, ComparableOperator operator, String rightAttribute) {
        this(contractor, leftAttribute, operator, rightAttribute, 0);
    }

    /**
     * Get integer constant that is added as an offset to the value of the right attribute
     * @return integer constant that is added as an offset to the value of the right attribute
     */
    public int getRightConstant() {
        return rightConstant;
    }

    @Override
    public boolean isAlwaysTrue() {

        if (!getLeftAttribute().equals(getRightAttribute())) {
            return false;
        }

        return (getOperator().equals(InequalityOperator.LT) && getRightConstant() >= 1) ||
                (getOperator().equals(InequalityOperator.GT) && getRightConstant() <= -1) ||
                (getOperator().equals(InequalityOperator.LEQ) && getRightConstant() >= 0) ||
                (getOperator().equals(InequalityOperator.GEQ) && getRightConstant() <= 0) ||
                (getOperator().equals(EqualityOperator.EQ) && getRightConstant() == 0) ||
                (getOperator().equals(EqualityOperator.NEQ) && getRightConstant() != 0);
    }

    @Override
    public boolean isAlwaysFalse() {

        if (!getLeftAttribute().equals(getRightAttribute())) {
            return false;
        }

        return (getOperator().equals(InequalityOperator.LT) && getRightConstant() <= 0) ||
                (getOperator().equals(InequalityOperator.GT) && getRightConstant() >= 0) ||
                (getOperator().equals(InequalityOperator.LEQ) && getRightConstant() <= -1) ||
                (getOperator().equals(InequalityOperator.GEQ) && getRightConstant() >= 1) ||
                (getOperator().equals(EqualityOperator.EQ) && getRightConstant() != 0) ||
                (getOperator().equals(EqualityOperator.NEQ) && getRightConstant() == 0);

    }

    @Override
    public AbstractAtom<?, ?, ?> fixLeft(DataObject o)
    {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getLeftAttribute()) && o.get(getLeftAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        if (attr.contains(getLeftAttribute()))
        {
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getRightAttribute(), //Right attribute
                    getOperator().getReversedOperator(), //Reverse order operator
                    getContractor().subtract((T) o.get(getLeftAttribute()), getRightConstant())); //Constant value for left attribute
        }

        return this;
    }

    @Override
    public AbstractAtom<?, ?, ?> fixRight(DataObject o) {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getRightAttribute()) && o.get(getRightAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        if (attr.contains(getRightAttribute()))
        {
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getLeftAttribute(), //Left attribute
                    getOperator(), //Same operator
                    getContractor().add((T) o.get(getRightAttribute()), getRightConstant())); //Constant value for right attribute
        }

        return this;
    }


    @Override
    public AbstractAtom<?, ?, ?> fix(DataObject o) {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getLeftAttribute()) && o.get(getLeftAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;
        
        if(attr.contains(getRightAttribute()) && o.get(getRightAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;
        
        //If none of the attributes are present in o, we just return this atom
        if (getAttributes().noneMatch(attr::contains))
            return this;
        else if (!attr.contains(getLeftAttribute()) && attr.contains(getRightAttribute())) {
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getLeftAttribute(), //Left attribute
                    getOperator(), //Same operator
                    getContractor().add((T) o.get(getRightAttribute()), getRightConstant())); //Constant value for right attribute
        } else if (attr.contains(getLeftAttribute()) && !attr.contains(getRightAttribute())) {
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getRightAttribute(), //Right attribute
                    getOperator().getReversedOperator(), //Reverse order operator
                    getContractor().subtract((T) o.get(getLeftAttribute()), getRightConstant())); //Constant value for left attribute
        } else
            return test(o) ? AbstractAtom.ALWAYS_TRUE : AbstractAtom.ALWAYS_FALSE;
    }

    @Override
    public VariableOrdinalAtom<?> getInverse() {
        return new VariableOrdinalAtom<>(
            this.getContractor(),
            this.getLeftAttribute(),
            this.getOperator().getInverseOperator(),
            this.getRightAttribute(),
            this.getRightConstant());
    }

    @Override
    public boolean test(DataObject o) throws DataException {

        Objects.requireNonNull(o, "DataObject must not be null");

        if (o.get(getLeftAttribute()) == null || o.get(getRightAttribute()) == null)
            return false;

        if (getContractor().test(o.get(getLeftAttribute())) &&
            getContractor().test(o.get(getRightAttribute())))
        {
            T leftAttributeValue = getContractor().getFromDataObject(o, getLeftAttribute());
            T rightAttributeValue = getContractor().add(getContractor().getFromDataObject(o, getRightAttribute()), rightConstant);
            return getOperator().test(leftAttributeValue, rightAttributeValue);
        }
        else {
            throw new DataException("Passed attribute value "
                + o.get(getLeftAttribute())
                + " or " + o.get(getRightAttribute())
                + " does not fulfill contract specified by "
                + getContractor());
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VariableOrdinalAtom<?> that = (VariableOrdinalAtom<?>) o;
        return rightConstant.equals(that.rightConstant);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(rightConstant);
        return result;
    }

    @Override
    public String toString() {
        if (rightConstant == 0)
        {
            return getLeftAttribute()
                + " "
                + getOperator().getSymbol()
                + " "
                + getRightAttribute();
        }
        else
        {
            return getLeftAttribute()
                + " "
                + getOperator().getSymbol()
                + " "
                + SUCCESSOR
                + getRightConstant()
                + "("
                + getRightAttribute()
                + ")";
        }
    }

    @Override
    public VariableOrdinalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new VariableOrdinalAtom<>(
                getContractor(),
                nameTransform.apply(getLeftAttribute()),
                getOperator(),
                nameTransform.apply(getRightAttribute()),
                getRightConstant()
        );
    }

    @Override
    public VariableOrdinalAtom<T> flip()
    {
        return new VariableOrdinalAtom<>(
            getContractor(),
            getRightAttribute(),
            getOperator().getReversedOperator(),
            getLeftAttribute(),
            -1 * rightConstant
        );
    }    
}
