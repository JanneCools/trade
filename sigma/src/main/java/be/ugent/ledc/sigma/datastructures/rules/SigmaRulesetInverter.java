package be.ugent.ledc.sigma.datastructures.rules;

import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.VariableVarioAtom;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;

import java.util.*;
import java.util.stream.Collectors;

public class SigmaRulesetInverter {

    public static Set<CPF> invert(SigmaRuleset ruleset) {

        Set<CPF> result = new HashSet<>(Collections.singleton(new CPF(AbstractAtom.ALWAYS_TRUE)));

        Set<SigmaRule> sigmaRules = ruleset.getRules();

        for (SigmaRule sigmaRule : sigmaRules) {
            result = processSigmaRule(sigmaRule, result);
            result = removeRedundancy(result);
        }

        return result;

    }

    public static Set<Pair<CPF, CPF>> processSigmaRuleWithVario(Set<AbstractAtom<?,?,?>> atoms, Set<Pair<CPF, CPF>> CPFs) {
        Set<Pair<CPF, CPF>> result = new HashSet<>();

        for (AbstractAtom<?,?,?> atom : atoms) {
            AbstractAtom<?,?,?> inverseAtom = atom.getInverse();

            for (Pair<CPF, CPF> pair: CPFs) {
                CPF cpf = pair.getFirst();
                CPF varioCPF = pair.getSecond();

                if (atom instanceof VariableVarioAtom<?,?>) {
                    result.add(new Pair<>(
                            cpf,
                            new CPF(SetOperations.union(varioCPF.getAtoms(), Set.of(inverseAtom)),
                                    SetOperations.union(varioCPF.getAttributes(), inverseAtom.getAttributesSet()))));
                } else {
                    result.add(new Pair<>(
                            CPFImplicator.imply(new CPF(SetOperations.union(cpf.getAtoms(), Set.of(inverseAtom)))),
                            varioCPF
                    ));
                }
            }
        }

        return result;
    }

    // TODO: method to add rule to existing inverted set
    // TODO: partition non-overlapping rules

    public static Set<CPF> processSigmaRule(SigmaRule sigmaRule, Set<CPF> CPFs) {

        Set<CPF> extendedCPFs = new HashSet<>();

        Set<AbstractAtom<?, ?, ?>> atoms = sigmaRule.getAtoms();

        for (AbstractAtom<?, ?, ?> atom : atoms) {

            AbstractAtom<?, ?, ?> inverseAtom = atom.getInverse();

            for (CPF cpf : CPFs) {

                Set<AbstractAtom<?, ?, ?>> cpfAtoms = new HashSet<>(cpf.getAtoms());
                cpfAtoms.add(inverseAtom);

                CPF extendedCPF = CPFImplicator.imply(new CPF(cpfAtoms));
                extendedCPFs.add(extendedCPF);
            }

        }

        return extendedCPFs;

    }

    public static Set<CPF> removeRedundancy(Set<CPF> CPFs) {
        List<CPF> list = new ArrayList<>(CPFs);

        // prepare a two-dimensional list to store results of the implies function
        int[][] implications = new int[CPFs.size()][CPFs.size()];

        Set<CPF> result = new HashSet<>();
        for (int i = 0; i < CPFs.size(); i++) {
            CPF cpf1 = list.get(i);
            boolean noneMatch = true;
            int j = 0;
            while (j < CPFs.size() && noneMatch) {
                if (i != j) {
                    CPF cpf2 = list.get(j);

                    // compute implications if not yet done
                    if (implications[i][j] == 0)
                        implications[i][j] = cpf1.implies(cpf2) ? 1 : 2;
                    if (implications[j][i] == 0)
                        implications[j][i] = cpf2.implies(cpf1) ? 1 : 2;

                    // cpf1 is equivalent to cpf2 and it has a lower hashcode, or cpf1 does not imply cpf2
                    noneMatch = (implications[i][j] == 1 && implications[j][i] == 1 && cpf1.hashCode() < cpf2.hashCode())
                            || implications[i][j] == 2;
                }
                j++;
            }
            if (noneMatch)
                result.add(cpf1);
        }

        return result;
    }

    public static Set<Pair<CPF,CPF>> removeRedundancyWithVario(Set<Pair<CPF,CPF>> CPFs) {
        List<Pair<CPF,CPF>> list = new ArrayList<>(CPFs);

        // prepare a two-dimensional list to store results of the implies-function
        int[][] implications = new int[CPFs.size()][CPFs.size()];

        Set<Pair<CPF,CPF>> result = new HashSet<>(CPFs.size());
        for (int i = 0; i < CPFs.size(); i++) {
            Pair<CPF,CPF> pair1 = list.get(i);
            boolean noneMatch = true;
            int j = 0;
            while (j < CPFs.size() && noneMatch) {
                if (i != j) {
                    Pair<CPF,CPF> pair2 = list.get(j);

                    // compute implications if not yet done
                    boolean equalVario = pair1.getSecond().equals(pair2.getSecond());
                    if (implications[i][j] == 0)
                        implications[i][j] = equalVario && pair1.getFirst().implies(pair2.getFirst()) ? 1 : 2;
                    if (implications[j][i] == 0)
                        implications[j][i] = equalVario && pair2.getFirst().implies(pair1.getFirst()) ? 1 : 2;

                    // no match if pair1 is equivalent to pair2 and has a lower hashcode, or if pair1 does not imply pair2
                    noneMatch = (implications[i][j] == 1 && implications[j][i] == 1 & pair1.getFirst().hashCode() < pair2.getFirst().hashCode())
                            || implications[i][j] == 2;
                }
                j++;
            }
            if (noneMatch)
                result.add(pair1);
        }

        return result;
    }

}
