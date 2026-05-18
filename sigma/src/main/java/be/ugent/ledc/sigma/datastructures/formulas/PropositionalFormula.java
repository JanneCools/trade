package be.ugent.ledc.sigma.datastructures.formulas;

import be.ugent.ledc.core.dataset.DataObject;

import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Toon Boeckling
 * <p>
 * This interface represents a PropositionalFormula, which is a Predicate evaluated on a DataObject consisting of a set of Atoms
 */
public interface PropositionalFormula extends Predicate<DataObject> {

    /**
     * Get the set of attributes involved in the PropositionalFormula object.
     *
     * @return set of attributes involved in the PropositionalFormula object
     */
    Set<String> getAttributes();

    /**
     * Returns true if given attribute is involved in the PropositionalFormula object and false otherwise.
     * By default, an attribute is involved if it is within the set of involved attributes.
     *
     * @param attribute attribute to check whether it is involved in the PropositionalFormula object
     * @return true if given attribute is involved in the PropositionalFormula object, false otherwise
     */
    default boolean hasAttribute(String attribute) {
        return getAttributes().contains(attribute);
    }

    /**
     * Returns true if PropositionalFormula is satisfied by a given DataObject object and false otherwise.
     * By default, a PropositionalFormula is satisfied by a given DataObject object if the test evaluates to true.
     *
     * @param dataObject DataObject object for which it has to be checked whether it satisfies the PropositionalFormula
     * @return true if PropositionalFormula is satisfied by a given DataObject object and false otherwise
     */
    default boolean isSatisfied(DataObject dataObject) {
        return test(dataObject);
    }

    /**
     * Returns true if PropositionalFormula is failed by a given DataObject object and false otherwise.
     * By default, a PropositionalFormula is failed by a given DataObject object if the test evaluates to false.
     *
     * @param dataObject DataObject object for which it has to be checked whether it fails the PropositionalFormula
     * @return true if PropositionalFormula is failed by a given DataObject object and false otherwise
     */
    default boolean isFailed(DataObject dataObject) {
        return !isSatisfied(dataObject);
    }

    /**
     * Returns true if PropositionalFormula object is a logical tautology, and therefore, evaluates to true for each data object (is always satisfied), false otherwise.
     *
     * @return true if PropositionalFormula object is a logical tautology, and therefore, evaluates to true for each data object (is always satisfied), false otherwise
     */
    boolean isTautology();

    /**
     * Returns true if PropositionalFormula object is a logical contradiction, and therefore, evaluates to false for each data object (is never satisfied), false otherwise.
     *
     * @return true if PropositionalFormula object is a logical contradiction, and therefore, evaluates to false for each data object (is never satisfied), false otherwise.
     */
    boolean isContradiction();

    /**
     * Returns a PropositionalFormula object in which the attributes specified in DataObject o are given their constant value from o.
     *
     * @param dataObject DataObject object which constant values are fixed in the PropositionalFormula object
     * @return PropositionalFormula object in which the attributes specified in DataObject o are given their constant value from o
     */
    PropositionalFormula fix(DataObject dataObject);

}
