package be.ugent.ledc.sigma.repair.cost;

import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.datastructures.Pair;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record CostMapping<T extends Comparable<? super T>>(Map<T, TreeMap<Integer, Set<T>>> costMap) {

    public CostMapping {

        Set<Integer> costs = costMap.values().stream().flatMap(tm -> tm.keySet().stream()).collect(Collectors.toSet());

        if (costs.stream().anyMatch(c -> c < 0)) {
            throw new RepairRuntimeException("Cost functions must be positive definite. Found: " + costMap);
        }

        for (T originalValue : costMap.keySet()) {

            TreeMap<Integer, Set<T>> currentCostMap = costMap.get(originalValue);
            if (currentCostMap.entrySet().stream().anyMatch(e -> e.getKey() > 0 && e.getValue().contains(originalValue))) {
                throw new RepairRuntimeException("Cost functions must be positive definite. Found original value " + originalValue + " and " + costMap);
            }

        }

    }

    public TreeMap<Integer, Set<T>> getCostMapForOriginalValue(T originalValue) {
        return costMap.get(originalValue);
    }

    public Pair<Integer, Set<T>> getMinCostEntryForOriginalValue(T originalValue) {
        TreeMap<Integer, Set<T>> costMapForOriginalValue = costMap.get(originalValue);
        Map.Entry<Integer, Set<T>> minCostEntryForOriginalValue = costMapForOriginalValue.firstEntry();
        return new Pair<>(minCostEntryForOriginalValue.getKey(), minCostEntryForOriginalValue.getValue());
    }

    public int getMinCostForOriginalValue(T originalValue) {
        return getMinCostEntryForOriginalValue(originalValue).getFirst();
    }

    public Set<T> getMinCostValuesForOriginalValue(T originalValue) {
        return getMinCostEntryForOriginalValue(originalValue).getSecond();
    }

}
