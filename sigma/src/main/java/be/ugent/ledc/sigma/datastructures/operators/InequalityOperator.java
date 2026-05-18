package be.ugent.ledc.sigma.datastructures.operators;

import java.util.function.BiPredicate;

/**
 * This class defines four ComparableOperator objects, being 'greater than (>)', 'greather than or equal to (>=)', 'lower than (<)' and 'lower than or equal to (<=)'
 *
 * @author Toon Boeckling
 */
public class InequalityOperator extends ComparableOperator {

    public static final InequalityOperator LEQ = new InequalityOperator("<=", (Comparable c1, Comparable c2) -> c1.compareTo(c2) <= 0);
    public static final InequalityOperator LT = new InequalityOperator("<", (Comparable c1, Comparable c2) -> c1.compareTo(c2) < 0);
    public static final InequalityOperator GEQ = new InequalityOperator(">=", (Comparable c1, Comparable c2) -> c1.compareTo(c2) >= 0);
    public static final InequalityOperator GT = new InequalityOperator(">", (Comparable c1, Comparable c2) -> c1.compareTo(c2) > 0);

    /**
     * Create a new InequalityOperator object
     *
     * @param symbol              String symbol of the InequalityOperator object.
     * @param embeddedBiPredicate BiPredicate object embedded served by the InequalityOperator object.
     */
    InequalityOperator(String symbol, BiPredicate<Comparable, Comparable> embeddedBiPredicate) {
        super(symbol, symbol, embeddedBiPredicate);
    }

    @Override
    public String toString() {
        return "InequalityOperator{" +
                "symbol='" + getSymbol() + '\'' +
                ", embeddedBiPredicate=" + getEmbeddedPredicate() +
                '}';
    }

}
