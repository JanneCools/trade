package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.DonorSelector;

import java.util.*;

public class JointFallbackRepair extends JointRepair {

    private final SequentialRepair fallback;

    public JointFallbackRepair(SequentialRepair fallback, DonorSelector selector) {
        super(selector, fallback.getRules(), fallback.getCostModel(), fallback.getNullBehavior());
        this.fallback = fallback;
    }

    public JointFallbackRepair(SequentialRepair fallback, Dataset cleanData) throws RepairException {
        super(fallback.getRules(), fallback.getCostModel(), fallback.getNullBehavior(), cleanData);
        this.fallback = fallback;
    }

    public JointFallbackRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior, Dataset cleanData) throws RepairException {
        super(rules, costModel, nullBehavior, cleanData);
        this.fallback = new SequentialRepair(rules, costModel, nullBehavior, cleanData);
    }

    public JointFallbackRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, Dataset cleanData) throws RepairException {
        super(rules, costModel, cleanData);
        this.fallback = new SequentialRepair(rules, costModel, cleanData);
    }

    public JointFallbackRepair(SCCSigmaRuleset rules, Dataset cleanData) throws RepairException {
        super(rules, cleanData);
        this.fallback = new SequentialRepair(rules, cleanData);
    }

    @Override
    public DataObject repair(DataObject o, Set<String> solution) throws RepairException {

        return super.repair(o, solution);
    }

}
