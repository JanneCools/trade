package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.AtomOperations;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractException;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;

import java.util.*;
import java.util.stream.Collectors;

public class CPFRandomRepairSelection implements CPFRepairSelection {

    private final static long CAP = 100L;

    @Override
    public <T extends Comparable<? super T>> DataObject selectRepair(Set<CPF> minCostChangeExpressions, DataObject originalObject) {

        CPF selectedCPF = selectRandomCPF(minCostChangeExpressions);

        Set<String> repairAttributes = selectedCPF.getAttributes();
        Set<String> otherAttributes = originalObject.getAttributes().stream().filter(at -> !repairAttributes.contains(at)).collect(Collectors.toSet());

        DataObject repairObject = originalObject.project(otherAttributes);

        for (String attribute : repairAttributes) {
            selectedCPF = CPFImplicator.imply(selectedCPF.fix(repairObject));
            T value = selectRandomValue(selectedCPF, attribute);
            repairObject.set(attribute, value);
        }

        return repairObject;

    }

    private CPF selectRandomCPF(Set<CPF> cpfs) {

        if (cpfs == null || cpfs.isEmpty()) {
            throw new IllegalArgumentException("The set of CPFs cannot be empty.");
        }

        List<CPF> cpfsList = new ArrayList<>(cpfs);

        int randomIndex = new Random().nextInt(cpfsList.size());
        return cpfsList.get(randomIndex);

    }

    private <T extends Comparable<? super T>> T selectRandomValue(CPF cpf, String attribute) {

        if (cpf.getContractor(attribute) instanceof OrdinalContractor) {

            OrdinalContractor<T> contractor = (OrdinalContractor<T>) cpf.getContractor(attribute);

            Set<Interval<T>> intervals = AtomOperations.getIntervalsFromOrdinalAtoms(cpf.getAtoms(), attribute);

            if (intervals == null || intervals.isEmpty()) {
                throw new IllegalArgumentException("The set of intervals " + intervals + " cannot be empty in order to select a random value from it.");
            }

            List<Interval<T>> intervalsList = new ArrayList<>(intervals);

            int randomIntervalIndex = new Random().nextInt(intervalsList.size());
            Interval<T> selectedInterval = intervalsList.get(randomIntervalIndex);

            long intervalCardinality;

            try {
                intervalCardinality = contractor.cardinality(selectedInterval);
            } catch (SigmaContractException ex) {
                intervalCardinality = Long.MAX_VALUE;
            }

            T low = selectedInterval.getLeftBound();

            if (selectedInterval.isLeftOpen()) {
                if (selectedInterval.getLeftBound() == null) {
                    low = contractor.first();
                } else {
                    low = contractor.next(selectedInterval.getLeftBound());
                }
            }

            if (intervalCardinality > CAP) {

                Set<T> values = new HashSet<>();

                for (int i = 0; i < CAP; i++) {
                    long randomValueIndex = new Random().nextLong(intervalCardinality);
                    values.add(contractor.add(low, randomValueIndex));
                }

                return new ArrayList<>(values).get(new Random().nextInt(values.size()));

            } else {
                long randomValueIndex = new Random().nextLong(intervalCardinality);
                return contractor.add(low, randomValueIndex);
            }

        } else {

            Set<T> valueset = AtomOperations.getValuesetFromNominalAtoms(cpf.getAtoms(), attribute);

            if (valueset.isEmpty()) {
                throw new IllegalArgumentException("The value set " + valueset + " cannot be empty in order to select a random value from it.");
            }

            List<T> valuelist = new ArrayList<>(valueset);

            int randomIntervalIndex = new Random().nextInt(valuelist.size());
            return valuelist.get(randomIntervalIndex);

        }

    }


}
