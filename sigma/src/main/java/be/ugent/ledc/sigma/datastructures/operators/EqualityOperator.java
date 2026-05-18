package be.ugent.ledc.sigma.datastructures.operators;

import java.util.function.BiPredicate;

/**
 * This class defines two ComparableOperator objects, being 'equal to (==)' and 'not equal to (!=)'
 *
 * @author Toon Boeckling
 */
public class EqualityOperator extends ComparableOperator {

    public static final EqualityOperator EQ = new EqualityOperator(
        "==",
        "=",
        (Comparable c1, Comparable c2) -> c1.compareTo(c2) == 0
    );
    
    public static final EqualityOperator NEQ = new EqualityOperator(
        "!=",
        "<>",
        (Comparable c1, Comparable c2) -> c1.compareTo(c2) != 0);

    /**
     * Create a new EqualityOperator object
     *
     * @param symbol              String symbol of the EqualityOperator object.
     * @param embeddedBiPredicate BiPredicate object embedded served by the EqualityOperator object.
     */
    EqualityOperator(String symbol, String sqlSymbol, BiPredicate<Comparable, Comparable> embeddedBiPredicate) {
        super(symbol, sqlSymbol, embeddedBiPredicate);
    }

    @Override
    public String toString() {
        return "EqualityOperator{" +
                "symbol='" + getSymbol() + '\'' +
                ", embeddedBiPredicate=" + getEmbeddedPredicate() +
                '}';
    }


}
