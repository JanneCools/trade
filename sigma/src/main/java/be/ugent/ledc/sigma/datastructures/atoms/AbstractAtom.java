package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.binding.jdbc.DBMS;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.operators.AbstractOperator;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;

/**
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @param <C> Type of SigmaContractor object used by this atom during evaluation
 * @param <O> Type of the AbstractOperator object used during evaluation
 * @author Toon Boeckling
 * <p>
 * Abstract class implementing the Atom interface containing a contractor and an operator, getters and basic operations (equals, hashCode, toString).
 */
public abstract class AbstractAtom<T extends Comparable<? super T>, C extends SigmaContractor<T>, O extends AbstractOperator<?, ?>> implements Atom
{
    public static final DummyAtom<?, ?, ?> ALWAYS_TRUE = new DummyAtom<>(true);
    public static final DummyAtom<?, ?, ?> ALWAYS_FALSE = new DummyAtom<>(false);
    
    private final C contractor;
    private final O operator;

    /**
     * Create a new AbstractAtom object
     *
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param operator   Operator object used during evaluation
     */
    public AbstractAtom(C contractor, O operator) {
        this.contractor = contractor;
        this.operator = operator;                
    }

    /**
     * Retrieve the SigmaContractor object which operations are used during evaluation of the atom.
     * More specifically, it is used for operations on the values within the atom or evaluated data object.
     *
     * @return SigmaContractor object which operations are used during evaluation of the atom
     */
    public C getContractor() {
        return contractor;
    }

    /**
     * Retrieve the Operator object used during evaluation.
     *
     * @return Operator object used during evaluation
     */
    public O getOperator() {
        return operator;
    }

    /**
     * Check whether this AbstractAtom object implies the other AbstractAtom object
     * @param other AbstractAtom object for which it is checked whether it is implied by this AbstractAtom object
     * @return true if this AbstractAtom object implies the other AbstractAtom object, false otherwise
     */
    public boolean implies(AbstractAtom<?, ?, ?> other) {
        return AtomImplicator.implies(this, other);
    }

    /**
     * Check whether this AbstractAtom object is equivalent to the other AbstractAtom object
     * @param other AbstractAtom object for which it is checked whether it is equivalent to this AbstractAtom object
     * @return true if this AbstractAtom object is equivalent to the other AbstractAtom object, false otherwise
     */
    public boolean isEquivalentTo(AbstractAtom<?, ?, ?> other) {
        return this.implies(other) && other.implies(this);
    }

    public abstract AbstractAtom<?,?,?> getInverse();

    public abstract AbstractAtom<?,?,?> fix(DataObject o);

    public abstract String toSqlString(DBMS vendor);

    public abstract AtomType getAtomType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractAtom<?, ?, ?> that = (AbstractAtom<?, ?, ?>) o;
        return Objects.equals(contractor, that.contractor) && Objects.equals(operator, that.operator);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(contractor);
        result = 31 * result + Objects.hashCode(operator);
        return result;
    }

    @Override
    public String toString() {
        return "AbstractAtom{" +
                "contractor=" + contractor +
                ", operator=" + operator +
                '}';
    }

    public static class DummyAtom<T extends Comparable<? super T>, C extends SigmaContractor<T>, O extends AbstractOperator<?, ?>> extends AbstractAtom<T, C, O>
    {
        private final boolean fixedValue;
        
        public DummyAtom(boolean fixedValue)
        {
            super(null,null);
            this.fixedValue = fixedValue;
        }

        @Override
        public AbstractAtom<?, ?, ?> getInverse() {
            return new DummyAtom<>(!fixedValue);
        }

        @Override
        public AbstractAtom<?, ?, ?> fix(DataObject o)
        {
            return this;
        }

        @Override
        public Stream<String> getAttributes()
        {
            return Stream.empty();
        }

        @Override
        public Set<String> getAttributesSet() { return Collections.emptySet(); }

        @Override
        public boolean isAlwaysTrue() {
            return this.equals(AbstractAtom.ALWAYS_TRUE);
        }

        @Override
        public boolean isAlwaysFalse() {
            return this.equals(AbstractAtom.ALWAYS_FALSE);
        }

        @Override
        public boolean test(DataObject t)
        {
            return fixedValue;
        }

        @Override
        public String toString()
        {
            return fixedValue ? "True" : "False";
        }

        @Override
        public String toSqlString(DBMS vendor)
        {
            return fixedValue ? "true" : "false";
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 83 * hash + (this.fixedValue ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final DummyAtom<?, ?, ?> other = (DummyAtom<?, ?, ?>) obj;
            return this.fixedValue == other.fixedValue;
        }

        @Override
        public AtomType getAtomType()
        {
            return AtomType.DUMMY;
        }

        @Override
        public AbstractAtom<T, C, O> nameTransform(Function<String, String> nameTransform)
        {
            return this;
        }

        @Override
        public boolean involves(String attribute)
        {
            return false;
        }
    }

    public abstract boolean involves(String attribute);
    
    public abstract AbstractAtom<T, C, O> nameTransform(Function<String,String> nameTransform);
    
}
