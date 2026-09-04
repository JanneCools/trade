package be.ugent.ledc.chronos.algorithms.repair;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.CPFRandomRepairSelection;

import java.util.*;
import java.util.function.BiFunction;

public class TimeSeriesRepair<I extends Comparable<? super I>> {

    private final Map<String, TemporalDataset<I>> partitionedDatasets;
    private PartitionSlideRepair<I> repairEngine;

    public TimeSeriesRepair(
            Map<String, TemporalDataset<I>> partitionedDatasets
    ) {
        this.partitionedDatasets = new HashMap<>(partitionedDatasets);
    }

    public TimeSeriesRepair(TimeSeriesRepair<I> other) {
        this.partitionedDatasets = new HashMap<>(other.partitionedDatasets);
        this.repairEngine = new PartitionSlideRepair<>(other.repairEngine);
    }

    public void initialize(
            ConstantCostModel costModel, TRuleset<I> ruleset, NullBehavior nullBehavior, boolean earlyStop
    ) throws RepairException {
        this.repairEngine = new PartitionSlideRepair<>(
                ruleset,
                costModel,
                nullBehavior,
                new CPFRandomRepairSelection(),
                earlyStop
        );
    }

    public void initialize(
            NonConstantCostModel costModel, TRuleset<I> ruleset, NullBehavior nullBehavior, boolean earlyStop
    ) throws RepairException {
        this.repairEngine = new PartitionSlideRepair<>(
                ruleset,
                costModel,
                nullBehavior,
                new CPFRandomRepairSelection(),
                earlyStop
        );
    }

    public double repair(int numAnchors, Map<String, Pair<TemporalDataset<I>, Long>> result) throws RepairException, ChronosException {

        // for each time series (partitions by keys), determine the anchors and repair the time series
        List<String> keys = partitionedDatasets.keySet().stream().toList();
        double duration = 0.0;
        int i = 0;
        int divider = keys.size() < 10 ? 1 : keys.size() / 10;
        for (String key : keys) {
            if (i % divider == 0) {
                System.out.println("i: " + i + " (size: " + partitionedDatasets.get(key).size() + ")");
            }
            i++;
            I curr = partitionedDatasets.get(key).start();
            Set<I> anchors = new HashSet<>();
            // add start of time series
            anchors.add(curr);
            if (numAnchors > 1) {
                // add uniform points in time series
                int size = partitionedDatasets.get(key).size();
                int jumpSize = numAnchors >= size ? 1 : (size-1) / (numAnchors-1);
                for (int j = 0; j < numAnchors-2 && j < size; j++) {
                    for (int k = 0; k < jumpSize; k++) {
                        curr = partitionedDatasets.get(key).getObjectSignal().nextIndex(curr);
                    }
                    if (curr != null) anchors.add(curr);
                }
                // add end of time series
                anchors.add(partitionedDatasets.get(key).end());
            }

            long start = System.currentTimeMillis();
            Pair<TemporalDataset<I>, Long> repair = repairEngine.repair(partitionedDatasets.get(key), anchors);
            long stop = System.currentTimeMillis();
            duration += (stop - start)/1000.0;

            result.put(key, repair);
        }

        return duration;
    }

    public double repair(
            int numAnchors, Set<String> considerAttributes,
            BiFunction<Pair<String, Integer>, Set<String>, Boolean> validator,
            Map<String, Pair<TemporalDataset<I>, Long>> result
    ) throws RepairException, ChronosException {

        List<String> keys = partitionedDatasets.keySet().stream().toList();
        double duration = 0.0;
        int i = 0;
        int divider = keys.size() < 10 ? 1 : keys.size() / 10;
        for (String key : keys) {
            if (i % divider == 0) {
                System.out.println("i: " + i + " (size: " + partitionedDatasets.get(key).size() + ")");
            }
            i++;
            I curr = partitionedDatasets.get(key).start();
            int index = 1;
            Set<I> anchors = new HashSet<>();
            // add start of time series
            anchors.add(curr);
            if (numAnchors > 1) {
                // add uniform points in time series
                int size = partitionedDatasets.get(key).size();
                int jumpSize = numAnchors >= size ? 1 : (size - 1) / (numAnchors - 1);
                for (int j = 0; j < numAnchors - 2 && j < size; j++) {
                    for (int k = 0; k < jumpSize; k++) {
                        curr = partitionedDatasets.get(key).getObjectSignal().nextIndex(curr);
                        index++;
                    }
                    if (!considerAttributes.isEmpty()) {
                        // search for timestamp without errors
                        int tempIndex = index;
                        I temp = curr;
                        while (validator.apply(new Pair<>(key, tempIndex), considerAttributes)) {
                            temp = partitionedDatasets.get(key).getObjectSignal().previousIndex(temp);
                            tempIndex--;
                        }
                        anchors.add(temp);
                    } else {
                        if (curr != null) anchors.add(curr);
                    }
                }
                // add end of time series
                index = partitionedDatasets.get(key).size();
                curr = partitionedDatasets.get(key).end();
                if (!considerAttributes.isEmpty()) {
                    // search for timestamp without errors
                    int tempIndex = index;
                    I temp = curr;
                    while (validator.apply(new Pair<>(key, tempIndex), considerAttributes)) {
                        temp = partitionedDatasets.get(key).getObjectSignal().previousIndex(temp);
                        tempIndex--;
                    }
                    anchors.add(temp);
                } else {
                    anchors.add(curr);
                }
            }

            long start = System.currentTimeMillis();
            Pair<TemporalDataset<I>, Long> repair = repairEngine.repair(partitionedDatasets.get(key), anchors);
            long stop = System.currentTimeMillis();
            duration += (stop - start) / 1000.0;

            result.put(key, repair);
        }

        return duration;
    }



}
