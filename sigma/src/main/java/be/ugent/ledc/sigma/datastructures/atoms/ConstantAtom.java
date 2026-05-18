package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.binding.jdbc.DBMS;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorUtil;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.Operator;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @param <T> Datatype of the value of the attribute on which the atom operates
 * @param <C> Type of SigmaContractor object used by this atom during evaluation
 * @param <O> Type of the ComparableOperator object used during evaluation
 * @author Toon Boeckling
 * <p>
 * A ConstantAtom object consists of one attribute that is compared with a single constant
 * by means of a ComparableOperator object.
 * Examples include A == 5, A <= 5, A < 3.14, A > 7, A == "Hello",...
 */
public abstract class ConstantAtom<T extends Comparable<? super T>, C extends SigmaContractor<T>, O extends ComparableOperator>
        extends AbstractAtom<T, C, O> {

    private final String attribute;
    private final T constant;

    /**
     * Create a new ConstantAtom object
     *
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   ComparableOperator object used during evaluation
     * @param constant   Constant to which the value of the given attribute is compared during evaluation
     */
    public ConstantAtom(C contractor, String attribute, O operator, T constant) {
        super(contractor, operator);
        this.attribute = attribute;
        this.constant = contractor.get(constant);
    }

    /**
     * Get the attribute defined within this ConstantAtom object.
     *
     * @return attribute defined within this ConstantAtom object
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Get the constant defined within this ConstantAtom object.
     *
     * @return constant defined within this ConstantAtom object
     */
    public T getConstant() {
        return constant;
    }

    @Override
    public Stream<String> getAttributes() {
        return Stream.of(attribute);
    }

    @Override
    public Set<String> getAttributesSet() {
        return Collections.singleton(attribute);
    }

    @Override
    public boolean test(DataObject o) throws DataException {

        Objects.requireNonNull(o, "DataObject must not be null");

        if(o.get(attribute) == null)
            return false;

        if (getContractor().test(o.get(attribute)))
        {
            T attributeValue = getContractor().getFromDataObject(o, attribute);
            return getOperator().test(attributeValue, constant);
        }
        else
        {
            throw new DataException("Passed attribute value "
                + o.get(attribute)
                + " does not fulfill contract specified by "
                + getContractor());
        }
    }

    /**
     * Static method used for parsing the individual components (method arguments) to a constant atom
     * @param leftOperand Attribute to parse
     * @param operator ComparableOperator object to parse
     * @param rightOperand Constant to parse
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param <T> Datatype of constant to parse
     * @return AbstractAtom object as a result of parsing the individual components
     * @throws AtomException Exception to throw in case the atom could not be parsed
     */
    public static <T extends Comparable<? super T>> AbstractAtom<?,?,?> parseConstantAtom(String leftOperand, Operator<?,?> operator, String rightOperand, SigmaContractor<?> contractor) throws AtomException
    {
        T typedOperand2 = (T) contractor.parseConstant(rightOperand);

        if(SigmaContractorUtil.isOrdinalContractor(contractor))
            return new ConstantOrdinalAtom<>(
                    (OrdinalContractor<T>) contractor,
                    leftOperand,
                    (ComparableOperator) operator,
                    typedOperand2
            );

        if(SigmaContractorUtil.isNominalContractor(contractor))
        {
            return new ConstantNominalAtom<>(
                    (NominalContractor<T>) contractor,
                    leftOperand,
                    (EqualityOperator) operator,
                    typedOperand2);
        }

        throw new AtomException("Could not parse constant atom with"
                + " left operand '" + leftOperand + "'"
                + " operator '" + operator.getSymbol() + "'"
                + " and right operand '" + rightOperand + "'.");
    }

    @Override
    public AbstractAtom<?, ?, ?> fix(DataObject o) {
        if (!o.getAttributes().contains(this.attribute))
            return this;
        else
            return test(o) ? AbstractAtom.ALWAYS_TRUE : AbstractAtom.ALWAYS_FALSE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstantAtom<?, ?, ?> that = (ConstantAtom<?, ?, ?>) o;
        return Objects.equals(attribute, that.attribute) && Objects.equals(constant, that.constant);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(attribute);
        result = 31 * result + Objects.hashCode(constant);
        return result;
    }

    @Override
    public String toString()
    {
        return attribute + " " + getOperator().getSymbol() + " " + getContractor().printConstant(constant);
    }

    @Override
    public String toSqlString(DBMS vendor)
    {
        return vendor
                .getAgent()
                .escaping()
                .apply(attribute)
                + " "
                + getOperator()
                .getSqlSymbol()
                + " "
                + getContractor()
                .printConstant(constant);
    }


    @Override
    public AtomType getAtomType()
    {
        return AtomType.CONSTANT;
    } 

    @Override
    public boolean involves(String attribute)
    {
        return this.attribute.equals(attribute);
    }
}
