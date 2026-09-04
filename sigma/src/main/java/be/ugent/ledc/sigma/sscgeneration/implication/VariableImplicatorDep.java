package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Deprecated
public class VariableImplicatorDep<T extends Comparable<? super T>> extends SigmaRuleImplicator<T>
{
    @Override
    public boolean isApplicable(String generator, SigmaContractor<?> contract, Set<SigmaRule> contributors)
    {
        return true;
    }

    @Override
    public Set<SigmaRule> generate(String generator, SigmaContractor<T> generatorContractor, Set<SigmaRule> contributors)
    {
        Set<CPF> result = new HashSet<>(Collections.singleton(new CPF(AbstractAtom.ALWAYS_TRUE)));

        for (SigmaRule contributor: contributors)
        {
            result = processSigmaRule(contributor, result);
            result = removeRedundancy(result);
        }

        result.forEach(System.out::println);

        //With the resulting stack, we create all new rules
        return buildNewRules(result, generator);

    }

    private Set<CPF> processSigmaRule(SigmaRule sigmaRule, Set<CPF> CPFs)
    {
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

    private Set<CPF> removeRedundancy(Set<CPF> CPFs)
    {

        return CPFs
                .stream()
                .filter(cpf1 -> CPFs.stream().noneMatch(cpf2 -> !cpf1.equals(cpf2) && cpf1.implies(cpf2)))
                .collect(Collectors.toSet());

    }

    private Set<SigmaRule> buildNewRules(Set<CPF> cpfs, String g)
    {
        //Eliminate the generator
        List<CPF> eliminatedCPFs = cpfs
                .stream()
                .map(cpf -> new CPF(cpf
                        .getAtoms()
                        .stream()
                        .filter(atom -> !atom.involves(g))
                        .collect(Collectors.toSet()))
                )
                .toList();

        int cpfIndex = 1;


        Map<Integer, AbstractAtom<?,?,?>> atomIndex = new HashMap<>();
        Map<Integer, Set<Integer>> ruleIndex = new HashMap<>();

        for(CPF cpf: eliminatedCPFs)
        {
            for(AbstractAtom<?,?,?> aa: cpf.getAtoms())
            {
                Set<String> attrs = aa
                        .getAttributes()
                        .collect(Collectors.toSet());

                Integer key = atomIndex
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().getAttributes().allMatch(attrs::contains))
                        .filter(e -> e.getValue().isEquivalentTo(aa))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if(key == null)
                {
                    int idx = atomIndex.size() + 1;
                    atomIndex.put(idx, aa);
                    ruleIndex.put(idx, SetOperations.set(cpfIndex));
                }
                else
                {
                    ruleIndex.get(key).add(cpfIndex);
                }
            }

            cpfIndex++;
        }

        Set<Set<Integer>> covers = new HashSet<>();

        searchCovers(
                covers,
                ruleIndex
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        e -> SetOperations.set(e.getKey()),
                                        Map.Entry::getValue
                                )
                        ),
                1,
                eliminatedCPFs.size());

        Set<SigmaRule> rules = covers
                .stream()
                .map(cover -> new SigmaRule(
                        CPFImplicator.imply(new CPF(cover
                                .stream()
                                .map(idx -> atomIndex.get(idx).getInverse())
                                .collect(Collectors.toSet()
                                )))
                ))
                .collect(Collectors.toSet());

        //Remove redundant rules
        rules.removeIf(rule -> rules
                .stream()
                .anyMatch(dRule -> !dRule.equals(rule) && rule.isRedundantTo(dRule)));

        Set<SigmaRule> finalRules = new HashSet<>();

        //Reduce atoms
        for(SigmaRule rule: rules)
        {
            boolean stop = false;

            Set<AbstractAtom<?, ?, ?>> atoms = rule.getAtoms();

            while(!stop)
            {
                AbstractAtom<?,?,?> redundant = null;

                for(AbstractAtom<?, ?, ?> atom: atoms)
                {
                    Set<AbstractAtom<?,?,?>> otherAtoms = new HashSet<>(atoms);
                    otherAtoms.remove(atom);

                    if(CPFImplicator.atomIsImpliedByCPF(new CPF(otherAtoms), atom))
                    {
                        redundant = atom;
                        break;
                    }
                }

                if(redundant == null)
                {
                    stop = true;
                }
                else
                {
                    atoms.remove(redundant);
                }
            }

            finalRules.add(new SigmaRule(atoms));

        }

        return finalRules;
    }

    private void searchCovers(Set<Set<Integer>> covers,  Map<Set<Integer>, Set<Integer>> ruleIndex, int current, int cpfSize)
    {
        if(ruleIndex.isEmpty())
            return;

        //Check for output
        for(Set<Integer> cover: ruleIndex.keySet())
        {
            //If an atom combination covers all CPFs, add it to the cover
            if(ruleIndex.get(cover).size() == cpfSize)
            {
                covers.add(cover);
            }
        }

        //Remove full covers
        ruleIndex.entrySet().removeIf(e -> e.getValue().size() == cpfSize);

        //Construct new rule index
        Map<Set<Integer>, Set<Integer>> newRuleIndex = new HashMap<>();

        for(Set<Integer> leftCover: ruleIndex.keySet())
        {
            for(Set<Integer> rightCover: ruleIndex.keySet())
            {
                if(ruleIndex.get(leftCover).containsAll(ruleIndex.get(rightCover)))
                    continue;

                if(ruleIndex.get(rightCover).containsAll(ruleIndex.get(leftCover)))
                    continue;

                Set<Integer> union = SetOperations.union(leftCover, rightCover);

                if(union.size() != current + 1)
                    continue;

                newRuleIndex.put(
                        union,
                        SetOperations.union(
                                ruleIndex.get(leftCover),
                                ruleIndex.get(rightCover))
                );

            }
        }

        //Recursive call
        searchCovers(
                covers,
                newRuleIndex,
                current+1,
                cpfSize);
    }
}