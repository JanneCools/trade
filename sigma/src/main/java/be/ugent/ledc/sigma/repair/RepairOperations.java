package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.core.util.ListOperations;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleException;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetOperations;
import be.ugent.ledc.sigma.datastructures.values.IntervalOperations;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.OrdinalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.*;
import java.util.stream.Collectors;

public class RepairOperations
{
    /**
     * Provides an iterator over the permitted values an attribute can take, in
     * the context of an ongoing repair process. More specifically, this method
     * assume that some attributes must retain their values and the targeted
     * attribute must change it's value. To that extent, this method accounts for:
     * 
     * 1. the current value of the target attribute (attributeToTreat)
     * 2. the attributes + values that are fixed (repair)
     * 3. a selection of the rules that are relevant in determining the permitted
     * values of the target attribute.
     * 
     * To compute the permitted values, the selected rules are "fixed" on the repair,
     * meaning any atom with an attribute that appears in the repair, is transformed
     * by filling the value of that attribute. Doing so can create additional
     * restrictions on the permitted values of the target attribute. For example,
     * if the selected rules contain NOT (a in {1,2} AND b==1) and the repair is
     * {b=1}, then attribute a cannot take values 1 and 2.
     * 
     * @param <T>
     * @param target Target attribute 
     * @param current Current 'dirty' value of the target attribute
     * @param selectedRules A selection of rules relevant to restrain the domain
     * of the target
     * @param repair
     * @return 
     * @throws RepairException
     */
    public static <T extends Comparable<? super T>> ValueIterator<T> getPermittedValues(String target, T current, SigmaRuleset selectedRules, DataObject repair) throws RepairException
    {
        //We fix those rules selected
        Set<SigmaRule> fixedRules = selectedRules
            .getRules()
            .stream()
            .map(rule -> rule.fix(repair))
            .collect(Collectors.toSet());

        //Sanity check:
        //if some rule contains only ALWAYS_TRUE atoms, the rule will always fail...
        boolean contradiction = fixedRules
            .stream()
            .anyMatch(rule ->
                rule
                    .getAtoms()
                    .isEmpty()
            ||  rule
                    .getAtoms()
                    .stream()
                    .allMatch(atom -> atom.equals(AbstractAtom.ALWAYS_TRUE)));
        
        if (contradiction)
        {
            throw new RepairException(
                """
                Repair encoutered a situation where no repairs are possible:
                Attribute to change: """ + target + "\n"
                + "Repair so far: " + repair + "\n"
                + "Rules: " + selectedRules
                .getRules()
                .stream()
                .map(SigmaRule::toString)
                .collect(Collectors.joining("\n"))
            );
        }

        fixedRules.removeIf(rule -> rule
            .getAtoms()
            .stream()
            .allMatch(atom -> atom.equals(AbstractAtom.ALWAYS_FALSE))
        );

        //Filter out atoms containing no attributes
        for (SigmaRule rule : fixedRules)
        {
            rule.getAtoms().removeIf(atom -> atom.getAttributes().findAny().isEmpty());
        }

        Map<String, SigmaContractor<?>> cleanedContractors = new HashMap<>(selectedRules.getContractors());

        repair.getAttributes().forEach(cleanedContractors::remove);

        SigmaRuleset fixed = new SigmaRuleset(cleanedContractors, fixedRules);

        return getValueIterator(
            fixed.project(SetOperations.set(target)),
            target,
            current
        );
    }
    
    public static Map<String, ValueIterator<?>> getPermittedValues(DataObject dirty, Set<String> attributes, Set<String> conditionals, SigmaRuleset rules) throws RepairException
    {
        Map<String, ValueIterator<?>> permittedValues = new HashMap<>();

        for(String attribute: attributes)
        {
            Set<SigmaRule> selectedRules = rules
                .getRules()
                .stream()
                .filter(rule -> rule
                    .getInvolvedAttributes()
                    .stream()
                    .allMatch(a -> a.equals(attribute) || conditionals.contains(a)))
                .collect(Collectors.toSet());

            SigmaRuleset selectedRuleset = new SigmaRuleset(
                rules
                    .getContractors()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().equals(attribute) || conditionals.contains(e.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue)
                    ),
                selectedRules);

            permittedValues.put(
                attribute,
                getPermittedValues(
                    attribute,
                    (Comparable)dirty.get(attribute),
                    selectedRuleset,
                    dirty.project(conditionals))
            );
        }

        return permittedValues;
    }

    public static <T extends Comparable<? super T>> ValueIterator<T> getValueIterator(SigmaRuleset rules, String target, T current)
    {
        SigmaContractor<?> contractor = rules
            .getContractors()
            .get(target);

        //Collect univariate rules that involve the target
        Set<SigmaRule> univariateRules = rules
            .stream()
            .filter(rule ->
                    rule.getInvolvedAttributes().size() == 1
                &&  rule.involves(target))
            .collect(Collectors.toSet());
        
        if (contractor instanceof NominalContractor)
        {
            Set<String> permittedValues = SigmaRulesetOperations.convertUnivariateRulesToSet(univariateRules, target);

            permittedValues.remove((String)current);

            return new NominalValueIterator(permittedValues);

        }
        else if (contractor instanceof OrdinalContractor)
        {
            OrdinalContractor<T> cContractor = (OrdinalContractor<T>) contractor;

            //Start with an unlimited interval
            List<Interval<T>> result = ListOperations.list(new Interval<>(null,null));
        
            LinkedList<List<Interval<T>>> stack = new LinkedList<>();
            stack.push(result);
            
            for(SigmaRule uRule: univariateRules)
            {
                List<Interval<T>> ruleIntervals = new ArrayList<>();
                
                for(AbstractAtom<?,?,?> atom: uRule.getAtoms())
                {   
                    Set<Interval<T>> atomIntervals =  AtomOperations.<T>getIntervalsFromOrdinalAtoms(
                        SetOperations.set(atom.getInverse()),
                        target
                    );
                    
                    ruleIntervals = IntervalOperations.union(
                        ruleIntervals,
                        atomIntervals
                            .stream()
                            .sorted(Interval.leftBoundComparator())
                            .collect(Collectors.toList()),
                        cContractor);
                }
            
                result = IntervalOperations.intersect(result, ruleIntervals);
            }
            
            return new OrdinalValueIterator<>(result, cContractor);
        }
        else
            throw new SigmaRuleException("Unknown type of contractor: expected either nominal or ordinal data");
    }
}
