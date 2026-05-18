package be.ugent.ledc.sigma.sscgeneration.implication;


import be.ugent.ledc.core.datastructures.rules.Rule;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.Set;
import java.util.stream.Stream;

public interface RuleImplicator<T extends Comparable<? super T>, R extends Rule<?>>
{
    public boolean isApplicable(String generator, SigmaContractor<?> generatorContractor, Set<SigmaRule> candidateContributors);

    public Set<SigmaRule> generate(String generator, SigmaContractor<T> generatorContractor, Set<SigmaRule> candidateContributors);

    default CPF join(CPF cpf1, CPF cpf2)
    {
        CPF joined = CPFImplicator.imply(
            new CPF(SetOperations.union(
                cpf1.getAtoms(),
                cpf2.getAtoms())
            )
        );
        
        if(joined.equals(new CPF(AbstractAtom.ALWAYS_FALSE)))
            return joined;
        
        joined
            .getAtoms()
            .removeIf(atom ->
                atom.getAttributes().count() > 0
                && Stream.concat(
                    cpf1.getAtoms().stream(),
                    cpf2.getAtoms().stream())
                .noneMatch(oAtom -> oAtom.getAttributes().sorted().toList().equals(atom.getAttributes().sorted().toList()))
            );
        
        return joined;
                
    }
}
