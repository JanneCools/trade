package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.dataset.DataObject;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * <p>
 * An atom is one term in a boolean expression that can be tested on a DataObject object.
 * Examples include A < 5, C IN ['Hello', 'World'], X = 3.14, A >= B
 */
public interface Atom extends Predicate<DataObject> {

    /**
     * Get a Stream of attributes that are defined within this atom.
     *
     * @return Stream of attributes that are defined within this atom
     */
    Stream<String> getAttributes();

    /**
     * Get a Stream of attributes that are defined within this atom.
     *
     * @return Stream of attributes that are defined within this atom
     */
    Set<String> getAttributesSet();

    /**
     * Check whether Atom object is equivalent to always false (e.g. A IN {})
     *
     * @return true if Atom object is equivalent to always false, false otherwise
     */
    default boolean isAlwaysFalse() { return false; }

    /**
     * Check whether Atom object is equivalent to always true (e.g. A NOTIN {})
     *
     * @return true if Atom object is equivalent to always true, false otherwise
     */
    default boolean isAlwaysTrue() { return false; }
}
