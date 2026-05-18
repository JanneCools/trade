package be.ugent.ledc.sigma.datastructures.operators;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * <p>
 * A ComparableOperator object is an Operator object which serves on two Comparable objects
 */
public abstract class ComparableOperator extends AbstractOperator<Comparable, Comparable> {

    /**
     * Create a new ComparableOperator object
     *
     * @param symbol              String symbol of the ComparableOperator object.
     * @param sqlSymbol           String symbol in SQL of the ComparableOperator object.
     * @param embeddedBiPredicate BiPredicate object embedded served by the ComparableOperator object.
     */
    public ComparableOperator(String symbol, String sqlSymbol, BiPredicate<Comparable, Comparable> embeddedBiPredicate) {
        super(symbol, sqlSymbol, embeddedBiPredicate);
    }

    @Override
    public ComparableOperator getInverseOperator() throws OperatorException {
        return switch (getSymbol()) {
            case "<=" -> InequalityOperator.GT;
            case "<" -> InequalityOperator.GEQ;
            case ">=" -> InequalityOperator.LT;
            case ">" -> InequalityOperator.LEQ;
            case "==" -> EqualityOperator.NEQ;
            case "!=" -> EqualityOperator.EQ;
            default -> throw new OperatorException("Could not find an inverse operator for symbol " + getSymbol());
        };
    }

    @Override
    public Set<Operator<Comparable, Comparable>> getImpliedOperators() throws OperatorException {
        return switch (getSymbol()) {
            case "<=" -> Stream.of(InequalityOperator.LEQ).collect(Collectors.toSet());
            case "<" ->
                    Stream.of(InequalityOperator.LT, InequalityOperator.LEQ, EqualityOperator.NEQ).collect(Collectors.toSet());
            case ">=" -> Stream.of(InequalityOperator.GEQ).collect(Collectors.toSet());
            case ">" ->
                    Stream.of(InequalityOperator.GEQ, InequalityOperator.GT, EqualityOperator.NEQ).collect(Collectors.toSet());
            case "==" ->
                    Stream.of(EqualityOperator.EQ, InequalityOperator.GEQ, InequalityOperator.LEQ).collect(Collectors.toSet());
            case "!=" -> Stream.of(EqualityOperator.NEQ).collect(Collectors.toSet());
            default -> throw new OperatorException("Could not find implied operators for symbol " + getSymbol());
        };
    }

    @Override
    public ComparableOperator getReversedOperator() throws OperatorException {
        return switch (getSymbol()) {
            case "<=" -> InequalityOperator.GEQ;
            case "<" -> InequalityOperator.GT;
            case ">=" -> InequalityOperator.LEQ;
            case ">" -> InequalityOperator.LT;
            case "==" -> EqualityOperator.EQ;
            case "!=" -> EqualityOperator.NEQ;
            default -> throw new OperatorException("Could not find an inverse operator for symbol " + getSymbol());
        };
    }

    public static ComparableOperator getTransitiveOperatorVariable(List<ComparableOperator> operators) {

        if (operators.contains(InequalityOperator.LT) && !(operators.contains(InequalityOperator.GEQ) || operators.contains(InequalityOperator.GT) || operators.contains(EqualityOperator.NEQ))) {
            return InequalityOperator.LT;
        } else if (operators.contains(InequalityOperator.GT) && !(operators.contains(InequalityOperator.LEQ) || operators.contains(InequalityOperator.LT) || operators.contains(EqualityOperator.NEQ))) {
            return InequalityOperator.GT;
        } else if (operators.contains(InequalityOperator.LEQ) && !(operators.contains(InequalityOperator.GEQ) || operators.contains(InequalityOperator.GT) || operators.contains(EqualityOperator.NEQ))) {
            return InequalityOperator.LEQ;
        } else if (operators.contains(InequalityOperator.GEQ) && !(operators.contains(InequalityOperator.LEQ) || operators.contains(InequalityOperator.LT) || operators.contains(EqualityOperator.NEQ))) {
            return InequalityOperator.GEQ;
        } else if (operators.stream().allMatch(so -> so.equals(EqualityOperator.EQ))) {
            return EqualityOperator.EQ;
        } else if (operators.stream().allMatch(so -> so.equals(EqualityOperator.EQ) || so.equals(EqualityOperator.NEQ)) && operators.stream().filter(so -> so.equals(EqualityOperator.NEQ)).count() == 1) {
            return EqualityOperator.NEQ;
        }

        return null;
    }

    @Override
    public String toString() {
        return "ComparableOperator{" +
                "symbol='" + getSymbol() + '\'' +
                ", embeddedBiPredicate=" + getEmbeddedPredicate() +
                '}';
    }
}

