package be.ugent.ledc.sigma.datastructures.operators;

import java.util.Set;
import java.util.function.BiPredicate;

/**
 * @param <T> Datatype of the first object on which the Operator operates
 * @param <U> Datatype of the second object on which the Operator operates
 * @author Toon Boeckling
 * An Operator object serves as a BiPredicate on two objects of types T and U
 */
public interface Operator<T, U> extends BiPredicate<T, U> {

    /**
     * Get the String symbol of the Operator object.
     *
     * @return String symbol of the Operator object
     */
    String getSymbol();
    
    String getSqlSymbol();

    /**
     * Get the BiPredicate object embedded served by the Operator object.
     *
     * @return BiPredicate object embedded served by the Operator object
     */
    BiPredicate<T, U> getEmbeddedPredicate();

    /**
     * Get the inverse Operator object of this Operator object.
     *
     * @return inverse Operator object of this Operator object
     */
    Operator<T, U> getInverseOperator();

    /**
     * Get the set of implied Operator objects of this Operator object.
     *
     * @return set of implied Operator objects of this Operator object
     */
    Set<Operator<T, U>> getImpliedOperators();

    /**
     * Get the reversed Operator object of this Operator object.
     *
     * @return reversed Operator object of this Operator object
     */
    Operator<T, U> getReversedOperator();
}

