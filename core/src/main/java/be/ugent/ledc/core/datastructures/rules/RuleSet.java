package be.ugent.ledc.core.datastructures.rules;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * 
 * This class represents a generic RuleSet, which consists of a set of rules
 * and has a number of supporting methods to test consistency of a piece of data D.
 * @param <D>
 * @param <R>
 */
public class RuleSet<D, R extends Rule<D>> implements Iterable<R>
{
    private final Set<R> rules;

    public RuleSet(Set<R> rules)
    {
        this.rules = rules;
    }
    
    public RuleSet(R... rules)
    {
        this(Stream
            .of(rules)
            .collect(Collectors.toSet())
        );
    }

    public Set<R> getRules() {
        return rules;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.rules);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RuleSet<?, ?> other = (RuleSet<?, ?>) obj;
        return Objects.equals(this.rules, other.rules);
    }


    @Override
    public Iterator<R> iterator()
    {
        return rules.iterator();
    }
    
    /**
     * Tests if data d satisfies all rules.
     * @param d
     * @return true if all rules are satisfied by d.
     */
    public boolean isSatisfied(D d)
    {
        return rules.stream().allMatch(rule -> rule.isSatisfied(d));
    }
    
    /**
     * Tests if data fails at least one rule.
     * @param d
     * @return true if at least one rule is not satisfied.
     */
    public boolean isFailed(D d)
    {
        return !isSatisfied(d);
    }

    /**
     * Finds all rules that are failed by data d
     *
     * @param d The piece of data that is tested.
     * @return All those rules in the given ruleset that are failed by the data object o.
     */
    public Set<R> failingRules(D d)
    {
        return rules
            .stream()
            .filter(rule -> rule.isFailed(d))
            .collect(Collectors.toSet());
    }
    
    public Stream<R> stream()
    {
        return rules.stream();
    }
    
    public int size()
    {
        return rules.size();
    }
    
    public boolean isEmpty()
    {
        return rules.isEmpty();
    }
    
    /**
     * Returns a projection of the Ruleset over the given attributes.
     * A rule is selected if ALL it's involved attributes are in the required projection
     *
     * @param attributes
     * @return
     */
    public RuleSet<D, R> project(Set<String> attributes)
    {
        return new RuleSet<>(rules
            .stream()
            .filter(rule -> attributes.containsAll(rule.getInvolvedAttributes()))
            .collect(Collectors.toSet())
        );
    }

    public RuleSet<D, R> project(String... attributes)
    {
        return project(Stream.of(attributes).collect(Collectors.toSet()));
    }
    
    @Override
    public String toString()
    {
        return rules
            .stream()
            .map(rule -> rule.toString())
            .collect(Collectors.joining("\n")
        );
    }
}
