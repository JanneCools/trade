package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.*;
import java.util.stream.Collectors;

public class AtomOperations {

    public static <T extends Comparable<? super T>> Set<Interval<T>> getIntervalsFromOrdinalAtoms(Set<AbstractAtom<?, ?, ?>> atoms, String attribute) {

        if (atoms.stream().anyMatch(a -> a.getAttributes().anyMatch(at -> at.equals(attribute) && a.getContractor() instanceof NominalContractor))) {
            throw new AtomException("Cannot transform nominal atoms " + atoms + " for attribute " + attribute + " to intervals.");
        }

        Set<ConstantOrdinalAtom<T>> constantAtomsForAttribute = atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.CONSTANT)) && atom.getAttributes().anyMatch(at -> at.equals(attribute)))
                .map(atom -> (ConstantOrdinalAtom<T>) atom)
                .collect(Collectors.toSet());

        Set<SetOrdinalAtom<T>> setAtomsForAttribute = atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.SET)) && atom.getAttributes().anyMatch(at -> at.equals(attribute)))
                .map(atom -> (SetOrdinalAtom<T>) atom)
                .collect(Collectors.toSet());

        List<Set<Interval<T>>> nonVariableAtomIntervals = constantAtomsForAttribute
                .stream()
                .map(ConstantOrdinalAtom::toIntervals)
                .collect(Collectors.toList());

        nonVariableAtomIntervals.addAll(setAtomsForAttribute
                .stream()
                .map(SetOrdinalAtom::toIntervals)
                .collect(Collectors.toSet()));

        if (nonVariableAtomIntervals.isEmpty()) {
            return Collections.singleton(new Interval<>());
        }

        Set<Interval<T>> resultingIntervals = nonVariableAtomIntervals.get(0);

        for (int i = 1; i < nonVariableAtomIntervals.size(); i++) {

            Set<Interval<T>> nextResultingIntervals = new HashSet<>();

            for (Interval<T> resultingInterval: resultingIntervals) {

                for (Interval<T> nonVariableAtomInterval: nonVariableAtomIntervals.get(i)) {

                    Interval<T> intersection = resultingInterval.intersect(nonVariableAtomInterval);

                    if (intersection != null) {
                        nextResultingIntervals.add(intersection);
                    }
                }
            }

            resultingIntervals = new HashSet<>(nextResultingIntervals);

        }

        return resultingIntervals;

    }

    public static <T extends Comparable<? super T>> Set<T> getValuesetFromNominalAtoms(Set<AbstractAtom<?, ?, ?>> atoms, String attribute) {

        if (atoms.stream().anyMatch(a -> a.getAttributes().anyMatch(at -> at.equals(attribute) && a.getContractor() instanceof OrdinalContractor))) {
            throw new AtomException("Cannot transform ordinal atoms " + atoms + " for attribute " + attribute + " to value sets.");
        }

        List<SetNominalAtom<T>> setInAtomsForAttribute = atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.CONSTANT))
                        && atom.getAttributes().anyMatch(at -> at.equals(attribute))
                        && atom.getOperator().equals(EqualityOperator.EQ))
                .map(atom -> (ConstantNominalAtom<T>) atom)
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.IN, Set.of(atom.getConstant())))
                .distinct()
                .collect(Collectors.toList());

        setInAtomsForAttribute.addAll(atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.SET))
                        && atom.getAttributes().anyMatch(at -> at.equals(attribute))
                        && atom.getOperator().equals(SetOperator.IN))
                .map(atom -> (SetNominalAtom<T>) atom).toList());

        List<SetNominalAtom<T>> setNotInAtomsForAttribute = atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.CONSTANT))
                        && atom.getAttributes().anyMatch(at -> at.equals(attribute))
                        && atom.getOperator().equals(EqualityOperator.NEQ))
                .map(atom -> (ConstantNominalAtom<T>) atom)
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.NOTIN, Set.of(atom.getConstant())))
                .distinct()
                .collect(Collectors.toList());

        setNotInAtomsForAttribute.addAll(atoms
                .stream()
                .filter(atom -> (atom.getAtomType().equals(AtomType.SET))
                        && atom.getAttributes().anyMatch(at -> at.equals(attribute))
                        && atom.getOperator().equals(SetOperator.NOTIN))
                .map(atom -> (SetNominalAtom<T>) atom)
                .toList());

        if (setInAtomsForAttribute.isEmpty()) {
            throw new AtomException("Cannot transform set of atoms " + atoms + " for attribute " + attribute + " to value sets if this set does not contain an atom with an 'IN' or an '=' operator.");
        }

        Set<T> valueset = new HashSet<>(setInAtomsForAttribute.get(0).getConstants());

        for (int i = 1; i < setInAtomsForAttribute.size(); i++) {
            valueset.retainAll(setInAtomsForAttribute.get(i).getConstants());
        }

        for (SetNominalAtom<T> tSetNominalAtom : setNotInAtomsForAttribute) {
            valueset.removeAll(tSetNominalAtom.getConstants());
        }

        return valueset;

    }

}
