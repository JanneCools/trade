package be.ugent.ledc.sigma.datastructures.formulas;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class models a Conjunctive Propositional Formula (CPF), which is a conjunctive combination of a set of atoms.
 * The atoms are stored in a set and are assumed to be concatenated by means of a logical conjunction.
 * @author abronsel
 */
public class CPF implements PropositionalFormula {

    private final Set<AbstractAtom<?, ?, ?>> atoms;
    private final Set<String> involvedAttributes;

    public static final String ATOM_SEPARATOR = "&";

    public CPF(Set<AbstractAtom<?, ?, ?>> atoms) {
        this.atoms = atoms;
        this.involvedAttributes = atoms.stream().flatMap(Atom::getAttributes).collect(Collectors.toSet());
    }

    public CPF(Set<AbstractAtom<?, ?, ?>> atoms, Set<String> involvedAttributes) {
        this.atoms = atoms;
        this.involvedAttributes = involvedAttributes;
    }

    public CPF(AbstractAtom<?, ?, ?>... atoms) {
        this(Stream.of(atoms).collect(Collectors.toSet()));
    }

    /* ATOM RELATED METHODS */

    /**
     * Get the set of AbstractAtoms objects that correspond with in this CPF object.
     *
     * @return set of AbstractAtoms objects that correspond with in this CPF object
     */
    public Set<AbstractAtom<?, ?, ?>> getAtoms() {
        return atoms;
    }

    /**
     * Get all ConstantAtom objects in CPF object.
     *
     * @return all ConstantAtom objects in CPF object
     */
    public Set<ConstantAtom<?, ?, ?>> getConstantAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a.getAtomType() == AtomType.CONSTANT)
                .map(a -> (ConstantAtom<?, ?, ?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all ConstantOrdinalAtom objects in CPF object.
     *
     * @return all ConstantOrdinalAtom objects in CPF object
     */
    public Set<ConstantOrdinalAtom<?>> getConstantOrdinalAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a instanceof ConstantOrdinalAtom)
                .map(a -> (ConstantOrdinalAtom<?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all VariableAtom objects in this CPF
     *
     * @return all VariableAtom objects in CPF object
     */
    public Set<VariableAtom<?, ?, ?>> getVariableAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a.getAtomType().equals(AtomType.VARIABLE))
                .map(a -> (VariableAtom<?, ?, ?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all VariableOrdinalAtom objects in this CPF
     *
     * @return all VariableOrdinalAtom objects in CPF object
     */
    public Set<VariableOrdinalAtom<?>> getVariableOrdinalAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a instanceof VariableOrdinalAtom)
                .map(a -> (VariableOrdinalAtom<?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all SetAtom objects in this CPF
     *
     * @return all SetAtom objects in CPF object
     */
    public Set<SetAtom<?, ?>> getSetAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a.getAtomType().equals(AtomType.SET))
                .map(a -> (SetAtom<?, ?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all SetAtom objects in CPF object that feature a NominalContractor object.
     *
     * @return all SetAtom objects in CPF object that feature a NominalContractor object
     */
    public Set<SetNominalAtom<?>> getSetNominalAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a instanceof SetNominalAtom)
                .map(a -> (SetNominalAtom<?>) a)
                .collect(Collectors.toSet());
    }

    /**
     * Get all SetAtom objects in CPF object that feature an OrdinalContractor object.
     *
     * @return all SetAtom objects in CPF object that feature an OrdinalContractor object
     */
    public Set<SetOrdinalAtom<?>> getSetOrdinalAtoms() {
        return getAtoms()
                .stream()
                .filter(a -> a instanceof SetOrdinalAtom)
                .map(a -> (SetOrdinalAtom<?>) a)
                .collect(Collectors.toSet());
    }

    public boolean hasVariableAtoms() {
        return !getVariableAtoms().isEmpty();
    }

    /* SIGNATURE METHODS */

    @Override
    public boolean test(DataObject dataObject) {
        return getAtoms()
                .stream()
                .allMatch(a -> a.test(dataObject));
    }

    @Override
    public Set<String> getAttributes() { return involvedAttributes; }

    public boolean implies(CPF other) {
        return other
                .getAtoms()
                .stream()
                .allMatch(atom -> CPFImplicator.atomIsImpliedByCPF(this, atom));
    }

    public boolean isEquivalentTo(CPF other) {
        return implies(other) && other.implies(this);
    }

    @Override
    public boolean isTautology() {
        return this.isEquivalentTo(new CPF(AbstractAtom.ALWAYS_TRUE));
    }

    @Override
    public boolean isContradiction() {
        return this.isEquivalentTo(new CPF(AbstractAtom.ALWAYS_FALSE));
    }

    @Override
    public CPF fix(DataObject o) {
        return new CPF(getAtoms()
                .stream()
                .map(atom -> atom.fix(o))
                .collect(Collectors.toSet())
        );
    }

    public CPF project(Set<String> attributes) {
        return new CPF(getAtoms()
                .stream()
                .filter(atom -> atom
                        .getAttributes()
                        .allMatch(attributes::contains))
                .collect(Collectors.toSet())
        );
    }

    public CPF project(String ... attributes) {
        return project(Stream.of(attributes).collect(Collectors.toSet()));
    }

    public SigmaContractor<?> getContractor(String attribute) {
        return involvedAttributes.contains(attribute)
            ? atoms
                .stream()
                .filter(atom -> atom
                    .getAttributes()
                    .anyMatch(aa -> aa.equals(attribute)))
                .findFirst()
                .get()
                .getContractor()
            : null;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.atoms);
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
        final CPF other = (CPF) obj;
        return Objects.equals(this.atoms, other.atoms);
    }

    @Override
    public String toString()
    {
        return atoms
            .stream()
            .map(AbstractAtom::toString)
            .collect(Collectors.joining(" ".concat(ATOM_SEPARATOR).concat(" ")));
    }
}
