package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.Set;
import java.util.function.Function;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;


/**
 * @param <T> Datatype of the value of the attribute on which the atom operates
 * @author Toon Boeckling
 * <p>
 * A SetNominalAtom object can only exploit a NominalContractor object.
 * Examples include A NOTIN {"Hello", "world"}, A IN {"Set", "Nominal", "Atom"}
 */
public class SetNominalAtom<T extends Comparable<? super T>> extends SetAtom<T, NominalContractor<T>> {

    /**
     * Create a new SetNominalAtom object
     *
     * @param contractor NominalContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   SetOperator object used during evaluation
     * @param constants  Set of constants to which the value of the given attribute is compared during evaluation
     */
    public SetNominalAtom(NominalContractor<T> contractor, String attribute, SetOperator operator, Set<T> constants) {
        super(contractor, attribute, operator, constants);
    }

    @Override
    public SetNominalAtom<?> getInverse() {
        return new SetNominalAtom<>(
            this.getContractor(),
            this.getAttribute(),
            this.getOperator().getInverseOperator(),
            this.getConstants()
        );
    }

    @Override
    public SetNominalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new SetNominalAtom<>(
                getContractor(),
                nameTransform.apply(getAttribute()),
                getOperator(),
                getConstants()
        );
    }


}
