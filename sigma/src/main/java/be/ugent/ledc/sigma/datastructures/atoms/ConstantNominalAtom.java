package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import java.util.function.Function;

/**
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @author Toon Boeckling
 * <p>
 A ConstantNominalAtom object can only exploit an NominalContractor object and an EqualityOperator object.
 Examples include A == "Hello", A != "World"
 */
public class ConstantNominalAtom<T extends Comparable<? super T>> extends ConstantAtom<T, NominalContractor<T>, EqualityOperator> {

    /**
     * Create a new ConstantNominalAtom object
     *
     * @param contractor NominalContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   EqualityOperator object used during evaluation
     * @param constant   Constant to which the value of the given attribute is compared during evaluation
     */
    public ConstantNominalAtom(NominalContractor<T> contractor, String attribute, EqualityOperator operator, T constant) {
        super(contractor, attribute, operator, constant);
    }

    @Override
    public ConstantNominalAtom<?> getInverse() {
        return new ConstantNominalAtom<>(
            this.getContractor(),
            this.getAttribute(),
            (EqualityOperator) this.getOperator().getInverseOperator(),
            this.getConstant());
    }

    @Override
    public ConstantNominalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new ConstantNominalAtom<>(
                getContractor(),
                nameTransform.apply(getAttribute()),
                getOperator(),
                getConstant()
        );
    }


}
