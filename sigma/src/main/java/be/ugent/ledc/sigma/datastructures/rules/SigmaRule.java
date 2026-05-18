package be.ugent.ledc.sigma.datastructures.rules;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.rules.Rule;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.io.SigmaRuleParser;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * <p>
 * This class represents a sigma rule that keeps a conjunction of atoms.
 */
public class SigmaRule implements Rule<DataObject> {

    private final CPF cpf;

    /**
     * Create a new SigmaRule object.
     *
     * @param cpf The corresponding CPF object of the SigmaRule
     */
    public SigmaRule(CPF cpf) {
        this.cpf = cpf;
    }

    /**
     * Create a new SigmaRule object.
     *
     * @param allAtoms Set of AbstractAtom objects that correspond with the CPF object of the SigmaRule object
     */
    public SigmaRule(Set<AbstractAtom<?, ?, ?>> allAtoms) {
        this.cpf = new CPF(allAtoms);
    }

    /**
     * Create a new SigmaRule object.
     *
     * @param allAtoms Set of AbstractAtom objects that correspond with the CPF object of the SigmaRule object
     */
    public SigmaRule(AbstractAtom<?, ?, ?>... allAtoms) {
        this(Stream.of(allAtoms).collect(Collectors.toSet()));
    }

    /**
     * Get the CPF object that corresponds with in this SigmaRule object.
     *
     * @return the CPF object that corresponds with in this SigmaRule object
     */
    public CPF getCPF() {
        return this.cpf;
    }

    /* ATOM RELATED METHODS */

    public Set<AbstractAtom<?, ?, ?>> getAtoms() {
        return getCPF().getAtoms();
    }

    public Set<ConstantAtom<?, ?, ?>> getConstantAtoms() {
        return getCPF().getConstantAtoms();
    }

    public Set<ConstantOrdinalAtom<?>> getConstantOrdinalAtoms() {
        return getCPF().getConstantOrdinalAtoms();
    }

    public Set<VariableAtom<?, ?, ?>> getVariableAtoms() {
        return getCPF().getVariableAtoms();
    }

    public Set<SetAtom<?, ?>> getSetAtoms() { return getCPF().getSetAtoms(); }

    public Set<SetNominalAtom<?>> getSetNominalAtoms() { return getCPF().getSetNominalAtoms(); }

    public Set<SetOrdinalAtom<?>> getSetOrdinalAtoms() { return getCPF().getSetOrdinalAtoms(); }

    /* SIGNATURE METHODS */

    @Override
    public boolean test(DataObject dataObject)
    {
        return !getCPF().test(dataObject);
    }

    @Override
    public Set<String> getInvolvedAttributes() {
        return getCPF().getAttributes();
    }

    public boolean isAlwaysSatisfied() {
        return getCPF().isContradiction();
    }

    public boolean isAlwaysFailed() {
        return getCPF().isTautology();
    }

    public SigmaRule fix(DataObject dataObject) {
        return new SigmaRule(getCPF().fix(dataObject));
    }

    /* OTHER OPERATIONS */

    public SigmaRule project(Set<String> attributes) {
        return new SigmaRule(getCPF().project(attributes));
    }

    public SigmaRule project(String ... attributes) {
        return new SigmaRule(getCPF().project(attributes));
    }

    public SigmaContractor<?> getContractor(String attribute) {
        return getCPF().getContractor(attribute);
    }

    public boolean isRedundantTo(SigmaRule other) {
        return getCPF().implies(other.getCPF());
    }

    public boolean isEquivalentTo(SigmaRule other) { return getCPF().isEquivalentTo(other.getCPF()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SigmaRule sigmaRule = (SigmaRule) o;
        return Objects.equals(cpf, sigmaRule.getCPF());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cpf);
    }

    @Override
    public String toString() {

        return SigmaRuleParser.NEGATION + " "
            + (cpf.getAtoms().isEmpty()
            ? "TRUE"
            : cpf.getAtoms()
            .stream()
            .map(AbstractAtom::toString)
            .collect(Collectors.joining(" ".concat(CPF.ATOM_SEPARATOR).concat(" "))));
    }
}
