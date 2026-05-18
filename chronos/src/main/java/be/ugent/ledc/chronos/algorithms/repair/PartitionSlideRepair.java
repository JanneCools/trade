package be.ugent.ledc.chronos.algorithms.repair;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.VariableVarioAtom;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetOperations;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.selection.CPFRepairSelection;

import java.util.*;
import java.util.stream.Collectors;

public class PartitionSlideRepair<I extends Comparable<? super I>> {

    public static class PartitionData {
        final private Set<String> normAttributes;
        final private Set<SigmaRuleset> sigmaRulesets;
        final private SigmaRuleset mergedRuleset;

        public PartitionData(Set<String> normAttributes, SigmaRuleset mergedRuleset,
                             Set<SigmaRuleset> sigmaRulesets) {
            this.normAttributes = normAttributes;
            this.sigmaRulesets = sigmaRulesets;
            this.mergedRuleset = mergedRuleset;
        }

        public Set<String> getNormAttributes() {return this.normAttributes;}
        public SigmaRuleset getMergedRuleset() {return this.mergedRuleset;}
        public Set<SigmaRuleset> getSigmaRulesets() {return this.sigmaRulesets;}
    }

    Map<Set<String>, SlideRepair<?>> repairs = new HashMap<>();

    public PartitionSlideRepair(PartitionSlideRepair<I> repairEngine) {
        this.repairs = new HashMap<>(repairEngine.repairs);
    }

    public PartitionSlideRepair(SigmaRuleset ruleset, ConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        // create partitions based on the ruleset
        List<SigmaRuleset> sigmaPartitions = SigmaRulesetOperations.partition(ruleset).stream().toList();

        // merge partitions that have the CURR and NEXT of the same attribute
        Map<Integer, PartitionData> partitions = mergePartitions(sigmaPartitions);

        for (PartitionData partition : partitions.values()) {
            // invert the rulesets into CPFs and merge the sets of CPFs
            Iterator<SigmaRuleset> iterator = partition.getSigmaRulesets().iterator();
            Set<CPF> cpfs = iterator.hasNext() ? SigmaRulesetInverter.invert(iterator.next()) : new HashSet<>();
            while (iterator.hasNext()) {
                cpfs = mergeCPFSets(cpfs, SigmaRulesetInverter.invert(iterator.next()));
            }
            repairs.put(partition.getMergedRuleset().getContractors().keySet(),
                    new SlideRepair<>(cpfs, partition.getMergedRuleset(), costModel, nullBehavior, repairSelection));
        }
    }

    public PartitionSlideRepair(SigmaRuleset ruleset, NonConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection repairSelection) throws RepairException {
        // create partitions based on the ruleset
        List<SigmaRuleset> sigmaPartitions = SigmaRulesetOperations.partition(ruleset).stream().toList();

        // merge partitions that have the CURR and NEXT of the same attribute
        Map<Integer, PartitionData> partitions = mergePartitions(sigmaPartitions);

        for (PartitionData partition : partitions.values()) {
            // invert the rulesets into CPFs and merge the sets of CPFs
            Iterator<SigmaRuleset> iterator = partition.getSigmaRulesets().iterator();
            Set<CPF> cpfs = iterator.hasNext() ? SigmaRulesetInverter.invert(iterator.next()) : new HashSet<>();
            while (iterator.hasNext()) {
                cpfs = mergeCPFSets(cpfs, SigmaRulesetInverter.invert(iterator.next()));
            }
            repairs.put(partition.getMergedRuleset().getContractors().keySet(),
                    new SlideRepair<>(cpfs, partition.getMergedRuleset(), costModel, nullBehavior, repairSelection));
        }
    }

    public Pair<TemporalDataset<I>, Long> repair(TemporalDataset<I> dataset, Set<I> anchors) throws RepairException, ChronosException {
        TemporalDataset<I> repairedDataset = dataset;
        Long cost = 0L;
        for (SlideRepair slideRepair: repairs.values()) {
            Pair<TemporalDataset<I>, Long> bestSolution = slideRepair.repair(repairedDataset, anchors);
            repairedDataset =  bestSolution.getFirst();
            cost += bestSolution.getSecond();
        }
        return new Pair<>(repairedDataset, cost);
    }

    /**
     * Merge partitions that have the CURR and NEXT of the same attribute
     */
    public static Map<Integer, PartitionData> mergePartitions(
            List<SigmaRuleset> sigmaPartitions
    ) {
        int numPartitions = sigmaPartitions.size();

        Map<Integer, PartitionData> partitions = new HashMap<>();
        for (int i = 0; i < numPartitions; i++) {
            SigmaRuleset partition = sigmaPartitions.get(i);

            // get the normalized attributes of the partition
            Set<String> normAttributes = partition.getContractors().keySet().stream()
                    .map(PartitionSlideRepair::normalize)
                    .collect(Collectors.toSet());

            // verify whether the ruleset contains vario atoms
            boolean containsVario = partition.getRules().stream()
                    .anyMatch(rule -> rule.getAtoms().stream()
                            .anyMatch(atom -> atom instanceof VariableVarioAtom<?,?>));

            partitions.put(i, new PartitionData(
                    normAttributes, partition, new HashSet<>(Collections.singleton(partition)))
            );
        }

        // merge partitions that have the CURR and NEXT of the same attribute
        int i = 0;
        while (i < numPartitions) {
            if (partitions.containsKey(i)) {
                int currKey = i;
                PartitionData partition = partitions.get(i);
                Set<String> attributes = partition.getNormAttributes();

                // find partitions to merge with the current partition
                List<Integer> toMerge = partitions.keySet().stream()
                        .filter(key -> key > currKey && partitions.get(key).getNormAttributes().stream()
                                .anyMatch(attributes::contains))
                        .toList();

                if (!toMerge.isEmpty()) {
                    for (int key : toMerge) {
                        // merge the partitions
                        PartitionData partitionToMerge = partitions.get(key);

                        partition = new PartitionData(
                                SetOperations.union(partition.getNormAttributes(), partitionToMerge.getNormAttributes()),
                                partition.getMergedRuleset().merge(partitionToMerge.getMergedRuleset()),
                                SetOperations.union(partition.getSigmaRulesets(), partitionToMerge.getSigmaRulesets()));

                        // remove partition from map
                        partitions.remove(key);
                    }
                    partitions.put(currKey, partition);
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }

        return partitions;
    }

    private static Set<CPF> mergeCPFSets(Set<CPF> origSet, Set<CPF> newSet) {
        Set<CPF> mergedSet = new HashSet<>();

        for (CPF cpf1: origSet) {
            for (CPF cpf2: newSet) {
                CPF mergedCPF = new CPF(
                        SetOperations.union(cpf1.getAtoms(), cpf2.getAtoms()),
                        SetOperations.union(cpf1.getAttributes(), cpf2.getAttributes())
                );
                mergedSet.add(mergedCPF);
            }
        }
        return mergedSet;
    }

    public static Set<CPF> mergeCPFSets(Set<Set<CPF>> allCPFs) {
        Set<CPF> mergedSet = new HashSet<>();

        Iterator<Set<CPF>> iterator = allCPFs.iterator();
        if (iterator.hasNext()) {
            mergedSet = iterator.next();
        }
        while (iterator.hasNext()) {
            mergedSet = mergeCPFSets(mergedSet, iterator.next());
        }

        return mergedSet;
    }

    public static String normalize(String attribute) {
        if (attribute.endsWith(TRuleset.NEXT))
            return attribute.substring(0, attribute.length() - TRuleset.NEXT.length());
        else if (attribute.endsWith(TRuleset.CURR))
            return attribute.substring(0, attribute.length() - TRuleset.CURR.length());
        else
            return attribute;
    }
}
