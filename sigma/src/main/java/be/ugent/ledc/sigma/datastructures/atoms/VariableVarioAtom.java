package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;


/**
 * A VariableVarioAtom consists of four attributes and a list of parameters.
 * Given a variogram model, it uses two attributes and the parameters to compute a value n,
 * which indicates that the n^th functional power of the successor function is applied to the right attribute.
 * There are four possible variogram models: exponential, gaussian, linear, and spherical.
 * The formulas for the models are retrieved at <a href="https://geostat-framework.readthedocs.io/projects/pykrige/en/stable/variogram_models.html">PyKrige</a>
 *
 * @param <T> Datatype of the values of the attribute on which the atom operates
 * @param <U> Datatype of the additional attributes needed to compute the variogram function
 */
public class VariableVarioAtom<T extends Comparable<? super T>, U extends Comparable<? super U>>
        extends VariableAtom<T, OrdinalContractor<T>, ComparableOperator> {

    private final OrdinalContractor<U> varioContractor;
    private final String varioAttribute1;
    private final String varioAttribute2;

    private final String model;
    private final List<Long> parameters;

    public VariableVarioAtom(
            OrdinalContractor<T> contractor, String leftAttribute, ComparableOperator operator,
            String rightAttribute, OrdinalContractor<U> varioContractor, String varioAttribute1,
            String varioAttribute2, String model, List<Long> parameters) {
        super(contractor, leftAttribute, operator, rightAttribute);
        this.varioContractor = varioContractor;
        this.varioAttribute1 = varioAttribute1;
        this.varioAttribute2 = varioAttribute2;
        this.model = model;
        this.parameters = parameters;
        verifyParameterList();
    }

    private void verifyParameterList() {
        Map<String, Integer> expectedNumber = Map.of(
                "exponential", 3,
                "linear", 2,
                "gaussian", 3,
                "spherical", 3
        );

        if (parameters.size() != expectedNumber.get(model))
            throw new AtomException("The " + model + " variogram model expects " + expectedNumber.get(model)
                    + " parameters instead of " + parameters.size() + ".");

        if ((model.equals("exponential") || model.equals("gaussian") || model.equals("spherical")) && parameters.get(1) <= 0) {
            throw new AtomException("The range (second parameter) of the " + model
                    + " variogram model must be greater than 0 but is " + parameters.get(1) + ".");
        }
    }

    // getters
    public OrdinalContractor<U> getVarioContractor() { return varioContractor; }
    public String getVarioAttribute1() { return varioAttribute1; }
    public String getVarioAttribute2() { return varioAttribute2; }
    public String getModel() { return model; }
    public List<Long> getParameters() { return parameters; }

    public VariableOrdinalAtom<T> convert(DataObject o) {
        long value = computeSuccessorValue(o);
        return new VariableOrdinalAtom<>(
                getContractor(), getLeftAttribute(), getOperator(), getRightAttribute(), (int)value);
    }

    private long computeSuccessorValue(DataObject o) {
        long diff = varioContractor.absDifferenceInUnits((U)o.get(varioAttribute1), (U)o.get(varioAttribute2));

        switch (model) {
            case "exponential":
                long partialSillExp = parameters.get(0);
                long rangeExp = parameters.get(1);
                long nuggetExp = parameters.get(2);
                double exponentExp = 1 - Math.exp(-diff / (rangeExp / 3.0));
                return Math.round(partialSillExp * exponentExp) + nuggetExp;

            case "linear":
                long rico = parameters.get(0);
                long constant = parameters.get(1);
                return rico * diff + constant;

            case "gaussian":
                long partialSillGau = parameters.get(0);
                long rangeGau = parameters.get(1);
                long nuggetGau = parameters.get(2);
                double exponentGau = - Math.pow(diff, 2) / Math.pow((4 * rangeGau) / 7.0, 2);
                return Math.round(partialSillGau * (1 - Math.exp(exponentGau))) + nuggetGau;

            case "spherical":
                long partialSillSph = parameters.get(0);
                long rangeSph = parameters.get(1);
                long nuggetSph = parameters.get(2);
                // Spherical model reaches its plateau (sill) once distance >= range
                if (diff > rangeSph) {
                    return partialSillSph + nuggetSph;
                }
                double value1 = (3*diff) / (2.0*rangeSph);
                double value2 = Math.pow(diff, 3) / (2.0 * Math.pow(rangeSph, 3));
                return Math.round(partialSillSph * (value1 - value2)) + nuggetSph;

            default:
                return diff;
        }
    }

    @Override
    public AbstractAtom<?, ?, ?> fixLeft(DataObject o)
    {
        // TODO
        Set<String> attr = o.getAttributes();

        if(attr.contains(getLeftAttribute()) && o.get(getLeftAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        if (attr.contains(getLeftAttribute()) && attr.contains(getVarioAttribute2()) && attr.contains(getVarioAttribute1()))
        {
            long successorValue = computeSuccessorValue(o);
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getRightAttribute(), //Right attribute
                    getOperator().getReversedOperator(), //Reverse order operator
                    getContractor().subtract((T) o.get(getLeftAttribute()), successorValue)); //Constant value for left attribute
        }

        return this;
    }

    @Override
    public AbstractAtom<?, ?, ?> fixRight(DataObject o) {
        // TODO
        Set<String> attr = o.getAttributes();

        if(attr.contains(getRightAttribute()) && o.get(getRightAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        if (attr.contains(getRightAttribute()) && attr.contains(getVarioAttribute2()) && attr.contains(getVarioAttribute1()))
        {
            long successorValue = computeSuccessorValue(o);
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getLeftAttribute(), //Left attribute
                    getOperator(), //Same operator
                    getContractor().add((T) o.get(getRightAttribute()), successorValue)); //Constant value for right attribute
        }

        return this;
    }

    @Override
    public AbstractAtom<?, ?, ?> fix(DataObject o) {
        Set<String> attr = o.getAttributes();

        if(attr.contains(getLeftAttribute()) && o.get(getLeftAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        if(attr.contains(getRightAttribute()) && o.get(getRightAttribute()) == null)
            return AbstractAtom.ALWAYS_FALSE;

        //If none of the attributes are present in o, we just return this atom
        if (getAttributes().noneMatch(attr::contains) && !attr.contains(varioAttribute2) && !attr.contains(varioAttribute1))
            return this;
        //If the vario attributes are not present in o, we return this atom
        else if (!attr.contains(getVarioAttribute1()) || !attr.contains(getVarioAttribute2()))
            return this;
        else if (!attr.contains(getLeftAttribute()) && attr.contains(getRightAttribute())) {
            long successorValue = computeSuccessorValue(o);
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getLeftAttribute(), //Left attribute
                    getOperator(), //Same operator
                    getContractor().add((T) o.get(getRightAttribute()), successorValue)); //Constant value for right attribute
        } else if (attr.contains(getLeftAttribute()) && !attr.contains(getRightAttribute())) {
            long successorValue = computeSuccessorValue(o);
            return new ConstantOrdinalAtom<>(
                    getContractor(),
                    getRightAttribute(), //Right attribute
                    getOperator().getReversedOperator(), //Reverse order operator
                    getContractor().subtract((T) o.get(getLeftAttribute()), successorValue)); //Constant value for left attribute
        } else if (!attr.contains(getLeftAttribute()) && !attr.contains(getRightAttribute())) {
            long successorValue = computeSuccessorValue(o);
            return new VariableOrdinalAtom<>(
                    getContractor(),
                    getLeftAttribute(),
                    getOperator(),
                    getRightAttribute(),
                    (int)successorValue
            );
        } else
            return test(o) ? AbstractAtom.ALWAYS_TRUE : AbstractAtom.ALWAYS_FALSE;
    }

    @Override
    public VariableVarioAtom<?,?> getInverse() {
        return new VariableVarioAtom<>(
                this.getContractor(),
                this.getLeftAttribute(),
                this.getOperator().getInverseOperator(),
                this.getRightAttribute(),
                this.getVarioContractor(),
                this.getVarioAttribute1(),
                this.getVarioAttribute2(),
                this.getModel(),
                this.getParameters());
    }

    @Override
    public boolean test(DataObject o) throws DataException {

        Objects.requireNonNull(o, "DataObject must not be null");

        if (o.get(getLeftAttribute()) == null || o.get(getRightAttribute()) == null
                || o.get(getVarioAttribute1()) == null || o.get(getVarioAttribute2()) == null)
            return false;

        if (getContractor().test(o.get(getLeftAttribute())) &&
                getContractor().test(o.get(getRightAttribute())) &&
                getVarioContractor().test(o.get(getVarioAttribute1())) &&
                getVarioContractor().test(o.get(getVarioAttribute2())))
        {
            long successorValue = computeSuccessorValue(o);
            T leftAttributeValue = getContractor().getFromDataObject(o, getLeftAttribute());
            T rightAttributeValue = getContractor().add(getContractor().getFromDataObject(o, getRightAttribute()), successorValue);
            return getOperator().test(leftAttributeValue, rightAttributeValue);
        }
        else {
            throw new DataException("Passed attribute value "
                    + o.get(getLeftAttribute())
                    + ", " + o.get(getRightAttribute())
                    + ", " + o.get(getVarioAttribute1())
                    + " or " + o.get(getVarioAttribute2())
                    + " does not fulfill contract specified by "
                    + getContractor());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VariableVarioAtom<?,?> that = (VariableVarioAtom<?,?>) o;
        return varioAttribute1.equals(that.varioAttribute1) && varioAttribute2.equals(that.varioAttribute2)
                && model.equals(that.model) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + model.hashCode();
        for (long p: parameters)
            result = 31 * result + Objects.hashCode(p);
        return result;
    }

    @Override
    public String toString() {
        switch (model) {
            case "exponential":
                long partialSillExp = parameters.get(0);
                long rangeExp = parameters.get(1);
                long nuggetExp = parameters.get(2);
                return getLeftAttribute() + " " + getOperator().getSymbol() + " "
                        + SUCCESSOR + "[" + partialSillExp
                        + " * (1 - e^-3(" + getVarioAttribute1() + " - " + getVarioAttribute2() + ")/" + rangeExp
                        + ") + " + nuggetExp
                        + "] (" + getRightAttribute() + ")";

            case "linear":
                long rico = parameters.get(0);
                long constant = parameters.get(1);
                return getLeftAttribute() + " " + getOperator().getSymbol() + " "
                        + SUCCESSOR + "["
                        + rico + " * (" + getVarioAttribute1() + " - " + getVarioAttribute2() + ")"
                        + (constant > 0 ? " + " : " - ") + Math.abs(constant) + "] ("
                        + getRightAttribute()
                        + ")";

            case "gaussian":
                long partialSillGau = parameters.get(0);
                long rangeGau = parameters.get(1);
                long nuggetGau = parameters.get(2);
                BigDecimal denominator = BigDecimal.valueOf(Math.pow(4 * rangeGau / 7.0, 2));
                return getLeftAttribute() + " " + getOperator().getSymbol() + " "
                        + SUCCESSOR + "[" + partialSillGau
                        + " * (1 - e^-(" + getVarioAttribute1() + " - " + getVarioAttribute2() + ")²/" + denominator.setScale(2, RoundingMode.HALF_UP) + ")"
                        + " + " + nuggetGau
                        + "] (" + getRightAttribute() + ")";

            case "spherical":
                long partialSillSph = parameters.get(0);
                long rangeSph = parameters.get(1);
                long nuggetSph = parameters.get(2);
                BigDecimal denom1 = BigDecimal.valueOf(2 * rangeSph);
                BigDecimal denom2 = BigDecimal.valueOf(2 * Math.pow(rangeSph, 3));
                return getLeftAttribute() + " " + getOperator().getSymbol() + " "
                        + SUCCESSOR + "[" + partialSillSph + " * ("
                        + "3*(" + getVarioAttribute1() + " - " + getVarioAttribute2() + ")/" + denom1
                        + " - (" + getVarioAttribute1() + " - " + getVarioAttribute2() + ")³/" + denom2 + ")"
                        + " + " + nuggetSph
                        + "] (" + getRightAttribute() + ")";

            default:
                return super.toString();
        }
    }

    @Override
    public VariableVarioAtom<T,U> nameTransform(Function<String, String> nameTransform)
    {
        return new VariableVarioAtom<>(
                getContractor(),
                nameTransform.apply(getLeftAttribute()),
                getOperator(),
                nameTransform.apply(getRightAttribute()),
                getVarioContractor(),
                nameTransform.apply(getVarioAttribute1()),
                nameTransform.apply(getVarioAttribute2()),
                getModel(),
                getParameters()
        );
    }

    @Override
    public VariableVarioAtom<T,U> flip()
    {
        switch (model) {
            case "exponential", "gaussian", "spherical":
                long partialSillExp = parameters.get(0);
                long rangeExp = parameters.get(1);
                long nuggetExp = parameters.get(2);
                return new VariableVarioAtom<>(
                        getContractor(),
                        getRightAttribute(),
                        getOperator().getReversedOperator(),
                        getLeftAttribute(),
                        getVarioContractor(),
                        getVarioAttribute1(),
                        getVarioAttribute2(),
                        getModel(),
                        List.of(-1*partialSillExp, rangeExp, -1*nuggetExp)
                );

            case "linear":
                long rico = parameters.get(0);
                long constant = parameters.get(1);
                return new VariableVarioAtom<>(
                        getContractor(),
                        getRightAttribute(),
                        getOperator().getReversedOperator(),
                        getLeftAttribute(),
                        getVarioContractor(),
                        getVarioAttribute1(),
                        getVarioAttribute2(),
                        getModel(),
                        List.of(rico * -1, constant * -1)
                );

            default:
                return this;
        }
    }

}