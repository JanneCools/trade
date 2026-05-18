package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import java.util.*;
import java.util.function.Function;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;


/**
 * @param <T> Datatype of the value of the attribute on which the atom operates
 * @author Toon Boeckling
 * <p>
 * A SetOrdinalAtom object can only exploit a OrdinalContractor object.
 * Examples include A NOTIN {2, 3, 7}, A IN {1}, A IN {1, 5, 8}
 */
public class SetOrdinalAtom<T extends Comparable<? super T>> extends SetAtom<T, OrdinalContractor<T>> {


    /**
     * Create a new SetCountableAtom object
     *
     * @param contractor OrdinalContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   SetOperator object used during evaluation
     * @param constants  Set of constants to which the value of the given attribute is compared during evaluation
     */
    public SetOrdinalAtom(OrdinalContractor<T> contractor, String attribute, SetOperator operator, Set<T> constants) {
        super(contractor, attribute, operator, constants);
    }

    public Set<Interval<T>> toIntervals() {

        List<T> sortedConstants = new ArrayList<>(getConstants()).stream().sorted().toList();

        if (isAlwaysFalse()) {
            throw new AtomException("Cannot transform SetOrdinalAtom " + this + " featuring an empty set to a set of Interval objects");
        }

        if (isAlwaysTrue()) {
            return Collections.singleton(new Interval<>());
        }

        Set<Interval<T>> intervals = new HashSet<>();

        if (getOperator().equals(SetOperator.IN)) {

            for (int i = 0; i < sortedConstants.size(); i++) {

                T currentLowerBound = sortedConstants.get(i);

                while (i < sortedConstants.size() - 1 && getContractor().previous(sortedConstants.get(i + 1)).equals(sortedConstants.get(i))) {
                    i++;
                }

                intervals.add(new Interval<>(currentLowerBound, sortedConstants.get(i), false, false));

            }

        } else {

            intervals.add(new Interval<>(null, getContractor().previous(sortedConstants.get(0)), true, false));

            for (int i = 0; i < sortedConstants.size(); i++) {

                while (i < sortedConstants.size() - 1 && getContractor().previous(sortedConstants.get(i + 1)).equals(sortedConstants.get(i))) {
                    i++;
                }

                if (i == sortedConstants.size() - 1) {
                    intervals.add(new Interval<>(getContractor().next(sortedConstants.get(sortedConstants.size() - 1)), null, false, true));
                } else {
                    intervals.add(new Interval<>(getContractor().next(sortedConstants.get(i)), getContractor().previous(sortedConstants.get(i+1)), false, false));
                }

            }

        }

        return intervals;

    }

    @Override
    public SetOrdinalAtom<?> getInverse() {
        return new SetOrdinalAtom<>(
            this.getContractor(),
            this.getAttribute(),
            this.getOperator().getInverseOperator(),
            this.getConstants());
    }

    @Override
    public SetOrdinalAtom<T> nameTransform(Function<String, String> nameTransform)
    {
        return new SetOrdinalAtom<>(
                getContractor(),
                nameTransform.apply(getAttribute()),
                getOperator(),
                getConstants()
        );
    }
}
