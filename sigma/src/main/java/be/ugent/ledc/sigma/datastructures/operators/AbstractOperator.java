package be.ugent.ledc.sigma.datastructures.operators;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * @param <T> Datatype of the first object on which the Operator operates
 * @param <U> Datatype of the second object on which the Operator operates
 * @author Toon Boeckling
 * <p>
 * Abstract class implementing the Operator interface containing a symbol and an embedded BiPredicate object, getters and basic operations (equals, hashCode, toString).
 */
public abstract class AbstractOperator<T, U> implements Operator<T, U> {

    private final String symbol;
    private final String sqlSymbol;
    private final BiPredicate<T, U> embeddedBiPredicate;

    /**
     * Create a new AbstractOperator object
     *
     * @param symbol              String symbol of the Operator object.
     * @param sqlSymbol           String symbol in SQL of the Operator object.
     * @param embeddedBiPredicate BiPredicate object embedded served by the Operator object.
     */
    public AbstractOperator(String symbol, String sqlSymbol, BiPredicate<T, U> embeddedBiPredicate) {
        this.symbol = symbol;
        this.sqlSymbol = sqlSymbol;
        this.embeddedBiPredicate = embeddedBiPredicate;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getSqlSymbol()
    {
        return sqlSymbol;
    }

    @Override
    public BiPredicate<T, U> getEmbeddedPredicate() {
        return embeddedBiPredicate;
    }

    @Override
    public boolean test(T t, U u) {
        return embeddedBiPredicate.test(t, u);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractOperator<?, ?> that = (AbstractOperator<?, ?>) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(embeddedBiPredicate, that.embeddedBiPredicate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(symbol);
        result = 31 * result + Objects.hashCode(embeddedBiPredicate);
        return result;
    }

    @Override
    public String toString() {
        return "AbstractOperator{" +
                "symbol='" + symbol + '\'' +
                ", embeddedBiPredicate=" + embeddedBiPredicate +
                '}';
    }
}
