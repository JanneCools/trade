package be.ugent.ledc.sigma.datastructures.formulas;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.atoms.AtomImplicator;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.InequalityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.*;
import java.util.stream.Collectors;

public class CPFImplicator {

    public static CPF imply(CPF cpf) {
        return implyWithAtoms(new CPF(Set.of(AbstractAtom.ALWAYS_TRUE)), cpf.getAtoms());
    }

    public static CPF implyWithAtoms(CPF cpf, Set<AbstractAtom<?, ?, ?>> atoms) {

        List<AbstractAtom<?, ?, ?>> atomsToProcess = atoms
                .stream()
                .filter(at -> !at.isEquivalentTo(AbstractAtom.ALWAYS_TRUE))
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));

        if (atomsToProcess.stream().anyMatch(at -> at.isEquivalentTo(AbstractAtom.ALWAYS_FALSE))) {
            return new CPF(AbstractAtom.ALWAYS_FALSE);
        }

        Set<AbstractAtom<?, ?, ?>> resultingAtoms = new HashSet<>(cpf.getAtoms());

        while (!atomsToProcess.isEmpty()) {

            AbstractAtom<?, ?, ?> atomToProcess = atomsToProcess.remove(0);

            Iterator<AbstractAtom<?, ?, ?>> resultingAtomIterator = resultingAtoms.iterator();

            Set<AbstractAtom<?, ?, ?>> newAtomsToProcess = new HashSet<>();
            boolean addAtomToProcess = true;

            while (resultingAtomIterator.hasNext()) {

                AbstractAtom<?, ?, ?> resultingAtom = resultingAtomIterator.next();
                Set<AbstractAtom<?, ?, ?>> impliedAtoms = new HashSet<>(AtomImplicator.imply(atomToProcess, resultingAtom));

                // Search for an implied atom that is equivalent to the resulting atom
                Optional<AbstractAtom<?, ?, ?>> equivResultingAtom = impliedAtoms.stream().filter(resultingAtom::isEquivalentTo).findFirst();

                if (equivResultingAtom.isEmpty()) {
                    resultingAtomIterator.remove();
                } else {
                    impliedAtoms.remove(equivResultingAtom.get());
                }

                // Search for an implied atom that is equivalent to the atom to process
                Optional<AbstractAtom<?, ?, ?>> equivAtomToProcess = impliedAtoms.stream().filter(atomToProcess::isEquivalentTo).findFirst();

                if (equivAtomToProcess.isEmpty()) {
                    newAtomsToProcess = new HashSet<>(impliedAtoms);
                    addAtomToProcess = false;
                    break;
                } else {
                    impliedAtoms.remove(equivAtomToProcess.get());
                    newAtomsToProcess.addAll(impliedAtoms);
                }

            }

            atomsToProcess.addAll(newAtomsToProcess);

            if (addAtomToProcess) {
                resultingAtoms.add(atomToProcess);
            }

        }

        return new CPF(resultingAtoms);

    }

    public static boolean setIsImpliedBySet(Set<CPF> set1Inverted, Set<CPF> set2Inverted, Set<CPF> set2Implied) {
        boolean implying = true;
        Iterator<CPF> set1Iterator = set1Inverted.iterator();

        while (implying && set1Iterator.hasNext()) {
            CPF cpf1 = set1Iterator.next();

            // check if cpf1 implies any cpf of inverted set 2
            Iterator<CPF> set2Iterator = set2Inverted.iterator();
            boolean implicationFound = false;
            while (!implicationFound && set2Iterator.hasNext()) {
                CPF cpf2 = set2Iterator.next();
                implicationFound = cpf2.getAtoms().stream().allMatch(atom -> atomIsImpliedByCPF(cpf1, atom));
            }

            if (!implicationFound) {
                // check for every CPF of the implied set 2 whether it implies at least one of the atoms of the inverted cpf1
                Set<AbstractAtom<?,?,?>> invertedAtoms = cpf1.getAtoms().stream()
                        .map(AbstractAtom::getInverse)
                        .collect(Collectors.toSet());

                set2Iterator = set2Implied.iterator();
                while (implying && set2Iterator.hasNext()) {
                    CPF cpf2 = set2Iterator.next();

                    Iterator<AbstractAtom<?,?,?>> atomIterator = invertedAtoms.iterator();
                    implicationFound = false;
                    while (!implicationFound && atomIterator.hasNext()) {
                        AbstractAtom<?,?,?> atom = atomIterator.next();
                        implicationFound = atomIsImpliedByCPF(cpf2, atom);
                    }

                    if (!implicationFound)
                        implying = false;
                }
            }
        }

        return implying;
    }

    public static boolean atomIsImpliedByCPF(CPF cpf, AbstractAtom<?, ?, ?> impliedAtom) {

        CPF finalCPF = cpf;

        if (impliedAtom
                .getAttributes()
                .anyMatch(a1 -> finalCPF
                        .getAtoms()
                        .stream()
                        .anyMatch(aa -> aa
                                .getAttributes()
                                .anyMatch(a2 -> a1.equals(a2)
                                        && !aa.getContractor().equals(impliedAtom.getContractor()))))
        ) {
            throw new CPFException("Contractor of " + impliedAtom + " should be equal to contractor of " + cpf + " for same attributes.");
        }

        cpf = CPFImplicator.imply(cpf);

        if (cpf.equals(new CPF(AbstractAtom.ALWAYS_FALSE)) || impliedAtom.isAlwaysTrue()) {
            return true;
        }

        if (cpf.equals(new CPF(AbstractAtom.ALWAYS_TRUE)) || impliedAtom.isAlwaysFalse()) {
            return false;
        }

        if (impliedAtom.getAttributes().noneMatch(iat -> finalCPF.getAtoms().stream().anyMatch(atom -> atom.getAttributes().anyMatch(iat::equals)))) {
            return false;
        }

        // Nominal case
        if (impliedAtom instanceof ConstantNominalAtom<?>) {
            return cpf.getAtoms().stream().anyMatch(at -> AtomImplicator.implies(at, impliedAtom));
        } else if (impliedAtom instanceof SetNominalAtom<?>) {
            return cpf.getAtoms().stream().anyMatch(at -> AtomImplicator.implies(at, impliedAtom));
        } else if (impliedAtom instanceof VariableNominalAtom<?>) {
            return cpf.getAtoms().stream().anyMatch(at -> AtomImplicator.implies(at, impliedAtom))
                    || nonVariableImpliesVariableNominal(cpf.getAtoms(), (VariableNominalAtom<?>) impliedAtom);
        }

        // Ordinal case
        if (impliedAtom instanceof ConstantOrdinalAtom<?>) {
            return nonVariableImpliesConstantOrdinal(cpf.getAtoms(), (ConstantOrdinalAtom<?>) impliedAtom);
        } else if (impliedAtom instanceof SetOrdinalAtom<?>) {
            return nonVariableImpliesSetOrdinal(cpf.getAtoms(), (SetOrdinalAtom<?>) impliedAtom);
        } else if (impliedAtom instanceof VariableOrdinalAtom<?>) {
            return cpf.getAtoms().stream().anyMatch(at -> AtomImplicator.implies(at, impliedAtom))
                    || nonVariableImpliesVariableOrdinal(cpf.getAtoms(), (VariableOrdinalAtom<?>) impliedAtom);
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean nonVariableImpliesVariableNominal(Set<AbstractAtom<?, ?, ?>> atoms, VariableNominalAtom<?> impliedAtom) {

        Set<SetNominalAtom<T>> setAtomsLeft = atoms.stream()
                .filter(atom -> atom.getAtomType().equals(AtomType.SET) && atom.getAttributes().anyMatch(at -> impliedAtom.getLeftAttribute().equals(at)))
                .map(at -> (SetNominalAtom<T>) at)
                .collect(Collectors.toSet());

        Set<SetNominalAtom<T>> setInAtomsLeft = setAtomsLeft.stream()
                .filter(atom -> atom.getOperator().equals(SetOperator.IN))
                .collect(Collectors.toSet());

        Set<SetNominalAtom<T>> setNotInAtomsLeft = setAtomsLeft.stream()
                .filter(atom -> atom.getOperator().equals(SetOperator.NOTIN))
                .collect(Collectors.toSet());

        Set<ConstantNominalAtom<T>> constantAtomsLeft = atoms.stream()
                .filter(atom -> atom.getAtomType().equals(AtomType.CONSTANT) && atom.getAttributes().anyMatch(at -> impliedAtom.getLeftAttribute().equals(at)))
                .map(at -> (ConstantNominalAtom<T>) at)
                .collect(Collectors.toSet());

        setInAtomsLeft.addAll(constantAtomsLeft.stream()
                .filter(atom -> atom.getOperator().equals(EqualityOperator.EQ))
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.IN, Collections.singleton(atom.getConstant())))
                .collect(Collectors.toSet()));

        setNotInAtomsLeft.addAll(constantAtomsLeft.stream()
                .filter(atom -> atom.getOperator().equals(EqualityOperator.NEQ))
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.NOTIN, Collections.singleton(atom.getConstant())))
                .collect(Collectors.toSet()));

        Set<SetNominalAtom<T>> setAtomsRight = atoms.stream()
                .filter(atom -> atom.getAtomType().equals(AtomType.SET) && atom.getAttributes().anyMatch(at -> impliedAtom.getRightAttribute().equals(at)))
                .map(at -> (SetNominalAtom<T>) at)
                .collect(Collectors.toSet());

        Set<SetNominalAtom<T>> setInAtomsRight = setAtomsRight.stream()
                .filter(atom -> atom.getOperator().equals(SetOperator.IN))
                .collect(Collectors.toSet());

        Set<SetNominalAtom<T>> setNotInAtomsRight = setAtomsRight.stream()
                .filter(atom -> atom.getOperator().equals(SetOperator.NOTIN))
                .collect(Collectors.toSet());

        Set<ConstantNominalAtom<T>> constantAtomsRight = atoms.stream()
                .filter(atom -> atom.getAtomType().equals(AtomType.CONSTANT) && atom.getAttributes().anyMatch(at -> impliedAtom.getRightAttribute().equals(at)))
                .map(at -> (ConstantNominalAtom<T>) at)
                .collect(Collectors.toSet());

        setInAtomsRight.addAll(constantAtomsRight.stream()
                .filter(atom -> atom.getOperator().equals(EqualityOperator.EQ))
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.IN, Collections.singleton(atom.getConstant())))
                .collect(Collectors.toSet()));

        setNotInAtomsRight.addAll(constantAtomsRight.stream()
                .filter(atom -> atom.getOperator().equals(EqualityOperator.NEQ))
                .map(atom -> new SetNominalAtom<>(atom.getContractor(), atom.getAttribute(), SetOperator.NOTIN, Collections.singleton(atom.getConstant())))
                .collect(Collectors.toSet()));

        Optional<Set<T>> inConstantsLeft = setInAtomsLeft.stream().map(SetAtom::getConstants).findFirst();
        Optional<Set<T>> notInConstantsLeft = setNotInAtomsLeft.stream().map(SetAtom::getConstants).findFirst();
        Optional<Set<T>> inConstantsRight = setInAtomsRight.stream().map(SetAtom::getConstants).findFirst();
        Optional<Set<T>> notInConstantsRight = setNotInAtomsRight.stream().map(SetAtom::getConstants).findFirst();

        if (impliedAtom.getOperator().equals(EqualityOperator.EQ)) {

            return inConstantsLeft.isPresent() &&
                    inConstantsRight.isPresent() &&
                    inConstantsLeft.get().size() == 1 &&
                    inConstantsRight.get().size() == 1 &&
                    inConstantsLeft.get().stream().findFirst().equals(inConstantsRight.get().stream().findFirst());

        } else if (impliedAtom.getOperator().equals(EqualityOperator.NEQ)) {

            return inConstantsLeft.isPresent() && notInConstantsRight.isPresent() && notInConstantsRight.get().containsAll(inConstantsLeft.get()) ||
                    notInConstantsLeft.isPresent() && inConstantsRight.isPresent() && notInConstantsLeft.get().containsAll(inConstantsRight.get()) ||
                    inConstantsLeft.isPresent() && inConstantsRight.isPresent() && inConstantsLeft.get().stream().noneMatch(c -> inConstantsRight.get().contains(c));

        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean nonVariableImpliesConstantOrdinal(Set<AbstractAtom<?, ?, ?>> atoms, ConstantOrdinalAtom<?> impliedAtom) {

        Set<Interval<T>> intervals = AtomOperations.getIntervalsFromOrdinalAtoms(atoms, impliedAtom.getAttribute());
        Set<Interval<T>> impliedIntervals = ((ConstantOrdinalAtom<T>) impliedAtom).toIntervals();

        return intervals.stream().allMatch(it ->
                impliedIntervals.stream().anyMatch(ii ->
                        ii.allenEquals(it) || ii.allenStartedBy(it) || ii.allenFinishedBy(it) || ii.allenContains(it))
        );

    }

    private static <T extends Comparable<? super T>> boolean nonVariableImpliesSetOrdinal(Set<AbstractAtom<?, ?, ?>> atoms, SetOrdinalAtom<?> impliedAtom) {

        Set<Interval<T>> intervals = AtomOperations.getIntervalsFromOrdinalAtoms(atoms, impliedAtom.getAttribute());
        Set<Interval<T>> impliedIntervals = ((SetOrdinalAtom<T>) impliedAtom).toIntervals();

        return intervals.stream().allMatch(it ->
                impliedIntervals.stream().anyMatch(ii ->
                        ii.allenEquals(it) || ii.allenStartedBy(it) || ii.allenFinishedBy(it) || ii.allenContains(it))
                );

    }

    private static <T extends Comparable<? super T>> boolean nonVariableImpliesVariableOrdinal(Set<AbstractAtom<?, ?, ?>> atoms, VariableOrdinalAtom<?> impliedAtom) {

        Set<Interval<T>> leftIntervals = AtomOperations.getIntervalsFromOrdinalAtoms(atoms, impliedAtom.getLeftAttribute());
        Set<Interval<T>> rightIntervals = AtomOperations.getIntervalsFromOrdinalAtoms(atoms, impliedAtom.getRightAttribute());

        List<Interval<T>> sortedLeftIntervals = leftIntervals.stream().sorted(Interval.leftBoundComparator()).toList();
        List<Interval<T>> sortedRightIntervals = rightIntervals.stream().sorted(Interval.leftBoundComparator()).toList();

        ComparableOperator operator = impliedAtom.getOperator();

        if (operator.equals(InequalityOperator.LEQ)) {

            T leftMax = sortedLeftIntervals.get(sortedLeftIntervals.size() - 1).getRightBound();
            T rightMin = sortedRightIntervals.get(0).getLeftBound();

            return leftMax != null && rightMin != null && leftMax.compareTo(rightMin) <= 0;

        } else if (operator.equals(InequalityOperator.LT)) {

            T leftMax = sortedLeftIntervals.get(sortedLeftIntervals.size() - 1).getRightBound();
            T rightMin = sortedRightIntervals.get(0).getLeftBound();

            return leftMax != null && rightMin != null && leftMax.compareTo(rightMin) < 0;

        } else if (operator.equals(InequalityOperator.GEQ)) {

            T leftMin = sortedLeftIntervals.get(0).getLeftBound();
            T rightMax = sortedRightIntervals.get(sortedRightIntervals.size() - 1).getRightBound();

            return leftMin != null && rightMax != null && leftMin.compareTo(rightMax) >= 0;

        } else if (operator.equals(InequalityOperator.GT)) {

            T leftMin = sortedLeftIntervals.get(0).getLeftBound();
            T rightMax = sortedRightIntervals.get(sortedRightIntervals.size() - 1).getRightBound();

            return leftMin != null && rightMax != null && leftMin.compareTo(rightMax) > 0;

        } else if (operator.equals(EqualityOperator.EQ)) {

            return sortedLeftIntervals.size() == 1
                    && sortedRightIntervals.size() == 1
                    && sortedLeftIntervals.get(0).getLeftBound() != null
                    && sortedLeftIntervals.get(0).getRightBound() != null
                    && sortedLeftIntervals.get(0).getLeftBound().equals(sortedLeftIntervals.get(0).getRightBound())
                    && sortedRightIntervals.get(0).getLeftBound() != null
                    && sortedRightIntervals.get(0).getRightBound() != null
                    && sortedRightIntervals.get(0).getLeftBound().equals(sortedRightIntervals.get(0).getRightBound())
                    && sortedLeftIntervals.get(0).getRightBound().equals(sortedRightIntervals.get(0).getRightBound());

        } else if (operator.equals(EqualityOperator.NEQ)) {
            return sortedLeftIntervals.stream().allMatch(sli -> sortedRightIntervals.stream().allMatch(sli::isDisjunctWith));
        }

        return false;

    }

}
