package sigma.repair.cost.functions;

import core.cost.CostFunction;
import core.dataset.DataObject;
import core.datastructures.Interval;
import sigma.datastructures.contracts.OrdinalContractor;
import sigma.datastructures.contracts.SigmaContractor;

import java.util.Map;

public class PartitionTargetCostFunction <T extends Comparable<? super T>, C extends OrdinalContractor<T>,
        U extends Comparable<? super U>, D extends SigmaContractor<U>>
        implements CostFunction<T> {

    private final C contractor;

    private final String partitionAttribute;

    private final D partitionContractor;

    private final Map<U, T> partitionsTargets;

    public PartitionTargetCostFunction(C contractor,  D partitionContractor, String partitionAttribute,  Map<U, T> partitionsTargets) {
        this.contractor = contractor;
        this.partitionAttribute = partitionAttribute;
        this.partitionContractor = partitionContractor;
        this.partitionsTargets = partitionsTargets;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if(repairedValue == null)
            return Integer.MAX_VALUE;

        T target = partitionsTargets.get(partitionContractor.getFromDataObject(originalObject, partitionAttribute));
        if (target == null) return 1;
        long distanceToTarget = target.compareTo(repairedValue) <= 0
                ? contractor.cardinality(new Interval<>(target, repairedValue)) + 1
                : contractor.cardinality(new Interval<>(repairedValue, target)) + 1;

        if (distanceToTarget <= 0) return Integer.MAX_VALUE;

        return (int)distanceToTarget;
    }
}
