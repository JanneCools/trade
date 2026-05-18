package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.binding.jdbc.DBMS;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorUtil;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @param <C> Type of SigmaContractor object used by this atom during evaluation
 * @author Toon Boeckling
 * <p>
 * A SetAtom object consists of one attribute that is compared with a set of constants by means of a SetOperator object.
 * Examples include A IN {"Hello", "world"}, A NOTIN {2, 3, 7},...
 */
public abstract class SetAtom<T extends Comparable<? super T>, C extends SigmaContractor<T>>
        extends AbstractAtom<T, C, SetOperator> {

    private final String attribute;
    private final Set<T> constants;

    /**
     * Create a new SetAtom object.
     *
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param attribute  Attribute of which the value will be used during evaluation
     * @param operator   SetOperator object used during evaluation
     * @param constants  Set of constants to which the value of the given attribute is compared during evaluation
     */
    public SetAtom(C contractor, String attribute, SetOperator operator, Set<T> constants) {
        super(contractor, operator);
        this.attribute = attribute;
        this.constants = constants.stream().map(contractor::get).collect(Collectors.toSet());
    }

    /**
     * Get the attribute defined within this SetAtom object.
     *
     * @return attribute defined within this SetAtom object
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Get the set of constants defined within this ConstantAtom object.
     *
     * @return set of constants defined within this ConstantAtom object
     */
    public Set<T> getConstants() {
        return constants;
    }

    @Override
    public Stream<String> getAttributes() {
        return Stream.of(attribute);
    }

    @Override
    public Set<String> getAttributesSet() { return Collections.singleton(attribute); }

    @Override
    public boolean test(DataObject o) throws DataException {

        Objects.requireNonNull(o, "DataObject must not be null");

        if(o.get(attribute) == null)
            return false;
        
        if (getContractor().test(o.get(attribute))) {
            T attributeValue = getContractor().getFromDataObject(o, attribute);
            return getOperator().test(attributeValue, constants);
        } else {
            throw new DataException("Passed attribute value " + o.get(attribute) + " does not fulfill contract specified by " + getContractor());
        }
    }

    @Override
    public boolean isAlwaysTrue() {
        return getOperator().equals(SetOperator.NOTIN) && getConstants().isEmpty();
    }

    @Override
    public boolean isAlwaysFalse() {
        return getOperator().equals(SetOperator.IN) && getConstants().isEmpty();
    }

    /**
     * Static method used for parsing the individual components (method arguments) to a set atom
     * @param leftOperand Attribute to parse
     * @param operator SetOperator object to parse
     * @param rightOperand Set of constants to parse
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param <T> Datatype of constant to parse
     * @return AbstractAtom object as a result of parsing the individual components
     * @throws AtomException Exception to throw in case the atom could not be parsed
     */
    public static <T extends Comparable<? super T>> AbstractAtom<?, ?, ?> parseSetAtom(String leftOperand, SetOperator operator, String rightOperand, SigmaContractor<?> contractor) throws AtomException
    {
        Set<T> elements = (Set<T>) contractor.parseSetOfConstants(rightOperand);

        if(SigmaContractorUtil.isOrdinalContractor(contractor))
            return new SetOrdinalAtom<>(
                    (OrdinalContractor<T>) contractor,
                    leftOperand,
                    operator,
                    elements);

        if(SigmaContractorUtil.isNominalContractor(contractor))
            return new SetNominalAtom<>(
                    (NominalContractor<T>) contractor,
                    leftOperand,
                    operator,
                    elements);

        throw new AtomException("Could not parse set atom with"
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
        SetAtom<?, ?> setAtom = (SetAtom<?, ?>) o;
        return Objects.equals(attribute, setAtom.attribute) && Objects.equals(constants, setAtom.constants);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(attribute);
        result = 31 * result + Objects.hashCode(constants);
        return result;
    }

    @Override
    public String toString() {
        return attribute
            + " "
            + getOperator().getSymbol()
            + " " +
            getContractor()
                .printSetOfConstants(constants);
    }

    @Override
    public String toSqlString(DBMS vendor)
    {
        return vendor
                .getAgent()
                .escaping()
                .apply(attribute)
                + " "
                + getOperator().getSqlSymbol()
                + " "
                + constants
                .stream()
                .map(c -> getContractor().printConstant(c))
                .collect(Collectors.joining(",", "(", ")"));
    }


    @Override
    public AtomType getAtomType()
    {
        return AtomType.SET;
    }
    
    @Override
    public boolean involves(String attribute)
    {
        return this.attribute.equals(attribute);
    }
}
