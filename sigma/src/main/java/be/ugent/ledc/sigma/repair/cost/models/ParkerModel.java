package be.ugent.ledc.sigma.repair.cost.models;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.fundy.datastructures.PartialKey;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.ConstantCostFunction;
import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.repair.RepairOperations;
import be.ugent.ledc.sigma.repair.bounding.Bounding;
import be.ugent.ledc.sigma.repair.bounding.EnumeratedBounding;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.functions.SimpleIterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.iterators.CrossProductIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParkerModel<C extends CostFunction> extends CostModel<C>
{
    public static final int BOUND_LIMIT = 5000;

    /**
     * A strategy to limit permitted unbounded attributes in avoidance of quasi-infinite loops
     */
    private final Bounding<?, SigmaContractor<?>, ValueIterator<?>> bounding;

    /**
     * The partial key that needs to be satisfied.
     */
    private final PartialKey partialKey;

    private final Map<String, ValueIterator<?>> permittedValues;

    private final Set<String> unboundedAttributes;

    private final SCCSigmaRuleset rules;

    private final boolean forceBounding;
    
    /**
     * A flag that indicates whether or not cost function alternatives need to
     * be added to the bounded set of values.
     */
    private final boolean expand;

    public ParkerModel(PartialKey partialKey, Map<String, C> costFunctions, SCCSigmaRuleset rules, Bounding<?, SigmaContractor<?>, ValueIterator<?>> bounding, boolean forceBounding, boolean expand) throws RepairException
    {
        super(costFunctions);
        this.partialKey = partialKey;
        this.bounding = bounding;
        this.rules = rules;
        this.forceBounding = forceBounding;
        this.expand = expand;

        this.permittedValues = RepairOperations.getPermittedValues(
                new DataObject(),
                rules
                    .getContractors()
                    .keySet()
                    .stream()
                    .filter(a -> !rules
                            .getContractors()
                            .get(a)
                            .name()
                            .equals(SigmaContractorFactory.STRING.name())
                            ||  !rules.getDomainConstraints(a).isEmpty())
                    .collect(Collectors.toSet()),
                new HashSet<>(),
                rules);

        this.unboundedAttributes = this.permittedValues
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().cardinality() > BOUND_LIMIT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        this.unboundedAttributes.addAll(
                rules
                        .getContractors()
                        .keySet()
                        .stream()
                        .filter(a -> rules
                                .getContractors()
                                .get(a)
                                .name()
                                .equals(SigmaContractorFactory.STRING.name())
                                &&  rules.getDomainConstraints(a).isEmpty())
                        .collect(Collectors.toSet())
        );
    }

    public ParkerModel(PartialKey partialKey, Map<String, C> costFunctions, SCCSigmaRuleset rules,Bounding<?, SigmaContractor<?>, ValueIterator<?>> bounding) throws RepairException
    {
        this(partialKey, costFunctions, rules, bounding, false, false);
    }

    public ParkerModel(PartialKey partialKey, Map<String, C> costFunctions, SCCSigmaRuleset rules) throws RepairException
    {
        this(partialKey, costFunctions, rules, new EnumeratedBounding(), false, false);
    }

    public ParkerModel(PartialKey partialKey, SCCSigmaRuleset rules) throws RepairException
    {
        this(partialKey,
            Stream.concat(
                    rules.getRules().stream().flatMap(rule -> rule.getInvolvedAttributes().stream()),
                    partialKey.getDetermined().stream())
                .distinct()
                .collect(Collectors.toMap(
                    a->a,
                    a-> (C)new ConstantCostFunction<>()
                )),
            rules,
            new EnumeratedBounding(),
            false,
            false
        );
    }

    public PartialKey getPartialKey()
    {
        return partialKey;
    }

    public Bounding<?, SigmaContractor<?>, ValueIterator<?>> getBounding() {
        return bounding;
    }

    /**
     * A method to create, for a given bag of objects, those objects o that inflict
     * minimal cost when all objects in the bag are replaced by o.
     * These objects are the ones that can satisfy a key constraint
     * with minimal cost.
     * @param objectBag
     * @return
     */
    public CrossProductIterator minimalCostObjects(Multiset<DataObject> objectBag)
    {
        //For each attribute, keep the values that inflict minimal cost
        Map<String, List<?>> minChangeValues = new HashMap<>();

        for(String a: partialKey.getDetermined())
        {
            if(getCostFunction(a) == null)
            {
                //No cost function => keep original values
                minChangeValues.put(
                        a,
                        objectBag
                                .keySet()
                                .stream()
                                .map(o -> o.get(a))
                                .toList());
                continue;
            }

            Pair<Integer, Set<Object>> bestValues = new Pair<>(Integer.MAX_VALUE, new HashSet<>());
            ValueIterator<?> valueIterator;

            if (forceBounding)
            {
                valueIterator = bounding.boundPermittedValues(
                    a,
                    objectBag,
                    rules.getContractors().get(a),
                    permittedValues.get(a),
                    expand ? computeHints(objectBag, a) : new HashSet<>()
                );
            }
            else
            {
                valueIterator = !unboundedAttributes.contains(a)
                    ? permittedValues.get(a)
                    : bounding.getPermittedValues(
                    a,
                    objectBag,
                    rules.getContractors().get(a),
                    expand ? computeHints(objectBag, a) : new HashSet<>());
            }

            for(Object repairValue: valueIterator)
            {
                //Compute the cost associated with keeping this value
                int valueCost = objectBag
                    .keySet()
                    .stream()
                    .mapToInt(o -> objectBag.get(o) * getCostFunction(a).cost(o.get(a), repairValue, o))
                    .sum();

                if(valueCost < bestValues.getFirst())
                    bestValues = new Pair<>(valueCost, new HashSet<>());

                if(valueCost <= bestValues.getFirst())
                    bestValues.getSecond().add(repairValue);
            }

            minChangeValues.put(a, new ArrayList<>(bestValues.getSecond()));
        }

        //Return a cross product iterator
        return new CrossProductIterator(minChangeValues);
    }

    /**
     * This method builds an induced cost model for a given dirty object in the
     * scope of a partial key bag and a non partial key bag.
     * @param dirty The object that needs repairing
     * @param cover The cover on which we iterate
     * @param conditionals The attributes we have already fixed for some reason
     * @param objectBag The bag of objects for which consensus must be reached.
     * @param localRules The rules that each object must satisfy
     * @return A map of induced iterable cost functions for each relevant attribute.
     * @throws be.ugent.ledc.core.RepairException
     */
    public IterableCostModel buildInducedCostModel(DataObject dirty,
                                                   Set<String> cover,
                                                   Set<String> conditionals,
                                                   Multiset<DataObject> objectBag,
                                                   SigmaRuleset localRules) throws RepairException
    {
        //Initialize a mapping from attributes to iterable cost functions
        Map<String, IterableCostFunction> costFunctionMapping = new HashMap<>();

        //Compute the histogram
        Map<String, Map<Object, Integer>> histograms = buildHistograms(objectBag);

        //Determine which attributes to include in the induced model
        Set<String> inducedModelAttributes = new HashSet<>(cover);

        for(DataObject full: objectBag.keySet())
        {
            DataObject fullObject = new DataObject()
                    .concat(full)
                    .concat(dirty);

            inducedModelAttributes.addAll(
                    rules
                            .failingRules(fullObject) //For each failing rule...
                            .stream()
                            .filter(rule -> rule
                                    .getInvolvedAttributes()
                                    .stream()
                                    .anyMatch(a -> !dirty.getAttributes().contains(a)))
                            // ... that involves a non-key attribute...
                            .flatMap(rule -> rule
                                    .getInvolvedAttributes()    // ... get involved attributes ...
                                    .stream()
                                    .filter(a -> dirty.getAttributes().contains(a))// ... but in the partial key
                            )
                            .collect(Collectors.toSet())
            );
        }

        Map<String, ValueIterator<?>> localPermittedValues = RepairOperations
            .getPermittedValues(
                dirty,
                inducedModelAttributes,
                conditionals,
                localRules
            );

        //For each attribute in the induced model, build an induced cost function
        for(String a: inducedModelAttributes)
        {
            //Initialize the cost model for this attribute
            costFunctionMapping.put(
                    a,
                    buildCostFunction(
                            dirty,
                            cover,
                            a,
                            localPermittedValues.get(a),
                            objectBag.cardinality(),
                            histograms.get(a)
                    ));
        }

        return new IterableCostModel(costFunctionMapping);
    }


    private Map<String, Map<Object,Integer>> buildHistograms(Multiset<DataObject> objectBag)
    {
        Map<String, Map<Object,Integer>> histograms = new HashMap<>();

        for(DataObject o: objectBag.keySet())
        {
            for(String a: o.getAttributes())
            {
                histograms.computeIfAbsent(a, k -> new HashMap<>());

                if(o.get(a) != null)
                {
                    histograms.get(a).merge(o.get(a), objectBag.get(o), Integer::sum);
                }
            }
        }

        return histograms;
    }

    private IterableCostFunction buildCostFunction(DataObject dirty, Set<String> cover, String a, ValueIterator<?> permittedValues, int bagCardinality, Map<Object,Integer> histogram)
    {
        //Prepare the induced cost mapping
        Map<Object, Map<Object, Integer>> inducedCostMapping = new HashMap<>();

        //Get the value for attribute a the object currently has
        Object originalValue = dirty.get(a);

        inducedCostMapping.put(originalValue, new HashMap<>());

        for(Object aValue: permittedValues)
        {
            if(!aValue.equals(dirty.get(a)) || !cover.contains(a))
            {
                //Calculate the increase in cost for this repair value
                int increaseMultiplier = histogram.get(originalValue) == null ? bagCardinality : histogram.get(originalValue);
                Integer increase = increaseMultiplier * getCostFunction(a).cost(originalValue, aValue, dirty);

                //Calculate the decrease in cost for this repair value
                int decreaseMultiplier = histogram.get(aValue) == null ? 0 : histogram.get(aValue);
                Integer decrease = decreaseMultiplier * getCostFunction(a).cost(aValue, originalValue, dirty);

                int cost = increase - decrease;

                if(cost > 0 || originalValue == null)
                {
                    inducedCostMapping.get(originalValue).put(aValue, cost);
                }
            }
        }

        if(inducedCostMapping.get(originalValue).isEmpty())
        {
            inducedCostMapping.get(originalValue).put(originalValue, 0);
        }

        return new SimpleIterableCostFunction(inducedCostMapping);
    }

    public SigmaRuleset boundRules(SigmaRuleset ruleset, Multiset<DataObject> objectBag)
    {
        Set<SigmaRule> additionalRules;

        if (forceBounding)
        {
            additionalRules = ruleset
                .getContractors()
                .keySet()
                .stream()
                .filter(a -> objectBag //Don't add constraints for attributes that are null
                    .keySet()
                    .stream()
                    .anyMatch(o -> o.get(a) != null))
            .flatMap(a -> bounding.getDomainRules(
                    a,
                    objectBag,
                    ruleset.getContractors().get(a),
                    bounding.boundPermittedValues(
                        a,
                        objectBag,
                        ruleset.getContractors().get(a),
                        bounding.getPermittedValues(
                            a,
                            objectBag,
                            ruleset.getContractors().get(a),
                            expand ? computeHints(objectBag, a) : new HashSet<>()),
                        expand ? computeHints(objectBag, a) : new HashSet<>()
                    )
                ).stream())
                .collect(Collectors.toSet());
        }
        else
        {
            additionalRules = unboundedAttributes
                    .stream()
                    .filter(a -> ruleset.getContractors().get(a) != null)
                    .filter(a -> objectBag //Don't add constraints for attributes that are null
                        .keySet()
                        .stream()
                        .anyMatch(o -> o.get(a) != null))
                    .flatMap(a -> bounding.getDomainRules(
                        a,
                        objectBag,
                        ruleset.getContractors().get(a),
                        bounding.getPermittedValues(
                            a,
                            objectBag,
                            ruleset.getContractors().get(a),
                            expand ? computeHints(objectBag, a) : new HashSet<>()
                        )
                    ).stream())
                    .collect(Collectors.toSet());
        }

        if(additionalRules.isEmpty())
        {
            return ruleset;
        }

        return new SigmaRuleset(
                ruleset.getContractors(),
                Stream.concat(
                        ruleset.getRules().stream(),
                        additionalRules.stream()
                ).collect(Collectors.toSet())
        );

    }

    public SCCSigmaRuleset getRules()
    {
        return rules;
    }

    public boolean isForceBounding() {
        return forceBounding;
    }

    public Set<String> getUnboundedAttributes() {
        return unboundedAttributes;
    }

    public boolean hasBounding()
    {
        //Bounding is applied either if it is forced or unbound attributes is empty
        return isForceBounding() || !getUnboundedAttributes().isEmpty();
    }

    private Set computeHints(Multiset<DataObject> objectBag, String a)
    {
        //Initialize
        HashSet<Object> hints = new HashSet<>();

        for(DataObject o: objectBag.keySet())
        {
            if(o.get(a) != null)
            {
                hints.addAll(getCostFunction(a).alternatives(o.get(a), o));
            }
        }

        return hints;
    }
}