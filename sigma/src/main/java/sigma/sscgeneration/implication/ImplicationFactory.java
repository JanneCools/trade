package sigma.sscgeneration.implication;

import core.datastructures.rules.Rule;
import sigma.datastructures.contracts.SigmaContractor;
import sigma.datastructures.rules.SigmaRule;
import java.util.Set;

/**
 * An interface that is used in the FCF generator to provide a rule implicator
 * that can be used to do rule implication in a particular node of the FCF
 * structure.
 * @author abronsel
 */
public interface ImplicationFactory<R extends Rule<?>>
{
    <T extends Comparable<? super T>> RuleImplicator<T, R> create(String generator, SigmaContractor<T> rules, Set<SigmaRule> contributors);
}
