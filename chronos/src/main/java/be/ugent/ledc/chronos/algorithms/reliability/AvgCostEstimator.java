package be.ugent.ledc.chronos.algorithms.reliability;

import be.ugent.ledc.chronos.datastructures.TimePoint;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;
import be.ugent.ledc.sigma.repair.NonConstantCPFRepairEngine;
import be.ugent.ledc.sigma.repair.bounding.Bounding;
import be.ugent.ledc.sigma.repair.bounding.RangedBounding;
//import be.ugent.ledc.sigma.repair.changeexpressions.NonConstantCostCESearch;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;

import java.time.temporal.Temporal;
import java.util.*;

public class AvgCostEstimator<T extends Temporal> implements RefPointReliabilityEstimator<T> {

    public static final String CUR  = "#cur";
    public static final String REF = "#ref";

    private final Set<CPF> cpfs;
    private final Map<String, SigmaContractor<?>> contractors;
    private final NonConstantCPFRepairEngine engine;

    public AvgCostEstimator(SigmaRuleset sigmaRuleset, Map<String, IterableCostFunction<?>> costFunctions, Map<String, RangedBounding<?>> boundings) throws RepairException {
        this(SigmaRulesetInverter.invert(sigmaRuleset), sigmaRuleset.getContractors(), costFunctions, boundings);
    }

    public AvgCostEstimator(Set<CPF> cpfs, Map<String, SigmaContractor<?>> contractors, Map<String, IterableCostFunction<?>> costFunctions, Map<String, RangedBounding<?>> boundings) throws RepairException {

        this.cpfs = cpfs;
        this.contractors = contractors;

        NonConstantCostModel costModel = initializeCostModel(costFunctions, boundings);

        this.engine = new NonConstantCPFRepairEngine(cpfs, null, costModel);
    }

    private NonConstantCostModel initializeCostModel(Map<String, IterableCostFunction<?>> costFunctions, Map<String, RangedBounding<?>> boundings) {

        Map<String, IterableCostFunction<?>> estimatorCostFunctions = new HashMap<>();

        for (String attribute : costFunctions.keySet()) {
            estimatorCostFunctions.put(attribute.concat(CUR), costFunctions.get(attribute));
            estimatorCostFunctions.put(attribute.concat(REF), costFunctions.get(attribute));
        }

        Map<String, Bounding<?,?,?>> estimatorBoundings = new HashMap<>();

        for (String attribute : boundings.keySet()) {
            estimatorBoundings.put(attribute.concat(CUR), boundings.get(attribute));
            estimatorBoundings.put(attribute.concat(REF), boundings.get(attribute));
        }

        return new NonConstantCostModel(estimatorCostFunctions, estimatorBoundings);

    }

    public Set<CPF> getCPFs() {
        return cpfs;
    }

    public  Map<String, SigmaContractor<?>> getContractors() {
        return contractors;
    }

    public  SigmaContractor<?> getContractor(String attribute) {
        return contractors.get(attribute);
    }

    public NonConstantCPFRepairEngine getEngine() {
        return engine;
    }

    @Override
    public double getReliabilityScore(TimePoint<T> currentPoint, Set<TimePoint<T>> refPoints) throws RepairException {

        List<Integer> changeCosts = new ArrayList<>();

        for (TimePoint<T> refPoint : refPoints) {

            DataObject linkedPoints = applyRuleContracts(linkCurrentWithReference(currentPoint, refPoint));

            int changeCost = engine.getMinCost(linkedPoints);
            changeCosts.add(changeCost);

        }

        return changeCosts.stream().mapToDouble(c -> c).average().orElse(0.0);

    }

    private DataObject linkCurrentWithReference(TimePoint<T> cur, TimePoint<T> ref) {

        DataObject linkedObject = new DataObject();

        linkedObject.set("index#cur", cur.getIndex());
        linkedObject.set("index#ref", ref.getIndex());

        for (String at : cur.getDataObject().getAttributes()) {
            linkedObject.set(at.concat(CUR), cur.getDataObject().get(at));
            linkedObject.set(at.concat(REF), ref.getDataObject().get(at));
        }

        return linkedObject;

    }

    private <C extends Comparable<? super C>> DataObject applyRuleContracts(DataObject dataObject) {

        for (String attribute : dataObject.getAttributes()) {
            if (contractors.containsKey(attribute)) {
                SigmaContractor<C> contractor = (SigmaContractor<C>) getContractor(attribute);
                dataObject.set(attribute, contractor.get((C) dataObject.get(attribute)));
            }
        }

        return dataObject;

    }

}
