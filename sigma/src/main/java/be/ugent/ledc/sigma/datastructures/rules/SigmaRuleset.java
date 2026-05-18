package be.ugent.ledc.sigma.datastructures.rules;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.rules.RuleSet;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.RepairOperations;
import java.util.HashMap;

/**
 * @author Toon Boeckling
 * <p>
 * This class represents a sigma ruleset, which consists of a set of rules and a mapping of attributes on their contractors
 */
public class SigmaRuleset extends RuleSet<DataObject, SigmaRule> {

    public static final long DOMAIN_BOUND = 1000000L;
    private final Map<String, SigmaContractor<?>> contractors;

    /**
     * Create a new sigma rule set
     * @param contractors mapping of the attributes on their contractors of this ruleset
     * @param sigmaRules set of sigma rules in this ruleset
     * @throws SigmaRulesetException
     */
    public SigmaRuleset(Map<String, SigmaContractor<?>> contractors, Set<SigmaRule> sigmaRules) throws SigmaRulesetException {
        super(sigmaRules);
        this.contractors = contractors;
    }

    /**
     * Get the mapping of the attributes on their contractors of this ruleset
     * @return mapping of the attributes on their contractors of this ruleset
     */
    public Map<String, SigmaContractor<?>> getContractors()
    {
        return contractors;
    }

    /**
     * Returns a projection of the Ruleset over the given attributes. A SigmaRule
     * is selected if ALL it's involved attributes are in the required projection
     *
     * @param attributes
     * @return
     */
    @Override
    public SigmaRuleset project(Set<String> attributes) {
        Map<String, SigmaContractor<?>> pContractors = contractors.entrySet()
                .stream()
                .filter(e -> attributes.contains(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));

        Set<SigmaRule> pRules = getRules()
                .stream()
                .filter(rule -> attributes.containsAll(rule.getInvolvedAttributes()))
                .collect(Collectors.toSet());

        return new SigmaRuleset(pContractors, pRules);
    }

    /**
     * Verifies if this set of rules poses a constraint on the values the given
     * attribute can take. Such a domain constraint is a rule involving only that attribute.
     *
     * @param attribute The attribute for which we want to verify if domain constraints are present
     * @return Returns true if there is at least one rule involving the attribute and only that attribute.
     */
    public boolean hasDomainConstraints(String attribute)
    {
        return stream()
            .anyMatch(rule ->
                    rule.getInvolvedAttributes().size() == 1
                &&  rule.involves(attribute));
    }

    /**
     * Verifies if this set has domain constraints and additional these constraints
     * bound the domain to a finite set (in practice, we use an upper bound of 10^6 values.
     *
     * @param attribute The attribute for which we want to verify if domain constraints are present
     * @return Returns true if there is at least one rule involving the attribute and only that attribute.
     */
    public boolean hasFiniteDomain(String attribute)
    {
        return stream()
                .anyMatch(rule ->
                        rule.getInvolvedAttributes().size() == 1
                                &&  rule.involves(attribute))
                &&  getPermittedValues(attribute).cardinality() < DOMAIN_BOUND;
    }


    /**
     * Get the rules posing a constraint on the values the given attribute can take.
     * @param attribute The attribute for which we want to find its domain constraints
     * @return Set of sigma rules posing a constraint on the values the given attribute can take
     */
    public Set<SigmaRule> getDomainConstraints(String attribute) {
        return stream()
            .filter(r ->
                    r.getInvolvedAttributes().size() == 1
                &&  r.involves(attribute))
            .collect(Collectors.toSet());
    }

    /**
     * Verifies if this set of rules poses a constraint on the values the given
     * nominal attribute can take. Such a domain constraint is a rule involving only that attribute with operator != or NOTIN.
     *
     * @param nominalAttribute The nominal attribute for which we want to verify if domain constraints are present
     * @return Returns true if there is at least one rule involving the attribute and only that attribute with operator != or NOTIN.
     */
    public boolean hasNominalDomainConstraints(String nominalAttribute) {
        return contractors.get(nominalAttribute) instanceof NominalContractor
            && hasDomainConstraints(nominalAttribute)
            && getDomainConstraints(nominalAttribute)
                .stream()
                .anyMatch(r -> r
                    .getAtoms()
                    .stream()
                    .allMatch(a ->
                            a.getOperator().equals(EqualityOperator.NEQ)
                        ||  a.getOperator().equals(SetOperator.NOTIN)));
    }

    public Set<SigmaRule> getNominalDomainConstraints(String nominalAttribute) {

        if (!hasNominalDomainConstraints(nominalAttribute)) {
            return new HashSet<>();
        }

        return getDomainConstraints(nominalAttribute).stream().filter(r -> r.getAtoms().stream().allMatch(a -> a.getOperator().equals(EqualityOperator.NEQ) || a.getOperator().equals(SetOperator.NOTIN))).collect(Collectors.toSet());
    }

    /**
     * Verifies if this set of rules poses constraint on the values the given
     * attribute can take, conditioned upon the values other attributes take.
     * Such a conditional constraint is a rule involving the attribute, but also at least one other attribute.
     *
     * @param attribute The attribute for which we want to verify if conditional constraints are present
     * @return Returns true if there is at least one rule that involves the attribute and at least one other attribute.
     */
    public boolean hasConditionalConstraints(String attribute) {
        return stream()
                .anyMatch(rule ->
                        rule.getInvolvedAttributes().size() > 1
                                && rule.involves(attribute));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SigmaRuleset that = (SigmaRuleset) o;
        return Objects.equals(contractors, that.contractors) && Objects.equals(getRules(), that.getRules());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(contractors);
        result = 31 * result + Objects.hashCode(getRules());
        return result;
    }

    @Override
    public String toString() {
        return "SigmaRuleset{" +
                "contractors=" + contractors +
                ", sigmaRules=" + getRules() +
                '}';
    }

    public SigmaRuleset merge(SigmaRuleset other) throws SigmaRuleException
    {
        //Check for conflicts in contractors
        Set<String> conflictAttributes = other
            .getContractors()
            .keySet()
            .stream()
            .filter(oa ->
                    contractors.get(oa) != null
                &&  !Objects.equals(
                        contractors.get(oa),
                        other.getContractors().get(oa)))
            .collect(Collectors.toSet());

        if(!conflictAttributes.isEmpty())
            throw new SigmaRuleException("Cannot merge SigmaRulesets. "
                + "Cause: there is conflict in assigned contractors for "
                + "attributes " + conflictAttributes);

        Map<String, SigmaContractor<?>> mergedContractors = new HashMap<>();

        mergedContractors.putAll(contractors);
        mergedContractors.putAll(other.getContractors());

        return new SigmaRuleset(
                mergedContractors,
                Stream.concat(
                                stream(),
                                other.stream())
                        .distinct()
                        .collect(Collectors.toSet())
        );

    }
    
    /**
     * Provides an iterator over the permitted values an attribute can take,
     * restrained by a set of rules. This method is typically used to construct
     * a minimal set of values within the domain a datatype declares.
     * @param <T>
     * @param attribute
     * @param rules
     * @return 
     */
    public <T extends Comparable<? super T>> ValueIterator<T> getPermittedValues(String attribute)
    {        
        Map<String, SigmaContractor<?>> contract = new HashMap<>();
        contract.put(
            attribute,
            getContractors().get(attribute));

        SigmaRuleset proj = new SigmaRuleset
        (
            contract,
            getDomainConstraints(attribute)
        );
        
        return RepairOperations.getValueIterator(proj, attribute, null);
    }
}
