package be.ugent.ledc.sigma.datastructures.operators;

import java.util.stream.Stream;

/**
 * This class captures utility methods on operators
 */
public class OperatorUtil {

    /**
     * Get all currently existing AbstractOperator objects as a Stream.
     *
     * @return Stream of all currently existing AbstractOperator objects
     */
    public static Stream<AbstractOperator<?, ?>> getAbstractOperators() {
        return Stream.of(
                InequalityOperator.GT,
                InequalityOperator.GEQ,
                InequalityOperator.LT,
                InequalityOperator.LEQ,
                EqualityOperator.EQ,
                EqualityOperator.NEQ,
                SetOperator.IN,
                SetOperator.NOTIN
        );
    }

    /**
     * Get all currently existing ComparableOperator objects as a Stream.
     *
     * @return Stream of all currently existing ComparableOperator objects
     */
    public static Stream<ComparableOperator> getComparableOperators() {
        return Stream.of(
                InequalityOperator.GT,
                InequalityOperator.GEQ,
                InequalityOperator.LT,
                InequalityOperator.LEQ,
                EqualityOperator.EQ,
                EqualityOperator.NEQ
        );
    }

    /**
     * Get all currently existing InequalityOperator objects as a Stream.
     *
     * @return Stream of all currently existing InequalityOperator objects
     */
    public static Stream<InequalityOperator> getInequalityOperators() {
        return Stream.of(
                InequalityOperator.GT,
                InequalityOperator.GEQ,
                InequalityOperator.LT,
                InequalityOperator.LEQ
        );
    }

    /**
     * Get all currently existing EqualityOperator objects as a Stream.
     *
     * @return Stream of all currently existing EqualityOperator objects
     */
    public static Stream<EqualityOperator> getEqualityOperators() {
        return Stream.of(
                EqualityOperator.EQ,
                EqualityOperator.NEQ
        );
    }

    /**
     * Get all currently existing SetOperator objects as a Stream.
     *
     * @return Stream of all currently existing SetOperator objects
     */
    public static Stream<SetOperator> getSetOperators() {
        return Stream.of(
                SetOperator.IN,
                SetOperator.NOTIN
        );
    }

    /**
     * Get the AbstractOperator object matching the given symbol.
     *
     * @param symbol Symbol of the operator
     * @return AbstractOperator object matching the given symbol
     */
    public static AbstractOperator<?, ?> getOperatorBySymbol(String symbol) throws OperatorException {
        return switch (symbol.trim().toLowerCase()) {
            case "<=" -> InequalityOperator.LEQ;
            case "<" -> InequalityOperator.LT;
            case ">=" -> InequalityOperator.GEQ;
            case ">" -> InequalityOperator.GT;
            case "==" -> EqualityOperator.EQ;
            case "!=" -> EqualityOperator.NEQ;
            case "in" -> SetOperator.IN;
            case "notin" -> SetOperator.NOTIN;
            default ->
                    throw new OperatorException("Given symbol " + symbol + " does not represent a InequalityOperator object." +
                            "Possible symbols are '<=', '<', '>=', '>', '==', '!=', 'in' and 'notin'.");
        };
    }
}
