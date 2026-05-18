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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @param <C> Type of SigmaContractor object used by this atom during evaluation
 * @param <O> Type of the ComparableOperator object used during evaluation
 * @author Toon Boeckling
 * <p>
 * A VariableAtom object consists of two attributes (and an integer constant in case of ordinals)
 * which values are compared by means of a ComparableOperator object.
 * The integer constant n, when used, indicates that the n^th functional power
 * of the successor function is applied to the right attribute. If n is negative,
 * then the n^th functional power of the inverse function (the predecessor function)
 * is applied. If n=0, then S^n is the identity function (S^0(b) := b)
 *
 * Examples include A <= S^1(B), A == B, A > S^-4(B)...
 */
public abstract class VariableAtom<
        T extends Comparable<? super T>,
        C extends SigmaContractor<T>,
        O extends ComparableOperator>
    extends AbstractAtom<T, C, O> {

    public static final String SUCCESSOR         = "S^";
    public static final String SUCCESSOR_PATTERN = "S\\^";
    public static final String VARIO_EXP_PATTERN = "VE\\^";
    public static final String VARIO_LIN_PATTERN = "VL\\^";
    public static final String VARIO_GAU_PATTERN = "VG\\^";
    public static final String VARIO_SPH_PATTERN = "VS\\^";

    private final String leftAttribute;
    private final String rightAttribute;

    /**
     * Create a new VariableAtom object.
     *
     * @param contractor     SigmaContractor object used by this atom during evaluation
     * @param leftAttribute  Left attribute of which the value will be used during evaluation
     * @param operator       ComparableOperator object used during evaluation
     * @param rightAttribute Right attribute of which the value will be used during evaluation
     */
    public VariableAtom(C contractor, String leftAttribute, O operator, String rightAttribute) {
        super(contractor, operator);
        this.leftAttribute = leftAttribute;
        this.rightAttribute = rightAttribute;
    }

    /**
     * Get the left attribute defined within a VariableAtom object.
     *
     * @return left attribute defined within a VariableAtom object
     */
    public String getLeftAttribute() {
        return leftAttribute;
    }

    /**
     * Get the right attribute defined within a VariableAtom object.
     *
     * @return right attribute defined within a VariableAtom object
     */
    public String getRightAttribute() {
        return rightAttribute;
    }

    @Override
    public Stream<String> getAttributes() {
        return Stream.of(leftAttribute, rightAttribute);
    }

    @Override
    public Set<String> getAttributesSet() { return Set.of(leftAttribute, rightAttribute); }

    public abstract AbstractAtom<?, ?, ?> fixRight(DataObject o);

    public abstract AbstractAtom<?, ?, ?> fixLeft(DataObject o);

    @Override
    public boolean test(DataObject o) throws DataException {

        Objects.requireNonNull(o, "DataObject must not be null");

        if(o.get(leftAttribute) == null || o.get(rightAttribute) == null)
            return false;

        if (getContractor().test(o.get(leftAttribute)) &&
                getContractor().test(o.get(rightAttribute))) {
            T leftAttributeValue = getContractor().getFromDataObject(o, leftAttribute);
            T rightAttributeValue = getContractor().getFromDataObject(o, rightAttribute);
            return getOperator().test(leftAttributeValue, rightAttributeValue);
        } else {
            throw new DataException("Passed attribute value " + o.get(leftAttribute) + " or " + o.get(rightAttribute) + " does not fulfill contract specified by " + getContractor());
        }

    }

    /**
     * Static method used for parsing the individual components (method arguments) to a variable atom
     * @param leftOperand Left attribute to parse
     * @param operator Operator object to parse
     * @param rightOperand Right attribute to parse
     * @param contractor SigmaContractor object used by this atom during evaluation
     * @param <T> Datatype of contractors
     * @return AbstractAtom object as a result of parsing the individual components
     * @throws AtomException Exception to throw in case the atom could not be parsed
     */
    public static  <T extends Comparable<? super T>> AbstractAtom<?, ?, ?> parseVariableAtom(String leftOperand, Operator<?,?> operator, String rightOperand, SigmaContractor<?> contractor) throws AtomException
    {
        //Check if both attributes have an ordinal contractor
        if(SigmaContractorUtil.isOrdinalContractor(contractor))
        {
            String rightAttribute = rightOperand.trim();
            int rightConstant = 0;
            Pattern pattern = Pattern.compile(buildSuccessorPattern(""));
            Matcher matcher = pattern.matcher(rightOperand.trim());

            if (matcher.matches())
            {
                rightAttribute  = matcher.group(2);
                rightConstant   = Integer.parseInt(matcher.group(1));
            }

            return new VariableOrdinalAtom<>(
                    (OrdinalContractor<T>) contractor,
                    leftOperand,
                    (ComparableOperator) operator,
                    rightAttribute,
                    rightConstant);
        }
        //Check if both attributes have a nominal contractor
        if(SigmaContractorUtil.isNominalContractor(contractor) )
        {
            return new VariableNominalAtom<>(
                    (NominalContractor<T>) contractor,
                    leftOperand,
                    (EqualityOperator) operator,
                    rightOperand);
        }

        throw new AtomException("Could not parse variable atom with"
                + " left operand '" + leftOperand + "'"
                + " operator '" + operator.getSymbol() + "'"
                + " and right operand '" + rightOperand + "'.");
    }

    public static  <T extends Comparable<? super T>, U extends Comparable<? super U>> AbstractAtom<?, ?, ?> parseVariableVarioAtom(
            String leftOperand, Operator<?,?> operator, String rightOperand, SigmaContractor<?> contractor,
            SigmaContractor<?> varioContractor) throws AtomException
    {
        Pattern patternExp = Pattern.compile(buildVarioPatternExponential(""));
        Matcher matcherExp = patternExp.matcher(rightOperand.trim());

        Pattern patternLin = Pattern.compile(buildVarioPatternLinear(""));
        Matcher matcherLin = patternLin.matcher(rightOperand.trim());

        Pattern patternGau = Pattern.compile(buildVarioPatternGaussian(""));
        Matcher matcherGau = patternGau.matcher(rightOperand.trim());

        Pattern patternSph = Pattern.compile(buildVarioPatternSpherical(""));
        Matcher matcherSph = patternSph.matcher(rightOperand.trim());

        boolean matchExp = matcherExp.matches();
        boolean matchGau = matcherGau.matches();
        boolean matchSph = matcherSph.matches();
        if (matchExp || matchGau || matchSph) {
            Matcher matcher = (matchExp ? matcherExp : (matchGau ? matcherGau : matcherSph));
            String model = (matchExp ? "exponential" : (matchGau ? "gaussian" : "spherical"));
            long partialSill = Integer.parseInt(matcher.group(1));
            long range = Integer.parseInt(matcher.group(2));
            long nugget = Integer.parseInt(matcher.group(3));
            String varioAttribute1 = matcher.group(4);
            String varioAttribute2 = matcher.group(5);
            String rightAttribute = matcher.group(6);

            return new VariableVarioAtom<>(
                    (OrdinalContractor<T>) contractor,
                    leftOperand,
                    (ComparableOperator) operator,
                    rightAttribute,
                    (OrdinalContractor<U>) varioContractor,
                    varioAttribute1,
                    varioAttribute2,
                    model,
                    List.of(partialSill, range, nugget)
            );
        } else if (matcherLin.matches()) {
            long rico = Integer.parseInt(matcherLin.group(1));
            long constant = Integer.parseInt(matcherLin.group(2));
            String varioAttribute1 = matcherLin.group(3);
            String varioAttribute2 = matcherLin.group(4);
            String rightAttribute = matcherLin.group(5);

            return new VariableVarioAtom<>(
                    (OrdinalContractor<T>) contractor,
                    leftOperand,
                    (ComparableOperator) operator,
                    rightAttribute,
                    (OrdinalContractor<U>) varioContractor,
                    varioAttribute1,
                    varioAttribute2,
                    "linear",
                    List.of(rico, constant)
            );
        }

        throw new AtomException("Could not parse variable atom with"
                + " left operand '" + leftOperand + "'"
                + " operator '" + operator.getSymbol() + "'"
                + " and right operand '" + rightOperand + "'.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VariableAtom<?, ?, ?> that = (VariableAtom<?, ?, ?>) o;
        return Objects.equals(leftAttribute, that.leftAttribute) && Objects.equals(rightAttribute, that.rightAttribute);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(leftAttribute);
        result = 31 * result + Objects.hashCode(rightAttribute);
        return result;
    }

    @Override
    public String toString() {
        return leftAttribute + " " + getOperator().getSymbol() + " " + rightAttribute;
    }

    @Override
    public AtomType getAtomType()
    {
        return AtomType.VARIABLE;
    }

    public static String buildSuccessorPattern(String attribute)
    {
        return "(?:" //Open non-captured group
            + SUCCESSOR_PATTERN      //Successor symbol
            + "\\s*"
            + "(\\-?\\s*\\d+)"     //Successor power: group 1
            + "\\s*"
            + ")?"                   //Close non-captured group + make optional
            + "\\("
            + "\\s*"
            + "(" + (attribute.isBlank() ? "[\\S]+" : attribute) +")"
            + "\\s*"
            + "\\)";
    }

    public static String buildVarioPatternExponential(String attribute)
    {
        return "(?:"
                + VARIO_EXP_PATTERN
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + (attribute.isBlank() ? "[\\S]+" : attribute) +")"
                + "\\s*"
                + "\\)";
    }

    public static String buildVarioPatternLinear(String attribute)
    {
        return "(?:"
                + VARIO_LIN_PATTERN
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + (attribute.isBlank() ? "[\\S]+" : attribute) +")"
                + "\\s*"
                + "\\)";
    }

    public static String buildVarioPatternGaussian(String attribute)
    {
        return "(?:"
                + VARIO_GAU_PATTERN
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + (attribute.isBlank() ? "[\\S]+" : attribute) +")"
                + "\\s*"
                + "\\)";
    }

    public static String buildVarioPatternExpGauSph1(String varioAttribute1)
    {
        return "(?:"
                + "(" + VARIO_EXP_PATTERN + "|" + VARIO_GAU_PATTERN + "|" + VARIO_SPH_PATTERN + ")"
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "(" + varioAttribute1 + ")"
                + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*\\)";
    }

    public static String buildVarioPatternLin1(String varioAttribute1) {
        return "(?:"
                + VARIO_LIN_PATTERN
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "(" + varioAttribute1 + ")"
                + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*\\)";
    }

    public static String buildVarioPatternExpGauSph2(String varioAttribute2)
    {
        return "(?:"
                + "(" + VARIO_EXP_PATTERN + "|" + VARIO_GAU_PATTERN + "|" + VARIO_SPH_PATTERN + ")"
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + varioAttribute2 +")"
                + "\\s*,\\s*" + "([\\S]+)" + "\\s*\\)";
    }

    public static String buildVarioPatternLin2(String varioAttribute2) {
        return "(?:"
                + VARIO_LIN_PATTERN
                + "\\s*"
                + "(\\-?\\d+)" + "\\s*,\\s*" + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + varioAttribute2 +")"
                + "\\s*,\\s*" + "([\\S]+)" + "\\s*\\)";
    }

    public static String buildVarioPatternSpherical(String attribute)
    {
        return "(?:"
                + VARIO_SPH_PATTERN
                + "\\s*"
                + "(\\-?\\d+)"
                + "\\s*,\\s*"
                + "(\\-?\\d+)"
                + "\\s*,\\s*"
                + "(\\-?\\d+)"
                + "\\s*)"
                + "\\(\\s*" + "([\\S]+)" + "\\s*,\\s*" + "([\\S]+)" + "\\s*,\\s*"
                + "(" + (attribute.isBlank() ? "[\\S]+" : attribute) +")"
                + "\\s*"
                + "\\)";
    }

    @Override
    public String toSqlString(DBMS vendor)
    {
        return vendor
                .getAgent()
                .escaping()
                .apply(leftAttribute)
                + " "
                + getOperator()
                .getSqlSymbol()
                + " "
                + vendor
                .getAgent()
                .escaping()
                .apply(rightAttribute);
    }

    @Override
    public boolean involves(String attribute)
    {
        return leftAttribute.equals(attribute) || rightAttribute.equals(attribute);
    }

    public abstract VariableAtom<T,C,O> flip();
}