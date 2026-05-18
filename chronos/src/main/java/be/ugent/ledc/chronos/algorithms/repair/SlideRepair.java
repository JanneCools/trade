package be.ugent.ledc.chronos.algorithms.repair;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.VariableVarioAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;
import be.ugent.ledc.sigma.repair.*;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.CPFRepairSelection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements a repair technique for a TemporalDataset where the rules
 * are a combination of (i) selection rules and (ii) transition rules.
 * Hereby, selection rules indicate conditions for consistency at each point in time,
 * whereas the transition rules indicate consistency between any two consecutive points in time.
 * <p>
 * The main idea of repair engine is to repair objects one be one. To that extent,
 * the repair method accepts an "anchor point", which is the time point from which
 * the repair starts. In a first step, the data at the anchor point is repaired
 * to be consistent against the selection rules. Next, the repair proceeds
 * with repairing the objects before and after the anchor points, while not altering the anchor
 * objects anymore. Because of this assumption, the transition rules can now be
 * 'materialized' by fixing the values of the anchor object. This procedure can be repeated
 * until all time points have been visited.
 * <p>
 * In this approach, we always repair one tuple at a time against a set of selection
 * rules. This set changes as transition rules are materialized to new conditions.
 * The repair of a tuple is done by an embedded repair engine.
 *
 * @param <I> The type of time index
 * @author abronsel
 */
public class SlideRepair<I extends Comparable<? super I>> {

    private static final int FORWARD    = 1;
    private static final int BACKWARD   = -1;

    private final Map<String, SigmaContractor<?>> contractors;

    /**
     * The set of attributes that do not have a cost function.
     * These attributes should not be repaired.
     */
    private final Set<String> noRepairAttributes;

    /**
     * The set of CPFs representing the (transition) constraints
     * This set does not contain vario atoms
     */
    private final Set<CPF> cpfs;

    /**
     * The set of sigma rules
     */
    private final Set<SigmaRule> rules;

    /**
     * A repair engine that is used during forward repair phase
     */
    private final CPFRepairEngine<?> forwardEngine;

    /**
     * A repair engine that is used during backward repair phase
     */
    private final CPFRepairEngine<?> backwardEngine;

    /**
     * A repair engine that is used during stationary repair phase
     */
    private final CPFRepairEngine<?> stationaryEngine;

    public SlideRepair(SigmaRuleset sigmaRuleset, ConstantCostModel costModel,
                       NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        this(null, sigmaRuleset, costModel, nullBehavior, repairSelection);
    }

    public SlideRepair(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, ConstantCostModel costModel,
                       NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        this.contractors = sigmaRuleset.getContractors();
        this.cpfs = cpfs;
        this.rules = null;

        // Verify equivalence/symmetry
        if(!new TRuleset<>(sigmaRuleset.getContractors(), sigmaRuleset.getRules()).isSymmetric())
            throw new RepairException("Cannot repair temporal data. "
                    + "Cause: set of transitions rules is not symmetric");

        // Store the attributes without a cost function
        this.noRepairAttributes = contractors.keySet().stream()
                .filter(attr -> !costModel.getCostFunctions().containsKey(attr))
                .collect(Collectors.toSet());

        // The forward engine keeps all rules, but rejects changes to current attributes
        ConstantCostModel forwardModel = new ConstantCostModel(costModel
                .getCostFunctions()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(TRuleset.NEXT))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue))
        );

        this.forwardEngine = new ConstantCPFRepairEngine(cpfs, sigmaRuleset, forwardModel, nullBehavior, repairSelection);

        // The backward engine keeps all rules, but rejects changes to next attributes
        ConstantCostModel backwardModel = new ConstantCostModel(costModel
                .getCostFunctions()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(TRuleset.CURR))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue))
        );

        this.backwardEngine = new ConstantCPFRepairEngine(cpfs, sigmaRuleset, backwardModel, nullBehavior, repairSelection);

        // The stationary engine keeps only current rules
        SigmaRuleset stationaryRules = sigmaRuleset.project(sigmaRuleset
                .stream()
                .flatMap(rule -> rule.getInvolvedAttributes().stream())
                .filter(att -> att.endsWith(TRuleset.CURR))
                .collect(Collectors.toSet())
        );

        this.stationaryEngine = new ConstantCPFRepairEngine(stationaryRules, backwardModel, nullBehavior, repairSelection);
    }

    public SlideRepair(SigmaRuleset sigmaRuleset, NonConstantCostModel costModel,
                       NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        this(null, sigmaRuleset, costModel, nullBehavior, repairSelection);
    }

    public SlideRepair(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, NonConstantCostModel costModel,
                       NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        this.contractors = sigmaRuleset.getContractors();
        this.cpfs = cpfs;
        this.rules = null;

        // Verify equivalence/symmetry
        if(!new TRuleset<>(sigmaRuleset.getContractors(), sigmaRuleset.getRules()).isSymmetric())
            throw new RepairException("Cannot repair temporal data. "
                    + "Cause: set of transitions rules is not symmetric");

        // Store the attributes without a cost function
        this.noRepairAttributes = contractors.keySet().stream()
                .filter(attr -> !costModel.getCostFunctions().containsKey(attr))
                .collect(Collectors.toSet());

        // The forward engine keeps all rules, but rejects changes to current attributes
        NonConstantCostModel forwardModel = new NonConstantCostModel(costModel
                .getCostFunctions()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(TRuleset.NEXT))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue)),
                costModel.getBoundings());

        this.forwardEngine = new NonConstantCPFRepairEngine(cpfs, sigmaRuleset, forwardModel, nullBehavior, repairSelection);

        // The backward engine keeps all rules, but rejects changes to next attributes
        NonConstantCostModel backwardModel = new NonConstantCostModel(costModel
                .getCostFunctions()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().endsWith(TRuleset.CURR))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue)),
                costModel.getBoundings());

        this.backwardEngine = new NonConstantCPFRepairEngine(cpfs, sigmaRuleset, backwardModel, nullBehavior, repairSelection);

        // The stationary engine keeps only current rules
        SigmaRuleset stationaryRules = sigmaRuleset.project(sigmaRuleset
                .stream()
                .flatMap(rule -> rule.getInvolvedAttributes().stream())
                .filter(att -> att.endsWith(TRuleset.CURR))
                .collect(Collectors.toSet())
        );

        this.stationaryEngine = new NonConstantCPFRepairEngine(stationaryRules, backwardModel, nullBehavior, repairSelection);
    }

    public  Map<String, SigmaContractor<?>> getContractors() {
        return contractors;
    }

    public  SigmaContractor<?> getContractor(String attribute) {
        return contractors.get(attribute);
    }

    public Pair<TemporalDataset<I>, Long> repair(TemporalDataset<I> dataset, Set<I> anchors) throws RepairException, ChronosException {

        Long bestCost = Long.MAX_VALUE;
        TemporalDataset<I> bestRepair = null;

        for (I anchor : anchors) {

            Pair<TemporalDataset<I>, Long> anchorRepair = repair(dataset, anchor, bestCost);

            if (anchorRepair.getSecond() < bestCost)
            {
                bestCost = anchorRepair.getSecond();
                bestRepair = anchorRepair.getFirst();
            }

        }

        return new Pair<>(bestRepair, bestCost);

    }

    private Pair<TemporalDataset<I>, Long> repair(TemporalDataset<I> dataset, I anchor, Long bestCost) throws RepairException, ChronosException {

        // Fetch the signal representation of this dataset
        Signal<I, DataObject> objectSignal = dataset.getObjectSignal();

        Signal<I, DataObject> repairSignal = new Signal<>(dataset.indexContractor());

        Long repairCost = 0L;

        // Sanity check: the anchor point must appear in the dataset
        if(!objectSignal.hasValueAt(anchor))
            throw new RepairException("Cannot repair temporal dataset. Cause: anchor "
                    + anchor
                    + " does not appear in the dataset.");

        // Step 1: initialization
        Pair<DataObject, Integer> stationaryRepair = repairStationary(objectSignal.valueAt(anchor));

        repairSignal.put(anchor, stationaryRepair.getFirst());
        repairCost += stationaryRepair.getSecond();

        if (repairCost > bestCost) {
            return new Pair<>(null, bestCost);
        }

        // Step 2: forward repair phase
        I forwardPointer = objectSignal.nextIndex(anchor);

        while (forwardPointer != null) {

            I repairPointer = objectSignal.previousIndex(forwardPointer);

            Pair<DataObject, Integer> transitionRepair = repairTransition(
                    objectSignal.valueAt(forwardPointer),
                    repairSignal.valueAt(repairPointer),
                    FORWARD
            );

            // Register
            repairSignal.put(forwardPointer, transitionRepair.getFirst());
            repairCost += transitionRepair.getSecond();

            if (repairCost > bestCost) {
                return new Pair<>(null, bestCost);
            }

            forwardPointer = objectSignal.nextIndex(forwardPointer);

        }

        // Step 2: backward repair phase
        I backwardPointer = objectSignal.previousIndex(anchor);

        while(backwardPointer != null) {

            I repairPointer = objectSignal.nextIndex(backwardPointer);

            Pair<DataObject, Integer> transitionRepair = repairTransition(
                    objectSignal.valueAt(backwardPointer),
                    repairSignal.valueAt(repairPointer),
                    BACKWARD
            );

            //Register
            repairSignal.put(backwardPointer, transitionRepair.getFirst());
            repairCost += transitionRepair.getSecond();

            if (repairCost > bestCost) {
                return new Pair<>(null, bestCost);
            }

            backwardPointer = objectSignal.previousIndex(backwardPointer);
        }

        ContractedDataset repair = new ContractedDataset(dataset.getContract());

        repairSignal
                .indexStream()
                .forEach(idx -> repair.addDataObject(repairSignal.valueAt(idx)));

        return new Pair<>(
                TemporalDataset.create(
                        repair,
                        dataset.getTimeAttribute(),
                        dataset.indexContractor()
                ),
                repairCost);

    }

    private Pair<DataObject,Integer> repairStationary(DataObject dataObject) throws RepairException {

        DataObject converted = new DataObject();

        for(String at: dataObject.getAttributes()) {
            converted.set(at.concat(TRuleset.CURR), dataObject.get(at));
        }

        DataObject o = applyRuleContracts(converted);

        if (stationaryEngine.getCpfs().stream().anyMatch(cpf -> cpf.isSatisfied(o))) {
            return new Pair<>(TRuleset.projectOverCurrent(converted), 0);
        } else {

            DataObject repair = stationaryEngine.repair(o);
            int cost = stationaryEngine.getCostModel().cost(converted, repair);
            repair = TRuleset.projectOverCurrent(repair);

            return new Pair<>(repair, cost);

        }

    }

    private Pair<DataObject,Integer> repairTransition(DataObject dataObject, DataObject anchor, int direction) throws RepairException {

        DataObject o = direction == FORWARD
                ? applyRuleContracts(TRuleset.createObject(anchor, dataObject, dataObject.getAttributes()))
                : applyRuleContracts(TRuleset.createObject(dataObject, anchor, dataObject.getAttributes()));

        // there are no rules with vario atoms so do normal repair
        boolean clean = cpfs.stream().anyMatch(cpf -> cpf.isSatisfied(o));

        if (clean) {
            return direction == FORWARD
                    ? new Pair<>(TRuleset.projectOverNext(o), 0)
                    : new Pair<>(TRuleset.projectOverCurrent(o), 0);
        }

        Pair<Integer, DataObject> repair = direction == FORWARD
                ? forwardEngine.repairWithCost(o)
                : backwardEngine.repairWithCost(o);

        return direction == FORWARD
                ? new Pair<>(TRuleset.projectOverNext(repair.getSecond()), repair.getFirst())
                : new Pair<>(TRuleset.projectOverCurrent(repair.getSecond()), repair.getFirst());
    }

    private <T extends Comparable<? super T>> DataObject applyRuleContracts(DataObject dataObject) {

        for (String attribute : dataObject.getAttributes()) {
            if (contractors.containsKey(attribute)) {
                SigmaContractor<T> contractor = (SigmaContractor<T>) getContractor(attribute);
                dataObject.set(attribute, contractor.get((T) dataObject.get(attribute)));
            }
        }

        return dataObject;

    }

}
