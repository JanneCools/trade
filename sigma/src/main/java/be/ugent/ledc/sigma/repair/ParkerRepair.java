package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.formulas.CPFException;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetException;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.iterators.CrossProductIterator;
import be.ugent.ledc.sigma.repair.cost.models.IterableCostModel;
import be.ugent.ledc.sigma.repair.cost.models.ParkerModel;
import be.ugent.ledc.sigma.repair.covers.ConditionalSetCoverSearch;
import be.ugent.ledc.sigma.repair.covers.SubsetMinimalCoverSearch;
import be.ugent.ledc.sigma.repair.selection.RepairSelection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ParkerRepair engine considers next the usual set of sigma rules a partial key constraint.
 * That constraint enforces a certain degree of correspondence between rows in a dataset.
 * 
 * @author abronsel
 * @param <M> The type of cost model used by this instances of Parker. This model
 * needs to be an extension of a ParkerModel
 * @param <C> The type of cost function used by the cost model M
 */
public class ParkerRepair<C extends CostFunction, M extends ParkerModel<C>> extends RepairEngine<M, SCCSigmaRuleset>
{    
    private final RepairSelection repairSelection;
    
    private final Map<String, Map<SigmaRule, List<SigmaRule>>> concurrentRules;
    
    /**
     * A flag that indicates whether or not objects can be projected when
     * constructing bags. When set to true, the repair search will project
     * objects such that only repairable attributes are kept. This removes
     * context attributes (e.g. sources) and so cost functions that use such
     * context cannot be used any more.
     * 
     * Allowing projection will offer significant speed-up in the search process.
     */
    private final boolean bagProject;
   
    public ParkerRepair(M costModel, RepairSelection repairSelection)
    {
        this(costModel, repairSelection, false);
    }
    
    public ParkerRepair(M costModel, RepairSelection repairSelection, boolean bagProject)
    {
        this(costModel, repairSelection, NullBehavior.CONSTRAINED_REPAIR, bagProject);
    }
    
    public ParkerRepair(M costModel, RepairSelection repairSelection, NullBehavior nullBehaviour, boolean bagProject)
    {
        super(SCCSigmaRuleset.create(
            new SigmaRuleset(
                costModel
                    .getRules()
                    .getContractors(),
                costModel
                    .getRules()
                    .stream()
                    .collect(Collectors.toSet()))),
            costModel,
            nullBehaviour);
        this.repairSelection = repairSelection;
        this.concurrentRules = buildConcurrentRules();
        this.bagProject = bagProject;
    }

    @Override
    public Dataset buildRepair(Dataset dataset) throws RepairException
    {
        //Initialize repaired data
        Dataset repairedDataset = new SimpleDataset();
        
        //Next, build equivalence classes per value of the key
        Map<DataObject, List<DataObject>> equivClasses = dataset
            .getDataObjects()
            .stream()
            .filter(o -> getCostModel()
                .getPartialKey()
                .getKey()
                .stream()
                .noneMatch(a -> o.get(a) == null)
            )
            .collect(Collectors
                .groupingBy(o -> o.project(
                    getCostModel()
                    .getPartialKey()
                    .getKey())));
        
        //Build the set of attributes involved in some edit rule, but non determined by the partial key
        //These attributes are special in the sense that they
        //(a) need to be consistent for each tuple
        //(b) need not to be the same for each tuple in the class
        Set<String> nonKeyAttributes = getRules()
        .getRules()
        .stream()
        .flatMap(rule -> rule
            .getInvolvedAttributes()
            .stream()
            .filter(a -> !getCostModel()
                .getPartialKey()
                .getDetermined()
                .contains(a))
            )
        .collect(Collectors.toSet());
        
        Set<String> repairableAttributes = Stream.concat(
            getCostModel().getPartialKey().getDetermined().stream(),
            nonKeyAttributes.stream()
        ).collect(Collectors.toSet());
        
        Set<String> priorConditionals = repairableAttributes
            .stream()
            .filter(a -> getCostModel().getCostFunction(a) == null)
            .collect(Collectors.toSet());
        
        //Project the rules to the attributes determined by the partial key
        SCCSigmaRuleset partialKeyRules = getRules().project(
            getCostModel()
                .getPartialKey()
                .getDetermined()
        );
        
        int classesWithNoRepairCount = 0;
        int tuplesWithNoRepairCount = 0;
        
        //Repair the dataset minimally is done by repairing each class independently
        for(Map.Entry<DataObject, List<DataObject>> e: equivClasses.entrySet())
        {
            if(VERBOSITY > 1)
                System.out.println("Repairing class: " + e.getKey());
            
            //Find distinct objects in the class
            Multiset<DataObject> objectBag = new Multiset<>();
            
            Set<DataObject> proj = new HashSet<>();
            
            for(DataObject o: e.getValue())
            {                              
                proj.add(
                    o.project(getCostModel()
                        .getPartialKey()
                        .getDetermined())
                );
                
                if(bagProject)
                {
                    objectBag.add(o.project(repairableAttributes), 1);
                }
                else
                {
                    objectBag.add(o, 1);
                }
            }
            
            Set<DataObject> mcrs;
            
            //In case of clean data...
            if(proj.size() == 1
            && proj.stream().allMatch(partialKeyRules::isSatisfied)
            && proj
                .stream()
                .allMatch(o -> o
                    .getAttributes()
                    .stream()
                    .allMatch(a -> o.get(a) != null || !getCostModel()
                        .getPartialKey()
                        .getDetermined()
                        .contains(a)))
            && nonKeyAttributes.isEmpty()
            )
            {
                mcrs = proj;
            }
            else
            {
                //First find those objects that would require minimal changes to satisfy the partial key constraint
                CrossProductIterator minChangeIterator = getCostModel().minimalCostObjects(objectBag);

                Set<DataObject> minChangeObjects = new HashSet<>();
            
                while(minChangeIterator.hasNext())
                {
                    minChangeObjects.add(minChangeIterator.next());
                }
                    
                //Keep only those minChangeObjects that are consistent
                mcrs = 
                    minChangeObjects
                    .stream()
                    .filter(mco ->
                        objectBag
                        .keySet()
                        .stream()
                        .allMatch(repairObject ->
                        getRules().isSatisfied(repairObject.concat(mco)))
                    )
                    .collect(Collectors.toSet());
                
                if(mcrs.isEmpty())
                {
                    if(VERBOSITY > 2)
                        System.out.println("Searching minimal cost repairs");

                    //This is where the magic is done...
                    mcrs = minConsistentRepairs(
                        getCostModel().getPartialKey().getDetermined(),
                        objectBag,
                        minChangeObjects,
                        new HashSet<>(priorConditionals),
                        partialKeyRules
                    );
                }
                else
                {
                    if(VERBOSITY > 2)
                        System.out.println("Consistent objects with minimal changes found...");
                }
            }  

            if(mcrs.isEmpty())
            {
                classesWithNoRepairCount++;
                tuplesWithNoRepairCount+= e.getValue().size();
//                
//                System.out.println(e.getKey());
                if(VERBOSITY > 3)
                    System.out.println("Warning: could not find a repair for class with key " + e.getKey());
                
            }
                         
            //We select a repair for the partial key
            DataObject partialKeyRepair = repairSelection
                .selectRepair(mcrs);
                           
            for(DataObject original: e.getValue())
            {
                //Make a copy of the original
                DataObject repaired = new DataObject(original);
                
                if(partialKeyRepair != null)
                {
                    //Apply the repair for the partial key
                    repaired = repaired.concat(partialKeyRepair);   
                }
                
                if(needsRepair(repaired, getCostModel().getPartialKey().getDetermined()) && VERBOSITY > 3)
                {
                    System.out.println("Warning: not all partial key attributes were repaired.");
                }
                
                //Possibly, this repair still violates some rules due to its values for non key attributes
                if(needsRepair(repaired))
                {
                    Multiset<DataObject> localBag = new Multiset<>();

                    localBag.add(repaired, 1);

                    Set<DataObject> minimalRepairs = minConsistentRepairs(
                        nonKeyAttributes,
                        localBag,
                        Stream.of(repaired).collect(Collectors.toSet()),
                        getCostModel().getPartialKey().getDetermined(),
                        getRules()
                    ); 

                    DataObject selectedNonKeyRepair = repairSelection.selectRepair(minimalRepairs);
                    if(selectedNonKeyRepair != null)
                    {
                        repaired = repaired.concat(selectedNonKeyRepair);
                    }
                }
                
                repairedDataset.addDataObject(repaired);
            }
        }
        
        if(getCostModel().hasBounding() && VERBOSITY > 3)
        {
            System.out.println("Classes with no repair: " + classesWithNoRepairCount);
            System.out.println("Tuples with no repair: " + tuplesWithNoRepairCount);
        }
        
        return repairedDataset;
    }
    
    private Set<DataObject> minConsistentRepairs(Set<String> attributes, Multiset<DataObject> objectBag, Set<DataObject> minChangeObjects, Set<String> conditionals, SCCSigmaRuleset localRules) throws RepairException
    {                      
        //Initiaze a pair to keep track of minimal repairs
        Pair<Integer, Set<DataObject>> minimalRepairs = new Pair<>(Integer.MAX_VALUE, new HashSet<>());
        
        //We sort objects according to the number of attributes that are offending
        //The idea is that having less offending attributes induces 
        //a higher chance of leading to minimal solutions
        List<DataObject> sortedBaseObject = minChangeObjects
            .stream()
            .sorted(Comparator.comparing(o -> offendingAttributes(o, localRules).size()))
            .toList();

        try
        {
            SigmaRuleset boundedRules = getCostModel().boundRules(localRules, objectBag);
            
            ConditionalSetCoverSearch coverSearch = new ConditionalSetCoverSearch(
                (boundedRules instanceof SCCSigmaRuleset
                    ? (SCCSigmaRuleset) boundedRules
                    : localRules),
                conditionals);

            for(DataObject mco: sortedBaseObject)
            {      
                if(VERBOSITY > 2)
                    System.out.println("Repairing: " + mco);
                
                //If the minimal cost object is consistent on the partial key attributes
                //we need to consider it as a part of a possible repair
                if(!needsRepair(mco, attributes))
                {
                    long cost = computeNonKeyCost(objectBag, mco, new HashSet<>());

                    if(cost < minimalRepairs.getFirst())
                    {
                        minimalRepairs.getSecond().clear();
                        minimalRepairs.getSecond().add(mco);
                    }
                    else if(cost == minimalRepairs.getFirst())
                    {
                        minimalRepairs.getSecond().add(mco);
                    }
                }

                //Now we get all solutions that are minimal in terms of subset-monotonity
                //These are set covers for which no subset is a set cover.
                List<Set<String>> monotoneSetCovers = coverSearch
                    .findMinimalCovers(mco, getNullBehavior())
                    .stream()
                    .filter(attributes::containsAll)
                    .sorted(Comparator.comparing(Set::size))
                    .toList();
                

                for(Set<String> cover: monotoneSetCovers)
                {
                    if(VERBOSITY > 2)
                        System.out.println("Cover: " + cover);
//
//                    if(cover.stream().anyMatch(a -> getCostModel().getUnboundedAttributes().contains(a) && mco.get(a) == null))
//                    {
//                        if(VERBOSE)
//                            System.out.println("Skipping cover: " + cover);
//                        continue;
//                    }

                    if(getCostModel().hasBounding())
                    {
                        cover.removeIf(a -> !getNullBehavior().repairIfNull(localRules, a) && mco.get(a) == null);
                        
                        if(VERBOSITY > 5)
                            System.out.println("Reduced cover accounting for NULLs: " + cover);
                    }

                    Pair<Integer, Set<DataObject>> solutionBestRepairs = inspectCover(
                        mco,
                        cover,
                        objectBag,
                        minimalRepairs.getFirst(),
                        0,               //Initially, there are no base changes to start from
                        conditionals, 
                        boundedRules);

                    if(solutionBestRepairs.getFirst() < minimalRepairs.getFirst())
                        minimalRepairs = solutionBestRepairs;
                    else if(solutionBestRepairs.getFirst().equals(minimalRepairs.getFirst()))
                        minimalRepairs.getSecond().addAll(solutionBestRepairs.getSecond());

                }
            }

            return minimalRepairs.getSecond();
        }
        catch (SigmaRulesetException ex)
        {
            if(ex.getMessage().startsWith("Set of rules is not satisfiable"))
                return new HashSet<>();
            else throw ex;
        }
        catch (RepairException ex)
        {
            if(ex.getMessage().startsWith("Repair encoutered a situation where no repairs are possible"))
                return new HashSet<>();
            else throw ex;
        }
        catch (CPFException ex)
        {
            if(ex.getMessage().startsWith("Set of reduced CPFs features contradiction"))
                return new HashSet<>();
            else throw ex;
        }
    }
       
    private Pair<Integer, Set<DataObject>> inspectCover(DataObject dirty, Set<String> cover, Multiset<DataObject> objectBag, Integer costUpperBound, Integer superCost, Set<String> conditionals, SigmaRuleset localRules) throws RepairException
    {
        //Init
        Pair<Integer, Set<DataObject>> minimalRepairs = new Pair<>(costUpperBound, new HashSet<>());
        
        //Get failing rules
        Set<SigmaRule> failingRules = localRules.failingRules(dirty);
        
        //Keep track of possible super covers to test.
        Map<DataObject, Integer> superCoverBuffer = new HashMap<>();

        //Build an iterator over (projected) objects in order of increasing cost
        IterableCostModel inducedCostModel = getCostModel()
            .buildInducedCostModel(
                dirty,
                cover,
                conditionals,
                objectBag,
                localRules);

        Iterator<DataObject> inducedIterator = inducedCostModel
            .buildLocoIterator(
                dirty,
                inducedCostModel.getAttributes(),
                conditionals,
                localRules,
                getCostModel().hasBounding()
            );

        //Iterate over the solution spaces
        while(inducedIterator.hasNext())
        {
            DataObject next = inducedIterator.next();
            if(VERBOSITY > 4)
                System.out.println("Next: " + next + " | " + conditionals);

            //Total cost: initialize it with the cost of the solution space
            int totalCost = superCost
                + inducedCostModel.cost(
                    dirty.project(next.getAttributes()),
                    next);

            //If the cost of solutions from this space is higher than
            //the current minimal cost, we don't need to search futher
            if(totalCost > minimalRepairs.getFirst())
                break;

            //Compose the candidate repair
            DataObject candidate = dirty
                .inverseProject(next.getAttributes())
                .concat(next);

            //Is this an actual repair on the partial key?
            //If not, we skip this solution
            if(!localRules.isSatisfied(candidate))
            {
                boolean mustInspect = true;

                for(String a: cover)
                {
                    if(failingRules
                        .stream()
                        .anyMatch(rule -> rule
                            .getVariableAtoms()
                            .stream()
                            .anyMatch(atom ->
                                    atom.getLeftAttribute().equals(a)
                                ||  atom.getRightAttribute().equals(a))))
                    {
                        mustInspect = true;
                        break;
                    }

                    //Construct an object like the repair, but no change for one attribute
                    DataObject contender = new DataObject(candidate).set(a, dirty.get(a));

                    //If the change does not fix at least one failing rule
                    if(failingRules
                        .stream()
                        .noneMatch(rule ->
                            rule.isSatisfied(candidate)
                        &&  !rule.isSatisfied(contender)
                        &&  (concurrentRules.get(a).get(rule) == null ||
                             concurrentRules.get(a).get(rule)
                                .stream()
                                .noneMatch(fcr -> fcr.isFailed(candidate))
                            )
                        )
                    )
                    {
                        mustInspect = false;
                        break;
                    }
                }

                if(mustInspect)
                    superCoverBuffer.put(next, totalCost);

                continue;
            }

            totalCost += (int) computeNonKeyCost(objectBag, candidate, next.getAttributes());

            //If the cost strictly lower than the current best solution
            //we remove current solutions and settle the new minimal cost
            if(totalCost < minimalRepairs.getFirst())
                minimalRepairs = new Pair<>(totalCost, new HashSet<>());

            if(totalCost <= minimalRepairs.getFirst())
                minimalRepairs.getSecond().add(next);
        }

        //Now test for super covers
        for(DataObject toTest: superCoverBuffer.keySet())
        {
            //Make a copy of the original object, but change the attributes in toTest
            DataObject modifiedObject = dirty
                .inverseProject(toTest.getAttributes())
                .concat(toTest);

            Set<String> newConditionals = new HashSet<>();

            newConditionals.addAll(conditionals);
            newConditionals.addAll(cover);

            Set<Set<String>> superSetCovers = new ConditionalSetCoverSearch(
                getRules(),
                newConditionals)
            .findMinimalCovers(modifiedObject, getNullBehavior());

            for(Set<String> ssc: superSetCovers)
            {
                try
                {
                    Pair<Integer, Set<DataObject>> sscRepairs = inspectCover(
                        modifiedObject,
                        ssc,
                        objectBag,
                        Math.min(costUpperBound, minimalRepairs.getFirst()),
                        superCoverBuffer.get(toTest), //Pass the cost payed so far
                        new HashSet<>(newConditionals),
                        localRules);

                    //For each repair found, add attribute already changed.
                    for(DataObject o: sscRepairs.getSecond())
                    {
                        for(String conditionalAttribute: newConditionals)
                            o.set(conditionalAttribute, modifiedObject.get(conditionalAttribute));
                    }

                    if(sscRepairs.getFirst() < minimalRepairs.getFirst())
                        minimalRepairs = sscRepairs;
                    else if(sscRepairs.getFirst().equals(minimalRepairs.getFirst()))
                        minimalRepairs.getSecond().addAll(sscRepairs.getSecond());
                }
                catch (RepairException ex)
                {
                    if(!ex.getMessage().startsWith("Repair encoutered a situation where no repairs are possible"))
                    {
                        throw ex;
                    }
                }
                catch (CPFException ex)
                {
                    if(!ex.getMessage().startsWith("Set of reduced CPFs features contradiction"))
                    {
                        throw ex;
                    }
                }
            }
        }
        
    
        return new Pair<>(
            minimalRepairs.getFirst(),
            minimalRepairs.getSecond().stream().map(dirty::concat).collect(Collectors.toSet())
        );
    }
    
    private long computeNonKeyCost(Multiset<DataObject> objectBag, DataObject candidate, Set<String> coverAttributes) throws RepairException
    {
        //If it is, we determine the additional costs for the non key parts
        long nKeyCost = 0L;

        //To compute that, we loop over all non key parts
        for(DataObject object: objectBag.keySet())
        {
            DataObject fullObject = new DataObject(object)
            .concat(candidate);

            //If this object has errors, it will require an additional cost
            //That cost = multiplicity * the cost of a minimal solution
            if(needsRepair(fullObject))
            {
                SubsetMinimalCoverSearch coverSearch = new SubsetMinimalCoverSearch(getRules());

                List<Set<String>> nMonotoneSetCovers = 
                coverSearch
                .findMinimalCovers(fullObject, getNullBehavior())
                .stream()
                .filter(sol -> sol.stream().noneMatch(a -> getCostModel().getPartialKey().getDetermined().contains(a)))
                .map(sol -> sol.stream().filter(a -> fullObject.get(a) != null).collect(Collectors.toSet()))
                .filter(sol -> !sol.isEmpty())        
                .sorted(Comparator.comparing(Set::size))
                .toList();

                int localNonKeyCost = nMonotoneSetCovers.isEmpty()
                    ? 0
                    : Integer.MAX_VALUE;

                for(Set<String> nCover: nMonotoneSetCovers)
                {
                    Multiset<DataObject> localBag = new Multiset<>();
                    localBag.add(object, objectBag.get(object));

                    Pair<Integer, Set<DataObject>> solutionBestRepairs = inspectCover(
                        fullObject,
                        nCover,
                        localBag,
                        localNonKeyCost,
                        0,
                        new HashSet<>(coverAttributes),
                        getRules());

                    if(solutionBestRepairs.getFirst() < localNonKeyCost)
                        localNonKeyCost = solutionBestRepairs.getFirst();
                }

                nKeyCost += localNonKeyCost;
            }
        }

        return nKeyCost;
    }
    
    private Set<String> offendingAttributes(DataObject o, SigmaRuleset localRules)
    {
        return localRules
            .failingRules(o)
            .stream()
            .flatMap(rule -> rule.getInvolvedAttributes().stream())
            .collect(Collectors.toSet());
    }
    
    private Map<String, Map<SigmaRule, List<SigmaRule>>> buildConcurrentRules()
    {        
        Map<String, Map<SigmaRule, List<SigmaRule>>> crMap = getRules()
            .getRules()
            .stream()
            .flatMap(rule -> rule.getInvolvedAttributes().stream())
            .distinct()
            .collect(Collectors.toMap(
                a -> a,
                a-> new HashMap<>()
            ));
        
        for(String attribute: crMap.keySet())
        {
            //Get relevant rules
            List<SigmaRule> rulesForAttribute = getRules()
            .getRules()
            .stream()
            .filter(rule -> rule.involves(attribute))
            .filter(rule -> rule.getVariableAtoms().isEmpty())
            .filter(rule -> rule.getInvolvedAttributes().size() > 1)
            .toList();
            
            for(SigmaRule ruleToStudy: rulesForAttribute)
            {
                List<SigmaRule> candidates = rulesForAttribute
                    .stream()
                    .filter(rule -> ruleToStudy
                        .getInvolvedAttributes()
                        .containsAll(rule.getInvolvedAttributes()))    
                    .filter(rule -> !rule.equals(ruleToStudy))
                    .filter(candidate -> candidate
                        .getConstantAtoms()
                        .stream()
                        .filter(atom -> !atom.getAttribute().equals(attribute))
                        .allMatch(atom -> CPFImplicator.atomIsImpliedByCPF(ruleToStudy.getCPF(), atom))
                    )
                    .filter(candidate -> ruleToStudy
                            .getConstantAtoms()
                            .stream()
                            .filter(atom -> !atom.getAttribute().equals(attribute))
                            .allMatch(atom -> CPFImplicator.atomIsImpliedByCPF(candidate.getCPF(), atom))
                    ).collect(Collectors.toList());

                if(!candidates.isEmpty())
                {
                    crMap.computeIfAbsent(attribute, k -> new HashMap<>());
                    crMap.get(attribute).put(ruleToStudy, candidates);
                }
            }
        }
        
        return crMap;
    }

    public RepairSelection getRepairSelection() {
        return repairSelection;
    }
}
