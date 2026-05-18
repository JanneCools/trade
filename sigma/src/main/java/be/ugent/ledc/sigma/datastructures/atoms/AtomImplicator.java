package be.ugent.ledc.sigma.datastructures.atoms;

import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.ComparableOperator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.InequalityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;

import java.util.*;
import java.util.stream.Collectors;

public class AtomImplicator {

    public static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> imply(AbstractAtom<?, ?, ?> atom1, AbstractAtom<?, ?, ?> atom2) {

        if (atom1.isAlwaysFalse() || atom2.isAlwaysFalse()) {
            return Set.of(AbstractAtom.ALWAYS_FALSE);
        }

        if (atom1.isAlwaysTrue() && atom2.isAlwaysTrue()) {
            return Set.of(AbstractAtom.ALWAYS_TRUE);
        }

        if (atom1.isAlwaysTrue()) {
            return Set.of(atom2);
        }

        if (atom2.isAlwaysTrue()) {
            return Set.of(atom1);
        }

        if (atom1.getAttributes().noneMatch(at1 -> atom2.getAttributes().anyMatch(at1::equals))) {
            return Set.of(atom1, atom2);
        }

        if (!atom1.getContractor().equals(atom2.getContractor())) {
            throw new AtomException("Contractor of " + atom1 + " should be equal to contractor of " + atom2);
        }

        // Nominal case
        if (atom1 instanceof ConstantNominalAtom<?> && atom2 instanceof ConstantNominalAtom<?>) {
            return implyTwoConstantNominalAtoms((ConstantNominalAtom<T>) atom1, (ConstantNominalAtom<T>) atom2);
        } else if (atom1 instanceof ConstantNominalAtom<?> && atom2 instanceof SetNominalAtom<?>) {
            return implyConstantSetNominalAtoms((ConstantNominalAtom<T>) atom1, (SetNominalAtom<T>) atom2);
        } else if (atom1 instanceof SetNominalAtom<?> && atom2 instanceof ConstantNominalAtom<?>) {
            return implyConstantSetNominalAtoms((ConstantNominalAtom<T>) atom2, (SetNominalAtom<T>) atom1);
        } else if (atom1 instanceof SetNominalAtom<?> && atom2 instanceof SetNominalAtom<?>) {
            return implyTwoSetAtoms((SetNominalAtom<T>) atom1, (SetNominalAtom<T>) atom2);
        } else if (atom1 instanceof ConstantNominalAtom<?> && atom2 instanceof VariableNominalAtom<?>) {
            return implyConstantVariableNominalAtoms((ConstantNominalAtom<T>) atom1, (VariableNominalAtom<T>) atom2);
        } else if (atom1 instanceof VariableNominalAtom<?> && atom2 instanceof ConstantNominalAtom<?>) {
            return implyConstantVariableNominalAtoms((ConstantNominalAtom<T>) atom2, (VariableNominalAtom<T>) atom1);
        } else if (atom1 instanceof SetNominalAtom<?> && atom2 instanceof VariableNominalAtom<?>) {
            return implySetVariableNominalAtoms((SetNominalAtom<T>) atom1, (VariableNominalAtom<T>) atom2);
        } else if (atom1 instanceof VariableNominalAtom<?> && atom2 instanceof SetNominalAtom<?>) {
            return implySetVariableNominalAtoms((SetNominalAtom<T>) atom2, (VariableNominalAtom<T>) atom1);
        } else if (atom1 instanceof VariableNominalAtom<?> && atom2 instanceof VariableNominalAtom<?>) {
            return implyTwoVariableNominalAtoms((VariableNominalAtom<T>) atom1, (VariableNominalAtom<T>) atom2);
        }

        // Ordinal case
        else if (atom1 instanceof ConstantOrdinalAtom<?> && atom2 instanceof ConstantOrdinalAtom<?>) {
            return implyTwoConstantOrdinalAtoms((ConstantOrdinalAtom<T>) atom1, (ConstantOrdinalAtom<T>) atom2);
        } else if (atom1 instanceof ConstantOrdinalAtom<?> && atom2 instanceof SetOrdinalAtom<?>) {
            return implyConstantSetOrdinalAtoms((ConstantOrdinalAtom<T>) atom1, (SetOrdinalAtom<T>) atom2);
        } else if (atom1 instanceof SetOrdinalAtom<?> && atom2 instanceof ConstantOrdinalAtom<?>) {
            return implyConstantSetOrdinalAtoms((ConstantOrdinalAtom<T>) atom2, (SetOrdinalAtom<T>) atom1);
        } else if (atom1 instanceof SetOrdinalAtom<?> && atom2 instanceof SetOrdinalAtom<?>) {
            return implyTwoSetAtoms((SetOrdinalAtom<T>) atom1, (SetOrdinalAtom<T>) atom2);
        } else if (atom1 instanceof ConstantOrdinalAtom<?> && atom2 instanceof VariableOrdinalAtom<?>) {
            return implyConstantVariableOrdinalAtoms((ConstantOrdinalAtom<T>) atom1, (VariableOrdinalAtom<T>) atom2);
        } else if (atom1 instanceof VariableOrdinalAtom<?> && atom2 instanceof ConstantOrdinalAtom<?>) {
            return implyConstantVariableOrdinalAtoms((ConstantOrdinalAtom<T>) atom2, (VariableOrdinalAtom<T>) atom1);
        } else if (atom1 instanceof SetOrdinalAtom<?> && atom2 instanceof VariableOrdinalAtom<?>) {
            return implySetVariableOrdinalAtoms((SetOrdinalAtom<T>) atom1, (VariableOrdinalAtom<T>) atom2);
        } else if (atom1 instanceof VariableOrdinalAtom<?> && atom2 instanceof SetOrdinalAtom<?>) {
            return implySetVariableOrdinalAtoms((SetOrdinalAtom<T>) atom2, (VariableOrdinalAtom<T>) atom1);
        } else if (atom1 instanceof VariableOrdinalAtom<?> && atom2 instanceof VariableOrdinalAtom<?>) {
            return implyTwoVariableOrdinalAtoms((VariableOrdinalAtom<T>) atom1, (VariableOrdinalAtom<T>) atom2);
        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    /** NOMINAL CASES **/

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyTwoConstantNominalAtoms(ConstantNominalAtom<T> atom1, ConstantNominalAtom<T> atom2) {

        String attribute = atom1.getAttribute();

        EqualityOperator operator1 = atom1.getOperator();
        EqualityOperator operator2 = atom2.getOperator();

        T constant1 = atom1.getConstant();
        T constant2 = atom2.getConstant();

        NominalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.EQ)) {
            if (constant1.equals(constant2)) {
                return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            }
        } else if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.NEQ)) {
            if (!constant1.equals(constant2)) {
                return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            }
        } else if (operator1.equals(EqualityOperator.NEQ) && operator2.equals(EqualityOperator.EQ)) {
            if (!constant1.equals(constant2)) {
                return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
            }
        } else if (operator1.equals(EqualityOperator.NEQ) && operator2.equals(EqualityOperator.NEQ)) {

            Set<T> newConstants = new HashSet<>(Arrays.asList(constant1, constant2));

            if (newConstants.size() > 1) {
                return Set.of(new SetNominalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants));
            } else {
                return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1));
            }

        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyConstantSetNominalAtoms(ConstantNominalAtom<T> atom1, SetNominalAtom<T> atom2) {

        String attribute = atom1.getAttribute();

        EqualityOperator operator1 = atom1.getOperator();
        SetOperator operator2 = atom2.getOperator();

        T constant1 = atom1.getConstant();
        Set<T> constants2 = atom2.getConstants();

        NominalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(EqualityOperator.EQ)) {
            if ((operator2.equals(SetOperator.IN) && constants2.contains(constant1)) || (operator2.equals(SetOperator.NOTIN) && !constants2.contains(constant1))) {
                return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            }
        } else if (operator1.equals(EqualityOperator.NEQ)) {

            if (operator2.equals(SetOperator.IN)) {

                Set<T> newConstants = new HashSet<>(constants2);
                newConstants.remove(constant1);

                if (newConstants.size() > 1) {
                    return Set.of(new SetNominalAtom<>(contractor, attribute, SetOperator.IN, newConstants));
                } else if (newConstants.size() == 1) {
                    return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.EQ, newConstants.stream().findFirst().get()));
                }

            } else if (operator2.equals(SetOperator.NOTIN)) {

                Set<T> newConstants = new HashSet<>(constants2);
                newConstants.add(constant1);

                if (newConstants.size() > 1) {
                    return Set.of(new SetNominalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants));
                } else {
                    return Set.of(new ConstantNominalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get()));
                }

            }
        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyTwoSetAtoms(SetAtom<T, ?> atom1, SetAtom<T, ?> atom2) {

        String attribute = atom1.getAttribute();

        SetOperator operator1 = atom1.getOperator();
        SetOperator operator2 = atom2.getOperator();

        Set<T> constants1 = atom1.getConstants();
        Set<T> constants2 = atom2.getConstants();

        if (operator1.equals(SetOperator.IN) || operator2.equals(SetOperator.IN)) {

            Set<T> newConstants = new HashSet<>();

            if (operator1.equals(SetOperator.IN) && operator2.equals(SetOperator.IN)) {
                newConstants = new HashSet<>(constants1);
                newConstants.retainAll(constants2);
            } else if (operator2.equals(SetOperator.NOTIN)) {
                newConstants = new HashSet<>(constants1);
                newConstants.removeAll(constants2);
            } else if (operator1.equals(SetOperator.NOTIN)) {
                newConstants = new HashSet<>(constants2);
                newConstants.removeAll(constants1);
            }

            if (newConstants.size() > 1) {
                if (atom1.getContractor() instanceof NominalContractor) {
                    return Set.of(new SetNominalAtom<>((NominalContractor<T>) atom1.getContractor(), attribute, SetOperator.IN, newConstants));
                } else {
                    return Set.of(new SetOrdinalAtom<>((OrdinalContractor<T>) atom1.getContractor(), attribute, SetOperator.IN, newConstants));
                }
            } else if (newConstants.size() == 1) {
                if (atom1.getContractor() instanceof NominalContractor) {
                    return Set.of(new ConstantNominalAtom<>((NominalContractor<T>) atom1.getContractor(), attribute, EqualityOperator.EQ, newConstants.stream().findFirst().get()));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>((OrdinalContractor<T>) atom1.getContractor(), attribute, EqualityOperator.EQ, newConstants.stream().findFirst().get()));
                }
            }

        } else {

            Set<T> newConstants = new HashSet<>(constants1);
            newConstants.addAll(constants2);

            if (newConstants.size() > 1) {
                if (atom1.getContractor() instanceof NominalContractor) {
                    return Set.of(new SetNominalAtom<>((NominalContractor<T>) atom1.getContractor(), attribute, SetOperator.NOTIN, newConstants));
                } else {
                    return Set.of(new SetOrdinalAtom<>((OrdinalContractor<T>) atom1.getContractor(), attribute, SetOperator.NOTIN, newConstants));
                }
            } else if (newConstants.size() == 1) {
                if (atom1.getContractor() instanceof NominalContractor) {
                    return Set.of(new ConstantNominalAtom<>((NominalContractor<T>) atom1.getContractor(), attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get()));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>((OrdinalContractor<T>) atom1.getContractor(), attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get()));
                }
            }

        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyConstantVariableNominalAtoms(ConstantNominalAtom<T> atom1, VariableNominalAtom<T> atom2) {

        String attribute1 = atom1.getAttribute();
        String attribute2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

        EqualityOperator operator1 = atom1.getOperator();
        ComparableOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

        T constant = atom1.getConstant();

        NominalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.EQ)) {
            return Set.of(
                    new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                    new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.EQ, constant)
            );
        } else if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.NEQ)) {
            return Set.of(
                    new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                    new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.NEQ, constant)
            );
        } else if (operator1.equals(EqualityOperator.NEQ) && operator2.equals(EqualityOperator.EQ)) {
            return Set.of(
                    new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, constant),
                    new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.NEQ, constant),
                    new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2)
            );
        }

        return Set.of(
                new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, constant),
                new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2)
        );

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implySetVariableNominalAtoms(SetNominalAtom<T> atom1, VariableNominalAtom<T> atom2) {

        String attribute1 = atom1.getAttribute();
        String attribute2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

        SetOperator operator1 = atom1.getOperator();
        ComparableOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

        Set<T> constants = atom1.getConstants();

        NominalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(SetOperator.IN)) {
            if (operator2.equals(EqualityOperator.EQ)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.EQ, constant)
                    );

                } else if (constants.size() > 1) {
                    return Set.of(
                            new SetNominalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new SetNominalAtom<>(contractor, attribute2, SetOperator.IN, constants),
                            new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2)
                    );
                }

            } else if (operator2.equals(EqualityOperator.NEQ) && constants.size() == 1) {

                T constant = constants.stream().findFirst().get();

                return Set.of(
                        new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.NEQ, constant)
                );

            }

        } else if (operator1.equals(SetOperator.NOTIN) && operator2.equals(EqualityOperator.EQ)) {
            if (constants.size() == 1) {

                T constant = constants.stream().findFirst().get();

                return Set.of(
                        new ConstantNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, constant),
                        new ConstantNominalAtom<>(contractor, attribute2, EqualityOperator.NEQ, constant),
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2)
                );

            } else {
                return Set.of(
                        new SetNominalAtom<>(contractor, attribute1, SetOperator.NOTIN, constants),
                        new SetNominalAtom<>(contractor, attribute2, SetOperator.NOTIN, constants),
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2)
                );
            }
        }

        return Set.of(
            new SetNominalAtom<>(contractor, attribute1, operator1, constants),
            new VariableNominalAtom<>(contractor, attribute1, (EqualityOperator) operator2, attribute2)
        );

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyTwoVariableNominalAtoms(VariableNominalAtom<T> atom1, VariableNominalAtom<T> atom2) {

        Set<String> commonAttributes = atom1
                .getAttributes()
                .filter(at1 -> atom2.getAttributes().anyMatch(at1::equals))
                .collect(Collectors.toSet());

        NominalContractor<T> contractor = atom1.getContractor();

        if (commonAttributes.size() == 1) {

            String commonAttribute = commonAttributes.stream().findFirst().get();

            String attribute1 = atom1.getLeftAttribute().equals(commonAttribute) ? atom1.getRightAttribute() : atom1.getLeftAttribute();
            String attribute2 = atom2.getLeftAttribute().equals(commonAttribute) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

            EqualityOperator operator1 = atom1.getRightAttribute().equals(commonAttribute) ? atom1.getOperator() : (EqualityOperator) atom1.getOperator().getReversedOperator();
            EqualityOperator operator2 = atom2.getLeftAttribute().equals(commonAttribute) ? atom2.getOperator() : (EqualityOperator) atom2.getOperator().getReversedOperator();

            if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute),
                        new VariableNominalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2),
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2)
                );
            } else if (operator1.equals(EqualityOperator.EQ) && operator2.equals(EqualityOperator.NEQ)) {
                return Set.of(
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute),
                        new VariableNominalAtom<>(contractor, commonAttribute, EqualityOperator.NEQ, attribute2),
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2)
                );
            } else if (operator1.equals(EqualityOperator.NEQ) && operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, commonAttribute),
                        new VariableNominalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2),
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2)
                );
            } else {
                return Set.of(
                        new VariableNominalAtom<>(contractor, attribute1, EqualityOperator.NEQ, commonAttribute),
                        new VariableNominalAtom<>(contractor, commonAttribute, EqualityOperator.NEQ, attribute2)
                );
            }

        } else {

            String attribute1 = atom1.getLeftAttribute();
            String attribute2 = atom1.getRightAttribute();

            EqualityOperator operator1 = atom1.getOperator();
            EqualityOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : (EqualityOperator) atom2.getOperator().getReversedOperator();

            if (operator1.equals(operator2)) {
                return Set.of(
                    new VariableNominalAtom<>(contractor, attribute1, operator1, attribute2)
                );
            }

            return Set.of(AbstractAtom.ALWAYS_FALSE);

        }

    }

    /** ORDINAL CASES **/

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyTwoConstantOrdinalAtoms(ConstantOrdinalAtom<T> atom1, ConstantOrdinalAtom<T> atom2) {

        String attribute = atom1.getAttribute();

        ComparableOperator operator1 = atom1.getOperator();
        ComparableOperator operator2 = atom2.getOperator();

        T constant1 = atom1.getConstant();
        T constant2 = atom2.getConstant();

        OrdinalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(InequalityOperator.LEQ)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, Collections.min(Arrays.asList(constant1, constant2))));
            } else if (operator2.equals(InequalityOperator.LT)) {
                if (constant1.compareTo(constant2) < 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant1));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant2));
                }
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
                } else if (constant1.compareTo(constant2) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GT)) {
                if (constant1.compareTo(contractor.next(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
                } else if (constant1.compareTo(contractor.next(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant2)
                    );
                }
            } else if (operator2.equals(EqualityOperator.EQ) && constant1.compareTo(constant2) >= 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
            } else if (operator2.equals(EqualityOperator.NEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, contractor.previous(constant1)));
                } else if (constant1.compareTo(constant2) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant2)
                    );
                } else if (constant1.compareTo(constant2) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant1)
                    );
                }
            }
        } else if (operator1.equals(InequalityOperator.LT)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                if (constant1.compareTo(constant2) <= 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant1));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant2));
                }
            } else if (operator2.equals(InequalityOperator.LT)) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, Collections.min(Arrays.asList(constant1, constant2))));
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                if (constant1.compareTo(contractor.next(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
                } else if (constant1.compareTo(contractor.next(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GT)) {
                if (contractor.previous(constant1).compareTo(contractor.next(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, contractor.previous(constant1)));
                } else if (contractor.previous(constant1).compareTo(contractor.next(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant2)
                    );
                }
            } else if (operator2.equals(EqualityOperator.EQ) && constant1.compareTo(constant2) > 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
            } else if (operator2.equals(EqualityOperator.NEQ)) {
                if (constant1.compareTo(contractor.next(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, contractor.previous(constant1)));
                } else if (constant1.compareTo(contractor.next(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant2)
                    );
                }  else if (constant1.compareTo(contractor.next(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant1)
                    );
                }
            }
        } else if (operator1.equals(InequalityOperator.GEQ)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
                } else if (constant1.compareTo(constant2) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.LT)) {
                if (constant1.compareTo(contractor.previous(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
                } else if (constant1.compareTo(contractor.previous(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, Collections.max(Arrays.asList(constant1, constant2))));
            } else if (operator2.equals(InequalityOperator.GT)) {
                if (constant1.compareTo(constant2) > 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant1));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant2));
                }
            } else if (operator2.equals(EqualityOperator.EQ) && constant1.compareTo(constant2) <= 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
            } else if (operator2.equals(EqualityOperator.NEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, contractor.next(constant1)));
                } else if (constant1.compareTo(constant2) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant2)
                    );
                } else if (constant1.compareTo(constant2) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant1)
                    );
                }
            }
        } else if (operator1.equals(InequalityOperator.GT)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                if (constant1.compareTo(contractor.previous(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
                } else if (constant1.compareTo(contractor.previous(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.LT)) {
                if (contractor.next(constant1).compareTo(contractor.previous(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, contractor.next(constant1)));
                } else if (contractor.next(constant1).compareTo(contractor.previous(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                if (constant1.compareTo(constant2) >= 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant1));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant2));
                }
            } else if (operator2.equals(InequalityOperator.GT)) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, Collections.max(Arrays.asList(constant1, constant2))));
            } else if (operator2.equals(EqualityOperator.EQ) && constant1.compareTo(constant2) < 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
            } else if (operator2.equals(EqualityOperator.NEQ)) {
                if (constant1.compareTo(contractor.previous(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, contractor.next(constant1)));
                } else if (constant1.compareTo(contractor.previous(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant2)
                    );
                } else if (constant1.compareTo(contractor.previous(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant1)
                    );
                }
            }
        } else if (operator1.equals(EqualityOperator.EQ)) {
            if (operator2.equals(InequalityOperator.LEQ) && constant1.compareTo(constant2) <= 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            } else if (operator2.equals(InequalityOperator.LT) && constant1.compareTo(constant2) < 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            } else if (operator2.equals(InequalityOperator.GEQ) && constant1.compareTo(constant2) >= 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            } else if (operator2.equals(InequalityOperator.GT) && constant1.compareTo(constant2) > 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            } else if (operator2.equals(EqualityOperator.EQ) && constant1.compareTo(constant2) == 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            } else if (operator2.equals(EqualityOperator.NEQ) && constant1.compareTo(constant2) != 0) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
            }
        } else if (operator1.equals(EqualityOperator.NEQ)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, contractor.previous(constant2)));
                } else if (constant1.compareTo(constant2) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant2)
                    );
                }  else if (constant1.compareTo(constant2) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.LT)) {
                if (constant1.compareTo(contractor.previous(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, contractor.previous(constant2)));
                } else if (constant1.compareTo(contractor.previous(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant2)
                    );
                }  else if (constant1.compareTo(contractor.previous(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                if (constant1.compareTo(constant2) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, contractor.next(constant2)));
                } else if (constant1.compareTo(constant2) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant2)
                    );
                }  else if (constant1.compareTo(constant2) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, constant2)
                    );
                }
            } else if (operator2.equals(InequalityOperator.GT)) {
                if (constant1.compareTo(contractor.next(constant2)) == 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, contractor.next(constant2)));
                } else if (constant1.compareTo(contractor.next(constant2)) > 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant2)
                    );
                }  else if (constant1.compareTo(contractor.next(constant2)) < 0) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, constant2)
                    );
                }
            } else if (operator2.equals(EqualityOperator.EQ)) {
                if (constant1.compareTo(constant2) != 0) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant2));
                }
            } else if (operator2.equals(EqualityOperator.NEQ)) {

                Set<T> newConstants = new HashSet<>(Collections.singleton(constant1));
                newConstants.add(constant2);

                if (newConstants.size() > 1) {
                    return Set.of(new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants));
                } else {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1));
                }

            } else {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, constant1),
                        new ConstantOrdinalAtom<>(contractor, attribute, operator2, constant2)
                );
            }
        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyConstantSetOrdinalAtoms(ConstantOrdinalAtom<T> atom1, SetOrdinalAtom<T> atom2) {

        String attribute = atom1.getAttribute();

        ComparableOperator operator1 = atom1.getOperator();
        SetOperator operator2 = atom2.getOperator();

        T constant1 = atom1.getConstant();
        Set<T> constants2 = atom2.getConstants();

        OrdinalContractor<T> contractor = atom1.getContractor();

        if (operator2.equals(SetOperator.IN)) {

            Set<T> newConstants = new HashSet<>(constants2);

            if (operator1.equals(InequalityOperator.LEQ)) {
                newConstants.removeIf(c -> constant1.compareTo(c) < 0);
            } else if (operator1.equals(InequalityOperator.LT)) {
                newConstants.removeIf(c -> constant1.compareTo(c) <= 0);
            } else if (operator1.equals(InequalityOperator.GEQ)) {
                newConstants.removeIf(c -> constant1.compareTo(c) > 0);
            } else if (operator1.equals(InequalityOperator.GT)) {
                newConstants.removeIf(c -> constant1.compareTo(c) >= 0);
            } else if (operator1.equals(EqualityOperator.EQ)) {
                newConstants.removeIf(c -> constant1.compareTo(c) != 0);
            } else if (operator1.equals(EqualityOperator.NEQ)) {
                newConstants.removeIf(c -> constant1.compareTo(c) == 0);
            }

            if (newConstants.size() == 1) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, newConstants.stream().findFirst().get()));
            } else if (newConstants.size() > 1) {
                return Set.of(new SetOrdinalAtom<>(contractor, attribute, SetOperator.IN, newConstants));
            }

        } else if (operator2.equals(SetOperator.NOTIN)) {

            Set<T> newConstants = new HashSet<>(constants2);

            if (operator1.equals(InequalityOperator.LEQ)) {

                newConstants.removeIf(c -> constant1.compareTo(c) < 0);

                T newConstant1 = constant1;

                while (!newConstants.isEmpty() && Collections.max(newConstants).equals(newConstant1)) {
                    newConstants.remove(newConstant1);
                    newConstant1 = contractor.previous(newConstant1);
                }

                if (newConstants.isEmpty()) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, newConstant1));
                } else if (newConstants.size() == 1) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, newConstant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get())
                    );
                } else {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LEQ, newConstant1),
                            new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants)
                    );
                }

            } else if (operator1.equals(InequalityOperator.LT)) {
                newConstants.removeIf(c -> constant1.compareTo(c) <= 0);

                T newConstant1 = constant1;

                while (!newConstants.isEmpty() && Collections.max(newConstants).equals(contractor.previous(newConstant1))) {
                    newConstants.remove(contractor.previous(newConstant1));
                    newConstant1 = contractor.previous(newConstant1);
                }

                if (newConstants.isEmpty()) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, newConstant1));
                } else if (newConstants.size() == 1) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, newConstant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get())
                    );
                } else {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.LT, newConstant1),
                            new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants)
                    );
                }

            } else if (operator1.equals(InequalityOperator.GEQ)) {
                newConstants.removeIf(c -> constant1.compareTo(c) > 0);

                T newConstant1 = constant1;

                while (!newConstants.isEmpty() && Collections.min(newConstants).equals(newConstant1)) {
                    newConstants.remove(newConstant1);
                    newConstant1 = contractor.next(newConstant1);
                }

                if (newConstants.isEmpty()) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, newConstant1));
                } else if (newConstants.size() == 1) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, newConstant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get())
                    );
                } else {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GEQ, newConstant1),
                            new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants)
                    );
                }

            } else if (operator1.equals(InequalityOperator.GT)) {
                newConstants.removeIf(c -> constant1.compareTo(c) >= 0);

                T newConstant1 = constant1;

                while (!newConstants.isEmpty() && Collections.min(newConstants).equals(contractor.next(newConstant1))) {
                    newConstants.remove(contractor.next(newConstant1));
                    newConstant1 = contractor.next(newConstant1);
                }

                if (newConstants.isEmpty()) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, newConstant1));
                } else if (newConstants.size() == 1) {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, newConstant1),
                            new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get())
                    );
                } else {
                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute, InequalityOperator.GT, newConstant1),
                            new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants)
                    );
                }

            } else if (operator1.equals(EqualityOperator.NEQ)) {
                newConstants.add(constant1);
            } else if (operator1.equals(EqualityOperator.EQ)) {
                if (!constants2.contains(constant1)) {
                    return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.EQ, constant1));
                } else {
                    return Set.of(AbstractAtom.ALWAYS_FALSE);
                }
            }

            if (newConstants.size() == 1) {
                return Set.of(new ConstantOrdinalAtom<>(contractor, attribute, EqualityOperator.NEQ, newConstants.stream().findFirst().get()));
            } else if (newConstants.size() > 1) {
                return Set.of(new SetOrdinalAtom<>(contractor, attribute, SetOperator.NOTIN, newConstants));
            }

        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyConstantVariableOrdinalAtoms(ConstantOrdinalAtom<T> atom1, VariableOrdinalAtom<T> atom2) {

        String attribute1 = atom1.getAttribute();
        String attribute2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

        ComparableOperator operator1 = atom1.getOperator();
        ComparableOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

        T constant = atom1.getConstant();

        int rightConstant = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightConstant() : atom2.getRightConstant() * -1;

        OrdinalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(InequalityOperator.LEQ)) {
            if (operator2.equals(InequalityOperator.GEQ) || operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LEQ, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.GT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.subtract(constant, rightConstant))
                );
            }
        } else if (operator1.equals(InequalityOperator.LT)) {
            if (operator2.equals(InequalityOperator.GEQ) || operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.GT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.previous(contractor.subtract(constant, rightConstant)))
                );
            }
        } else if (operator1.equals(InequalityOperator.GEQ)) {
            if (operator2.equals(InequalityOperator.LEQ) || operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GEQ, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.LT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.subtract(constant, rightConstant))
                );
            }
        } else if (operator1.equals(InequalityOperator.GT)) {
            if (operator2.equals(InequalityOperator.LEQ) || operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.LT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.next(contractor.subtract(constant, rightConstant)))
                );
            }
        } else if (operator1.equals(EqualityOperator.EQ)) {
            if (operator2.equals(InequalityOperator.LEQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GEQ, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.LT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.GEQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LEQ, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(InequalityOperator.GT)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, EqualityOperator.EQ, contractor.subtract(constant, rightConstant))
                );
            } else if (operator2.equals(EqualityOperator.NEQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, EqualityOperator.NEQ, contractor.subtract(constant, rightConstant))
                );
            }
        } else if (operator1.equals(EqualityOperator.NEQ)) {
            if (operator2.equals(EqualityOperator.EQ)) {
                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, constant),
                        new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, EqualityOperator.NEQ, contractor.subtract(constant, rightConstant))
                );
            }
        }

        return Set.of(
                new ConstantOrdinalAtom<>(contractor, attribute1, operator1, constant),
                new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant)
        );

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implySetVariableOrdinalAtoms(SetOrdinalAtom<T> atom1, VariableOrdinalAtom<T> atom2) {

        String attribute1 = atom1.getAttribute();
        String attribute2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

        SetOperator operator1 = atom1.getOperator();
        ComparableOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

        Set<T> constants = atom1.getConstants();

        int rightConstant = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightConstant() : atom2.getRightConstant() * -1;

        OrdinalContractor<T> contractor = atom1.getContractor();

        if (operator1.equals(SetOperator.IN)) {

            if (operator2.equals(InequalityOperator.LEQ)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GEQ, contractor.subtract(constant, rightConstant))
                    );

                } else {
                    return Set.of(
                            new SetOrdinalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GEQ, contractor.subtract(Collections.min(constants), rightConstant))
                    );
                }

            } else if (operator2.equals(InequalityOperator.LT)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.subtract(constant, rightConstant))
                    );

                } else {
                    return Set.of(
                            new SetOrdinalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.GT, contractor.subtract(Collections.min(constants), rightConstant))
                    );
                }

            } else if (operator2.equals(InequalityOperator.GEQ)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LEQ, contractor.subtract(constant, rightConstant))
                    );

                } else {
                    return Set.of(
                            new SetOrdinalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LEQ, contractor.subtract(Collections.max(constants), rightConstant))
                    );
                }

            } else if (operator2.equals(InequalityOperator.GT)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.subtract(constant, rightConstant))
                    );

                } else {
                    return Set.of(
                            new SetOrdinalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, InequalityOperator.LT, contractor.subtract(Collections.max(constants), rightConstant))
                    );
                }

            } else if (operator2.equals(EqualityOperator.EQ)) {

                if (constants.size() == 1) {

                    T constant = constants.stream().findFirst().get();

                    return Set.of(
                            new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                            new ConstantOrdinalAtom<>(contractor, attribute2, EqualityOperator.EQ, contractor.subtract(constant, rightConstant))
                    );

                } else {
                    return Set.of(
                            new SetOrdinalAtom<>(contractor, attribute1, SetOperator.IN, constants),
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant),
                            new SetOrdinalAtom<>(contractor, attribute2, SetOperator.IN, constants.stream().map(c -> contractor.subtract(c, rightConstant)).collect(Collectors.toSet()))
                    );
                }

            } else if (operator2.equals(EqualityOperator.NEQ) && constants.size() == 1) {

                T constant = constants.stream().findFirst().get();

                return Set.of(
                        new ConstantOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, constant),
                        new ConstantOrdinalAtom<>(contractor, attribute2, EqualityOperator.NEQ, contractor.subtract(constant, rightConstant))
                );

            }

        } else if (operator1.equals(SetOperator.NOTIN) && operator2.equals(EqualityOperator.EQ)) {

            return Set.of(
                    new SetOrdinalAtom<>(contractor, attribute1, SetOperator.NOTIN, constants),
                    new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant),
                    new SetOrdinalAtom<>(contractor, attribute2, SetOperator.NOTIN, constants.stream().map(c -> contractor.subtract(c, rightConstant)).collect(Collectors.toSet()))
            );

        }

        return Set.of(
                new SetOrdinalAtom<>(contractor, attribute1, operator1, constants),
                new VariableOrdinalAtom<>(contractor, attribute1, operator2, attribute2, rightConstant)
        );

    }

    private static <T extends Comparable<? super T>> Set<AbstractAtom<?, ?, ?>> implyTwoVariableOrdinalAtoms(VariableOrdinalAtom<T> atom1, VariableOrdinalAtom<T> atom2) {

        Set<String> commonAttributes = atom1
                .getAttributes()
                .filter(at1 -> atom2.getAttributes().anyMatch(at1::equals))
                .collect(Collectors.toSet());

        OrdinalContractor<T> contractor = atom1.getContractor();

        if (commonAttributes.size() == 1) {

            String commonAttribute = commonAttributes.stream().findFirst().get();

            String attribute1 = atom1.getLeftAttribute().equals(commonAttribute) ? atom1.getRightAttribute() : atom1.getLeftAttribute();
            String attribute2 = atom2.getLeftAttribute().equals(commonAttribute) ? atom2.getRightAttribute() : atom2.getLeftAttribute();

            ComparableOperator operator1 = atom1.getRightAttribute().equals(commonAttribute) ? atom1.getOperator() : atom1.getOperator().getReversedOperator();
            ComparableOperator operator2 = atom2.getLeftAttribute().equals(commonAttribute) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

            int rightConstant1 = atom1.getRightAttribute().equals(commonAttribute) ? atom1.getRightConstant() : atom1.getRightConstant() * -1;
            int rightConstant2 = atom2.getLeftAttribute().equals(commonAttribute) ? atom2.getRightConstant() : atom2.getRightConstant() * -1;

            if (operator1.equals(InequalityOperator.LEQ)) {
                if (operator2.equals(InequalityOperator.LEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.LT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 + rightConstant2)
                    );
                }
            } else if (operator1.equals(InequalityOperator.LT)) {
                if (operator2.equals(InequalityOperator.LEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else  if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.LT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 + rightConstant2 - 1)
                    );
                }
            } else if (operator1.equals(InequalityOperator.GEQ)) {
                if (operator2.equals(InequalityOperator.GEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.GT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + rightConstant2)
                    );
                }
            } else if (operator1.equals(InequalityOperator.GT)) {
                if (operator2.equals(InequalityOperator.GEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.GT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + rightConstant2 + 1)
                    );
                }
            } else if (operator1.equals(EqualityOperator.EQ)) {
                if (operator2.equals(InequalityOperator.LEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.LT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.LT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(InequalityOperator.GT)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, InequalityOperator.GT, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1 + rightConstant2)
                    );
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.NEQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                }
            } else if (operator1.equals(EqualityOperator.NEQ)) {
                if (operator2.equals(EqualityOperator.EQ)) {
                    return Set.of(
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, commonAttribute, rightConstant1),
                            new VariableOrdinalAtom<>(contractor, commonAttribute, EqualityOperator.EQ, attribute2, rightConstant2),
                            new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1 + rightConstant2)
                    );
                }
            }

            return Set.of(
                    new VariableOrdinalAtom<>(contractor, attribute1, operator1, commonAttribute, rightConstant1),
                    new VariableOrdinalAtom<>(contractor, commonAttribute, operator2, attribute2, rightConstant2)
            );

        } else {

            String attribute1 = atom1.getLeftAttribute();
            String attribute2 = atom1.getRightAttribute();

            ComparableOperator operator1 = atom1.getLeftAttribute().equals(attribute1) ? atom1.getOperator() : atom1.getOperator().getReversedOperator();
            ComparableOperator operator2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getOperator() : atom2.getOperator().getReversedOperator();

            int rightConstant1 = atom1.getLeftAttribute().equals(attribute1) ? atom1.getRightConstant() : atom1.getRightConstant() * -1;
            int rightConstant2 = atom2.getLeftAttribute().equals(attribute1) ? atom2.getRightConstant() : atom2.getRightConstant() * -1;

            if (operator1.equals(InequalityOperator.LEQ)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, Math.min(rightConstant1, rightConstant2)));
                } else if (operator2.equals(InequalityOperator.LT)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1));
                    } else {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.GT)) {
                    if (rightConstant1 > rightConstant2 + 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 >= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1));
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1 - 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant2)
                        );
                    }
                }

            } else if (operator1.equals(InequalityOperator.LT)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2));
                    } else {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.LT)) {
                    return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, Math.min(rightConstant1, rightConstant2)));
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    if (rightConstant1 > rightConstant2 + 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(InequalityOperator.GT)) {
                    if (rightConstant1 - 1 > rightConstant2 + 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 - 1 == rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1 - 1));
                    }
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 < rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1));
                    } else if (rightConstant1 == rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1 - 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant2)
                        );
                    }
                }

            } else if (operator1.equals(InequalityOperator.GEQ)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.LT)) {
                    if (rightConstant1 < rightConstant2 - 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, Math.max(rightConstant1, rightConstant2)));
                } else if (operator2.equals(InequalityOperator.GT)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1));
                    } else {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 <= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1));
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1 + 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant2)
                        );
                    }
                }

            } else if (operator1.equals(InequalityOperator.GT)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    if (rightConstant1 < rightConstant2 - 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 == rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(InequalityOperator.LT)) {
                    if (rightConstant1 + 1 < rightConstant2 - 1) {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2)
                        );
                    } else if (rightConstant1 + 1 == rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1 + 1));
                    }
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2));
                    } else {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.GT)) {
                    return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, Math.max(rightConstant1, rightConstant2)));
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 >= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1));
                    } else if (rightConstant1 == rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1 + 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant2)
                        );
                    }
                }

            } else if (operator1.equals(EqualityOperator.EQ)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    if (rightConstant1 <= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.LT)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    if (rightConstant1 >= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(InequalityOperator.GT)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 != rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant1));
                    }
                }

            } else if (operator1.equals(EqualityOperator.NEQ)) {

                if (operator2.equals(InequalityOperator.LEQ)) {
                    if (rightConstant1 > rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2));
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2 - 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LEQ, attribute2, rightConstant2)
                        );
                    }
                } else if (operator2.equals(InequalityOperator.LT)) {
                    if (rightConstant1 > rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2));
                    } else if (rightConstant1 == rightConstant2 - 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2 - 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.LT, attribute2, rightConstant2)
                        );
                    }
                } else if (operator2.equals(InequalityOperator.GEQ)) {
                    if (rightConstant1 < rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2));
                    } else if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2 + 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GEQ, attribute2, rightConstant2)
                        );
                    }
                } else if (operator2.equals(InequalityOperator.GT)) {
                    if (rightConstant1 <= rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2));
                    } else if (rightConstant1 == rightConstant2 + 1) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2 + 1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, InequalityOperator.GT, attribute2, rightConstant2)
                        );
                    }
                } else if (operator2.equals(EqualityOperator.EQ)) {
                    if (rightConstant1 != rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.EQ, attribute2, rightConstant2));
                    }
                } else if (operator2.equals(EqualityOperator.NEQ)) {
                    if (rightConstant1 == rightConstant2) {
                        return Set.of(new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1));
                    } else {
                        return Set.of(
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant1),
                                new VariableOrdinalAtom<>(contractor, attribute1, EqualityOperator.NEQ, attribute2, rightConstant2)
                        );
                    }
                }

            }

        }

        return Set.of(AbstractAtom.ALWAYS_FALSE);

    }

    public static <T extends Comparable<? super T>, U extends Comparable<? super U>> boolean implies(AbstractAtom<?, ?, ?> atom, AbstractAtom<?, ?, ?> impliedAtom) {

        if (atom.isAlwaysFalse() || impliedAtom.isAlwaysTrue()) {
            return true;
        }

        if (atom.isAlwaysTrue() || impliedAtom.isAlwaysFalse() || atom.getAttributes().anyMatch(at1 -> impliedAtom.getAttributes().noneMatch(at1::equals))) {
            return false;
        }

        if (!atom.getContractor().equals(impliedAtom.getContractor())) {
            throw new AtomException("Contractor of " + atom + " should be equal to contractor of " + impliedAtom);
        }

        // Nominal case
        if (atom instanceof ConstantNominalAtom<?> && impliedAtom instanceof ConstantNominalAtom<?>) {
            return constantImpliesConstantNominal((ConstantNominalAtom<T>) atom, (ConstantNominalAtom<T>) impliedAtom);
        } else if (atom instanceof ConstantNominalAtom<?> && impliedAtom instanceof SetNominalAtom<?>) {
            return constantImpliesSetNominal((ConstantNominalAtom<T>) atom, (SetNominalAtom<T>) impliedAtom);
        } else if (atom instanceof SetNominalAtom<?> && impliedAtom instanceof ConstantNominalAtom<?>) {
            return setImpliesConstantNominal((SetNominalAtom<T>) atom, (ConstantNominalAtom<T>) impliedAtom);
        } else if (atom instanceof SetNominalAtom<?> && impliedAtom instanceof SetNominalAtom<?>) {
            return setImpliesSet((SetNominalAtom<T>) atom, (SetNominalAtom<T>) impliedAtom);
        } else if (atom instanceof VariableNominalAtom<?> && impliedAtom instanceof VariableNominalAtom<?>) {
            return variableImpliesVariableNominal((VariableNominalAtom<T>) atom, (VariableNominalAtom<T>) impliedAtom);
        }

        // Ordinal case
        else if (atom instanceof ConstantOrdinalAtom<?> && impliedAtom instanceof ConstantOrdinalAtom<?>) {
            return constantImpliesConstantOrdinal((ConstantOrdinalAtom<T>) atom, (ConstantOrdinalAtom<T>) impliedAtom);
        } else if (atom instanceof ConstantOrdinalAtom<?> && impliedAtom instanceof SetOrdinalAtom<?>) {
            return constantImpliesSetOrdinal((ConstantOrdinalAtom<T>) atom, (SetOrdinalAtom<T>) impliedAtom);
        } else if (atom instanceof SetOrdinalAtom<?> && impliedAtom instanceof ConstantOrdinalAtom<?>) {
            return setImpliesConstantOrdinal((SetOrdinalAtom<T>) atom, (ConstantOrdinalAtom<T>) impliedAtom);
        } else if (atom instanceof SetOrdinalAtom<?> && impliedAtom instanceof SetOrdinalAtom<?>) {
            return setImpliesSet((SetOrdinalAtom<T>) atom, (SetOrdinalAtom<T>) impliedAtom);
        } else if (atom instanceof VariableOrdinalAtom<?> && impliedAtom instanceof VariableOrdinalAtom<?>) {
            return variableImpliesVariableOrdinal((VariableOrdinalAtom<T>) atom, (VariableOrdinalAtom<T>) impliedAtom);
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean constantImpliesConstantNominal(ConstantNominalAtom<T> atom, ConstantNominalAtom<T> impliedAtom) {

        EqualityOperator operator = atom.getOperator();
        EqualityOperator impliedOperator = impliedAtom.getOperator();

        T constant = atom.getConstant();
        T impliedConstant = impliedAtom.getConstant();

        if (operator.equals(EqualityOperator.EQ)) {
            if (impliedOperator.equals(EqualityOperator.EQ)) {
                return constant.equals(impliedConstant);
            } else if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return !constant.equals(impliedConstant);
            }
        } else if (operator.equals(EqualityOperator.NEQ)) {
            if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.equals(impliedConstant);
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean constantImpliesSetNominal(ConstantNominalAtom<T> atom, SetNominalAtom<T> impliedAtom) {

        EqualityOperator operator = atom.getOperator();
        SetOperator impliedOperator = impliedAtom.getOperator();

        T constant = atom.getConstant();
        Set<T> impliedConstants = impliedAtom.getConstants();

        if (operator.equals(EqualityOperator.EQ)) {
            if (impliedOperator.equals(SetOperator.IN)) {
                return impliedConstants.contains(constant);
            } else if (impliedOperator.equals(SetOperator.NOTIN)) {
                return !impliedConstants.contains(constant);
            }
        } else if (operator.equals(EqualityOperator.NEQ)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.size() == 1 && impliedConstants.contains(constant);
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean setImpliesConstantNominal(SetNominalAtom<T> atom, ConstantNominalAtom<T> impliedAtom) {

        SetOperator operator = atom.getOperator();
        ComparableOperator impliedOperator = impliedAtom.getOperator();

        Set<T> constants = atom.getConstants();
        T impliedConstant = impliedAtom.getConstant();

        if (operator.equals(SetOperator.IN)) {
            if (impliedOperator.equals(EqualityOperator.EQ)) {
                return constants.size() == 1 && constants.contains(impliedConstant);
            } else if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return !constants.contains(impliedConstant);
            }
        } else if (operator.equals(SetOperator.NOTIN)) {
            if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return constants.contains(impliedConstant);
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean setImpliesSet(SetAtom<T, ?> atom, SetAtom<T, ?> impliedAtom) {

        SetOperator operator = atom.getOperator();
        SetOperator impliedOperator = impliedAtom.getOperator();

        Set<T> constants = atom.getConstants();
        Set<T> impliedConstants = impliedAtom.getConstants();

        if (operator.equals(SetOperator.IN)) {

            if (impliedOperator.equals(SetOperator.IN)) {
                return impliedConstants.containsAll(constants);
            } else if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.stream().noneMatch(constants::contains);
            }

        } else if (operator.equals(SetOperator.NOTIN)) {

            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return constants.containsAll(impliedConstants);
            }

        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean variableImpliesVariableNominal(VariableNominalAtom<T> atom, VariableNominalAtom<T> impliedAtom) {

        String attribute1 = atom.getLeftAttribute();

        EqualityOperator operator = atom.getOperator();
        EqualityOperator impliedOperator = impliedAtom.getLeftAttribute().equals(attribute1) ? impliedAtom.getOperator() : (EqualityOperator) impliedAtom.getOperator().getReversedOperator();

        return operator.equals(impliedOperator);

    }

    private static <T extends Comparable<? super T>> boolean constantImpliesConstantOrdinal(ConstantOrdinalAtom<T> atom, ConstantOrdinalAtom<T> impliedAtom) {

        ComparableOperator operator = atom.getOperator();
        ComparableOperator impliedOperator = impliedAtom.getOperator();

        T constant = atom.getConstant();
        T impliedConstant = impliedAtom.getConstant();

        OrdinalContractor<T> contractor = atom.getContractor();

        if (operator.equals(InequalityOperator.LEQ)) {
            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return constant.compareTo(impliedConstant) <= 0;
            } else if (impliedOperator.equals(InequalityOperator.LT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) < 0;
            }
        } else if (operator.equals(InequalityOperator.LT)) {
            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return contractor.previous(constant).compareTo(impliedConstant) <= 0;
            } else if (impliedOperator.equals(InequalityOperator.LT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) <= 0;
            }
        } else if (operator.equals(InequalityOperator.GEQ)) {
            if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return constant.compareTo(impliedConstant) >= 0;
            } else if (impliedOperator.equals(InequalityOperator.GT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) > 0;
            }
        } else if (operator.equals(InequalityOperator.GT)) {
            if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return contractor.next(constant).compareTo(impliedConstant) >= 0;
            } else if (impliedOperator.equals(InequalityOperator.GT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) >= 0;
            }
        } else if (operator.equals(EqualityOperator.EQ)) {
            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return constant.compareTo(impliedConstant) <= 0;
            } else if (impliedOperator.equals(InequalityOperator.LT)) {
                return constant.compareTo(impliedConstant) < 0;
            } else if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return constant.compareTo(impliedConstant) >= 0;
            } else if (impliedOperator.equals(InequalityOperator.GT)) {
                return constant.compareTo(impliedConstant) > 0;
            } else if (impliedOperator.equals(EqualityOperator.EQ)) {
                return constant.compareTo(impliedConstant) == 0;
            } else if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) != 0;
            }
        } else if (operator.equals(EqualityOperator.NEQ)) {
            if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return constant.compareTo(impliedConstant) == 0;
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean constantImpliesSetOrdinal(ConstantOrdinalAtom<T> atom, SetOrdinalAtom<T> impliedAtom) {

        ComparableOperator operator = atom.getOperator();
        SetOperator impliedOperator = impliedAtom.getOperator();

        T constant = atom.getConstant();
        Set<T> impliedConstants = impliedAtom.getConstants();

        if (operator.equals(InequalityOperator.LEQ)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.stream().allMatch(c -> constant.compareTo(c) < 0);
            }
        } else if (operator.equals(InequalityOperator.LT)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.stream().allMatch(c -> constant.compareTo(c) <= 0);
            }
        } else if (operator.equals(InequalityOperator.GEQ)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.stream().allMatch(c -> constant.compareTo(c) > 0);
            }
        } else if (operator.equals(InequalityOperator.GT)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.stream().allMatch(c -> constant.compareTo(c) >= 0);
            }
        } else if (operator.equals(EqualityOperator.EQ)) {
            if (impliedOperator.equals(SetOperator.IN)) {
                return impliedConstants.contains(constant);
            } else if (impliedOperator.equals(SetOperator.NOTIN)) {
                return !impliedConstants.contains(constant);
            }
        } else if (operator.equals(EqualityOperator.NEQ)) {
            if (impliedOperator.equals(SetOperator.NOTIN)) {
                return impliedConstants.size() == 1 && impliedConstants.contains(constant);
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean setImpliesConstantOrdinal(SetOrdinalAtom<T> atom, ConstantOrdinalAtom<T> impliedAtom) {

        SetOperator operator = atom.getOperator();
        ComparableOperator impliedOperator = impliedAtom.getOperator();

        Set<T> constants = atom.getConstants();
        T impliedConstant = impliedAtom.getConstant();

        if (operator.equals(SetOperator.IN)) {
            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return impliedConstant.compareTo(Collections.max(constants)) >= 0;
            } else if (impliedOperator.equals(InequalityOperator.LT)) {
                return impliedConstant.compareTo(Collections.max(constants)) > 0;
            } else if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return impliedConstant.compareTo(Collections.min(constants)) <= 0;
            } else if (impliedOperator.equals(InequalityOperator.GT)) {
                return impliedConstant.compareTo(Collections.min(constants)) < 0;
            } else if (impliedOperator.equals(EqualityOperator.EQ)) {
                return constants.size() == 1 && constants.contains(impliedConstant);
            } else if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return !constants.contains(impliedConstant);
            }
        } else if (operator.equals(SetOperator.NOTIN)) {
            if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return constants.contains(impliedConstant);
            }
        }

        return false;

    }

    private static <T extends Comparable<? super T>> boolean variableImpliesVariableOrdinal(VariableOrdinalAtom<T> atom, VariableOrdinalAtom<T> impliedAtom) {

        String attribute1 = atom.getLeftAttribute();

        ComparableOperator operator = atom.getOperator();
        ComparableOperator impliedOperator = impliedAtom.getLeftAttribute().equals(attribute1) ? impliedAtom.getOperator() : impliedAtom.getOperator().getReversedOperator();

        int rightConstant = atom.getRightConstant();
        int impliedRightConstant = impliedAtom.getLeftAttribute().equals(attribute1) ? impliedAtom.getRightConstant() : impliedAtom.getRightConstant() * -1;

        if (operator.equals(InequalityOperator.LEQ)) {

            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return impliedRightConstant >= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.LT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant > rightConstant;
            }

        } else if (operator.equals(InequalityOperator.LT)) {

            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return impliedRightConstant + 1 >= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.LT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant >= rightConstant;
            }

        } else if (operator.equals(InequalityOperator.GEQ)) {

            if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return impliedRightConstant <= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.GT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant < rightConstant;
            }

        } else if (operator.equals(InequalityOperator.GT)) {

            if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return impliedRightConstant - 1 <= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.GT) || impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant <= rightConstant;
            }

        } else if (operator.equals(EqualityOperator.EQ)) {

            if (impliedOperator.equals(InequalityOperator.LEQ)) {
                return impliedRightConstant >= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.LT)) {
                return impliedRightConstant > rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.GEQ)) {
                return impliedRightConstant <= rightConstant;
            } else if (impliedOperator.equals(InequalityOperator.GT)) {
                return impliedRightConstant < rightConstant;
            } else if (impliedOperator.equals(EqualityOperator.EQ)) {
                return impliedRightConstant == rightConstant;
            } else if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant != rightConstant;
            }

        } else if (operator.equals(EqualityOperator.NEQ)) {

           if (impliedOperator.equals(EqualityOperator.NEQ)) {
                return impliedRightConstant == rightConstant;
            }

        }

        return false;

    }

}
