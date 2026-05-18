package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetNominalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleException;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleFolder
{
    private final Map<String, SigmaContractor<?>> contractors;
    
    private final Map<String, Set> definition;

    public SimpleFolder(Map<String, SigmaContractor<?>> contractors, Map<String, Set> definition)
    {
        this.contractors = contractors;
        this.definition = definition;
    }
    
    public Set<SigmaRule> foldRules(Set<SigmaRule> rules) throws SigmaRuleException {

        // The variable attributeCounter should be a LinkedHashMap object since we want to maintain the insertion order
        LinkedHashMap<String, Integer> attributeCounter = countRulesPerAttribute(rules);
        foldRulesRecursively(
            rules,
            attributeCounter);
        return rules;

    }

    protected void foldRulesRecursively(Set<SigmaRule> rules, LinkedHashMap<String, Integer> attributeCounter, String previousAttribute) throws SigmaRuleException {

        int totalEnteringAttributes = attributeCounter.values().stream().reduce(0, Integer::sum);

        // Some statistics used by the heuristic to decide the next attribute to combine on
        int maxDifference = 0;
        String bestAttribute = null;
        Set<SigmaRule> bestRules = null;
        LinkedHashMap<String, Integer> bestAttributeCounter = null;

        for (String attribute : attributeCounter.keySet())
        {
            // Not possible to combine on the same attribute in succession
            if (!attribute.equals(previousAttribute))
            {
                // Fold rules and calculate the number of times an attribute enters in a rule
                Set<SigmaRule> foldedRules = foldRulesByAttribute(rules, attribute);
                LinkedHashMap<String, Integer> newAttributeCounter = countRulesPerAttribute(foldedRules);

                // Calculate the profit of using the current attribute
                int currentDifference = totalEnteringAttributes - newAttributeCounter.values().stream().reduce(0, Integer::sum);

                // Update statistics if the current profit is higher than the previous best profit
                if (currentDifference > maxDifference)
                {
                    maxDifference = currentDifference;
                    bestAttribute = attribute;
                    bestRules = foldedRules;
                    bestAttributeCounter = newAttributeCounter;
                }
            }
        }

        // If it is the case that no new combinations can be formed
        if (maxDifference != 0)
        {
            // Recursive call
            foldRulesRecursively(
                bestRules,
                bestAttributeCounter,
                bestAttribute);
        }

    }

    protected void foldRulesRecursively(Set<SigmaRule> rules, LinkedHashMap<String, Integer> attributeCounter) throws SigmaRuleException
    {
        foldRulesRecursively(rules, attributeCounter, null);
    }

    protected LinkedHashMap<String, Integer> countRulesPerAttribute(Set<SigmaRule> rules) {

        // Initialize HashMap object that counts for each attributes
        //the number of times it appears as non-entering attribute in the given ruleset
        Map<String, Integer> attributeCounter = new HashMap<>();
        definition
            .keySet()
            .forEach(a -> attributeCounter.put(a, 0));

        rules.forEach(rule -> rule
                .getInvolvedAttributes()
                .forEach(involvedAttribute ->
                    attributeCounter.put(
                        involvedAttribute,
                        attributeCounter.get(involvedAttribute) + 1)
                ));

        // Sort entries in attributeCounter Map object based on value (descending)
        LinkedHashMap<String, Integer> sortedAttributeCounter = new LinkedHashMap<>();
        attributeCounter.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedAttributeCounter.put(x.getKey(), x.getValue()));

        return sortedAttributeCounter;

    }

    protected Set<SigmaRule> foldRulesByAttribute(Set<SigmaRule> rules, String attribute)
    {
        Map<SigmaRule, Set> projectedCombinedRules = new HashMap<>();

        // Project each SigmaRule object over the given attribute
        rules.forEach(sigmaRule -> {

            // Get all values of the rule for the given attribute
            // This Set object is empty if the set of values equals the domain
            Set valuesToAdd = getValuesToAdd(sigmaRule, attribute);

            SigmaRule projection = sigmaRule.project(attribute);

            // Update the HashMap object projectedCombinedRules
            // If the projection does not exists as key or the given attribute is not involved in the rule (valuesToAdd is empty),
            //add a new entry (projection, valuesToAdd)
            // Otherwise, add valuesToAdd to the current existing values for this projection
            if (!projectedCombinedRules.containsKey(projection) || valuesToAdd.isEmpty())
            {
                projectedCombinedRules.put(projection, valuesToAdd);
            }
            else
            {

                Set currentAttributeValues = projectedCombinedRules.get(projection);

                // Only add values if not all values of the given attribute's domain are provided (currentAttributeValues is empty)
                if (!currentAttributeValues.isEmpty()) {

                    currentAttributeValues.addAll(valuesToAdd);

                    // If the variable currentAttributeValues contains all values of the given attribute's domain, add an empty HashSet object
                    if (!currentAttributeValues.containsAll(definition.get(attribute)))
                    {
                        projectedCombinedRules.put(projection, currentAttributeValues);
                    }
                    else
                    {
                        projectedCombinedRules.put(projection, new HashSet<>());
                    }
                }
            }

        });

        // Create a new CategoricalRuleSet object given the HashMap variable projectedCombinedRules
        return getCombinedRules(projectedCombinedRules, attribute);

    }

    private Set getValuesToAdd(SigmaRule sigmaRule, String attribute)
    {
        Set valuesToAdd = new HashSet<>();

        if (sigmaRule.involves(attribute))
        {
            Set<SetAtom> setAtoms = sigmaRule
                .getAtoms()
                .stream()
                .filter(atom -> atom instanceof SetAtom
                    && atom.getAttributes().allMatch(aa -> aa.equals(attribute))
                )
                .map(atom -> (SetAtom)atom)
                .collect(Collectors.toSet());
            
            if(setAtoms.size() != 1)
                throw new SigmaRuleException("Unexpected amount of set atoms during rule folding.");
            
            SetAtom atom = setAtoms.stream().findFirst().get();
            
            valuesToAdd.addAll(atom.getConstants());
        }

        return valuesToAdd;

    }

    private Set<SigmaRule> getCombinedRules(Map<SigmaRule, Set> projectedCombinedRules, String attribute)
    {

        Set<SigmaRule> foldedRules = new HashSet<>();

        // For each entry in Map variable projectedCombinedRules,
        // add given attribute with values to create a new SigmaRule object
        projectedCombinedRules.forEach((projection, attributeValues) ->
        {
            Map<String, Set> foldedAttributeConditions = projection
                .getAtoms()
                .stream()
                .map(atom -> (SetAtom)atom)
                .collect(Collectors.toMap(
                    atom -> atom.getAttribute(),
                    atom -> atom.getConstants()));

            if (!attributeValues.isEmpty())
            {
                foldedAttributeConditions.put(attribute, attributeValues);
            }

            foldedRules.add(buildNewSigmaRule(foldedAttributeConditions));

        });

        return foldedRules;

    }

    private SigmaRule buildNewSigmaRule(Map<String, Set> foldedAttributeConditions)
    {
        Set<AbstractAtom<?,?,?>> atoms = new HashSet<>();
        
        for(String a: foldedAttributeConditions.keySet())
        {
            if(contractors.get(a) instanceof NominalContractor)
            {
                atoms.add(new SetNominalAtom(
                    (NominalContractor) contractors.get(a),
                    a,
                    SetOperator.IN,
                    foldedAttributeConditions.get(a)));
            }
            else if(contractors.get(a) instanceof OrdinalContractor)
            {
                atoms.add(new SetOrdinalAtom(
                    (OrdinalContractor) contractors.get(a),
                    a,
                    SetOperator.IN,
                    foldedAttributeConditions.get(a)));
            }
            else
            {
                throw new SigmaRuleException("Unknown contractor type encountered in rule folding.");
            }
        }
        
        return new SigmaRule(atoms);
    }
}
