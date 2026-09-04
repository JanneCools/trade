package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;

import java.util.*;
import java.util.stream.Collectors;

public class VariableImplicator<T extends Comparable<? super T>> extends SigmaRuleImplicator<T>
{ 
    @Override
    public boolean isApplicable(String generator, SigmaContractor<?> contract, Set<SigmaRule> contributors) {
        return true;
    }

    @Override
    public Set<SigmaRule> generate(String generator, SigmaContractor<T> generatorContractor, Set<SigmaRule> contributors)
    {
        Set<CPF> cpfs = new HashSet<>(Collections.singleton(new CPF(AbstractAtom.ALWAYS_TRUE)));

        for (SigmaRule contributor: contributors) {
            cpfs = processAtomSet(contributor.getAtoms(), cpfs);
            cpfs = removeRedundancy(cpfs);
        }

        // eliminate the generator
        Set<CPF> noGenCPFs = cpfs
                .stream()
                .map(cpf -> new CPF(cpf
                        .getAtoms()
                        .stream()
                        .filter(atom -> !atom.involves(generator))
                        .collect(Collectors.toSet()))
                )
                .collect(Collectors.toSet());

        if (noGenCPFs.stream().anyMatch(cpf -> cpf.getAtoms().isEmpty()))
            return new HashSet<>();

        noGenCPFs = removeRedundancy(noGenCPFs);

        Set<CPF> result = new HashSet<>(Collections.singleton(new CPF(AbstractAtom.ALWAYS_TRUE)));

        for (CPF noGenCPF: noGenCPFs) {
            result = processAtomSet(noGenCPF.getAtoms(), result);
            result = removeRedundancy(result);
        }

        return result.stream().map(SigmaRule::new).collect(Collectors.toSet());
        
    }

    private Set<CPF> processAtomSet(Set<AbstractAtom<?, ?, ?>> atoms, Set<CPF> CPFs)
    {
        Set<CPF> extendedCPFs = new HashSet<>();

        for (AbstractAtom<?, ?, ?> atom : atoms) {

            AbstractAtom<?, ?, ?> inverseAtom = atom.getInverse();

            for (CPF cpf : CPFs) {

                Set<AbstractAtom<?, ?, ?>> cpfAtoms = new HashSet<>(cpf.getAtoms());

                CPF extendedCPF = CPFImplicator.implyWithAtoms(new CPF(cpfAtoms), Set.of(inverseAtom));
                extendedCPFs.add(extendedCPF);

            }

        }

        return extendedCPFs;

    }
    
    private Set<CPF> removeRedundancy(Set<CPF> CPFs) {
        return CPFs
            .stream()
            .filter(cpf1 -> CPFs.stream().noneMatch(cpf2 -> !cpf1.equals(cpf2) && cpf1.implies(cpf2)))
            .collect(Collectors.toSet());

    }

}
