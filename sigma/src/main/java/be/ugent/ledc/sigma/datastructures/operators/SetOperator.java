package be.ugent.ledc.sigma.datastructures.operators;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * <p>
 * A SetOperator object is an Operator object which serves on a Comparable and a set of Comparables
 * This class defines two SetOperators, being 'IN' and 'NOT IN'
 */
public class SetOperator extends AbstractOperator<Comparable, Set<? extends Comparable>> {

    public static final SetOperator IN = new SetOperator("in", (Comparable c1, Set<? extends Comparable> c2) -> c2.stream().anyMatch(e -> c1.compareTo(e) == 0));
    public static final SetOperator NOTIN = new SetOperator("notin", (Comparable c1, Set<? extends Comparable> c2) -> c2.stream().allMatch(e -> c1.compareTo(e) != 0));

    /**
     * Create a new SetOperator object
     *
     * @param symbol              String symbol of the SetOperator object.
     * @param embeddedBiPredicate BiPredicate object embedded served by the SetOperator object.
     */
    SetOperator(String symbol, BiPredicate<Comparable, Set<? extends Comparable>> embeddedBiPredicate) {
        super(symbol, symbol, embeddedBiPredicate);
    }

    @Override
    public SetOperator getInverseOperator() throws OperatorException {
        return switch (getSymbol().trim().toUpperCase()) {
            case "IN" -> NOTIN;
            case "NOTIN" -> IN;
            default -> throw new OperatorException("Could not find an inverse operator for symbol " + getSymbol());
        };
    }

    @Override
    public Set<Operator<Comparable, Set<? extends Comparable>>> getImpliedOperators() throws OperatorException {
        return switch (getSymbol().trim().toUpperCase()) {
            case "IN" -> Stream.of(IN).collect(Collectors.toSet());
            case "NOTIN" -> Stream.of(NOTIN).collect(Collectors.toSet());
            default -> throw new OperatorException("Could not find implied operators for symbol " + getSymbol());
        };
    }

    @Override
    public SetOperator getReversedOperator() throws OperatorException {
        return this;
    }

    @Override
    public String toString() {
        return "SetOperator{" +
                "symbol='" + getSymbol() + '\'' +
                ", embeddedBiPredicate=" + getEmbeddedPredicate() +
                '}';
    }
    
}
