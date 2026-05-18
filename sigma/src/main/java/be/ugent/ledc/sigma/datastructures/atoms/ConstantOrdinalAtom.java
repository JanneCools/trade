package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.InequalityOperator;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @author Toon Boeckling
 * <p>
 * A ConstantOrdinalAtom object can only exploit a OrdinalContractor object.
 * Examples include A == 5, A <= 5, A < 3.14, A > 7,...
 */
public class ConstantOrdinalAtom<T extends Comparable<? super T>>
        extends ConstantAtom<T, OrdinalContractor<T>, ComparableOperator> {

    /**
     * Create a new ConstantOrdinalAtom object
     *
     * @param contractor OrdinalContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   ComparableOperator object used during evaluation
     * @param constant   Constant to which the value of the given attribute is compared during evaluation
     */
    public ConstantOrdinalAtom(OrdinalContractor<T> contractor, String attribute, ComparableOperator operator, T constant) {
        super(contractor, attribute, operator, constant);
    }

    public Set<Interval<T>> toIntervals() {

        if (getOperator().equals(InequalityOperator.LEQ)) {
            return Collections.singleton(new Interval<>(null, getConstant(), true, false));
        } else if (getOperator().equals(InequalityOperator.LT)) {
            return Collections.singleton(new Interval<>(null, getContractor().previous(getConstant()), true, false));
        } else if (getOperator().equals(InequalityOperator.GEQ)) {
            return Collections.singleton(new Interval<>(getConstant(), null, false, true));
        } else if (getOperator().equals(InequalityOperator.GT)) {
            return Collections.singleton(new Interval<>(getContractor().next(getConstant()), null, false, true));
        } else if (getOperator().equals(EqualityOperator.EQ)) {
            return Collections.singleton(new Interval<>(getConstant(), getConstant(), false, false));
        } else {
            return Set.of(new Interval<>(null, getContractor().previous(getConstant()), true, false), new Interval<>(getContractor().next(getConstant()), null, false, true));
        }

    }

    @Override
    public ConstantOrdinalAtom<?> getInverse() {
        return new ConstantOrdinalAtom<>(
            this.getContractor(),
            this.getAttribute(),
            this.getOperator().getInverseOperator(),
            this.getConstant());
    }

    @Override
    public ConstantOrdinalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new ConstantOrdinalAtom<>(
                getContractor(),
                nameTransform.apply(getAttribute()),
                getOperator(),
                getConstant()
        );
    }

}
