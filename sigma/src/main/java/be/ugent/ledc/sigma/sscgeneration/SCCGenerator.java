package be.ugent.ledc.sigma.sscgeneration;

import be.ugent.ledc.core.datastructures.rules.Rule;
import be.ugent.ledc.core.datastructures.rules.RuleSet;

import java.util.Set;

/**
 * An SCC generator is a generic interface to transform a set of 
 * rules (typically tuple-level constraints) into a set that is Set-Cover Correctible (SCC).
 * This is a set of rules for which each cover of failing rules is also a solution
 * for the tuple that produced the failing rules.
 *
 * In other words, for given rules R, an SCC-set S for R carries the property that
 * for any tuple t, we can:
 * - take the rules in S that are failed by t 
 * - find a cover C of those failing rules
 * - construct a tuple t' that satisfies R and differs from t only in attributes C. 
 *  * 
 * @author abronsel
 * @param <R>
 * @param <S> 
 */
public interface SCCGenerator<R extends Rule<?>, S extends RuleSet<?,R>>
{
    Set<R> generateSCCSet(S ruleset);
}
