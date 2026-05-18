package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.atoms.*;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;
import be.ugent.ledc.sigma.repair.cost.CostMapping;
import be.ugent.ledc.sigma.repair.selection.CPFRepairSelection;

import java.util.*;
import java.util.stream.Collectors;

public class NonConstantCPFRepairEngine extends CPFRepairEngine<NonConstantCostModel>
{

    public NonConstantCPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, NonConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException
    {
        super(cpfs, sigmaRuleset, costModel, nullBehavior, selector);
    }

    public NonConstantCPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, NonConstantCostModel costModel) throws RepairException
    {
        super(cpfs, sigmaRuleset, costModel);
    }

    public NonConstantCPFRepairEngine(SigmaRuleset sigmaRuleset, NonConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException
    {
        super(sigmaRuleset, costModel, nullBehavior, selector);
    }

    public NonConstantCPFRepairEngine(SigmaRuleset sigmaRuleset, NonConstantCostModel costModel) throws RepairException
    {
        super(sigmaRuleset, costModel);
    }

    @Override
    public Pair<Integer, Set<CPF>> getMinCostChangeExpressionsWithCost(DataObject dataObject) throws RepairException
    {
        //Search change expressions
        Map<CPF, Set<CPF>> changeExpressionsMap = search
        (
            dataObject,
            AttributesToKeep.MAXIMAL_SETS
        );

        Set<CPF> changeExpressions = changeExpressionsMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        changeExpressions.removeIf(ce -> ce.getAttributes().stream().anyMatch(at -> getCostModel().getCostFunction(at) == null));
        changeExpressions.removeIf(ce1 -> changeExpressions.stream().anyMatch(ce2 -> !ce1.equals(ce2) && ce1.implies(ce2)));

        // data object is clean
        if (changeExpressions.isEmpty())
            return new Pair<>(0, new HashSet<>());

        Map<String, Set<Set<AbstractAtom<?, ?, ?>>>> boundingAtomsets = getCostModel()
            .getBoundingAtomsets(
                dataObject,
                getRules().getContractors()
            );

        int minCost = Integer.MAX_VALUE;
        Set<CPF> minCostChangeExpressions = new HashSet<>();

        Set<CPF> variableCEs = changeExpressions.stream().filter(CPF::hasVariableAtoms).collect(Collectors.toSet());
        Set<CPF> nonVariableCEs = changeExpressions.stream().filter(ce -> !variableCEs.contains(ce)).collect(Collectors.toSet());

        for (CPF changeExpression : nonVariableCEs) {
            minCost = inspectChangeExpression(changeExpression, dataObject, boundingAtomsets, minCost, minCostChangeExpressions);
        }

        for (CPF changeExpression : variableCEs) {
            minCost = inspectChangeExpression(changeExpression, dataObject, boundingAtomsets, minCost, minCostChangeExpressions);
        }

        if (minCostChangeExpressions.isEmpty()) {
            throw new RepairException("Could not find a minimal-cost change expression because of the current bounding settings. " +
                    "Manual inspection of the dirty data object " + dataObject + " is recommended.");
        }

        minCostChangeExpressions.removeIf(mcce1 -> minCostChangeExpressions.stream().anyMatch(mcce2 -> !mcce1.equals(mcce2) && mcce1.implies(mcce2)));

        return new Pair<>(minCost, minCostChangeExpressions);
    }

    private <T extends Comparable<? super T>> int inspectChangeExpression(CPF changeExpression, DataObject dataObject, Map<String, Set<Set<AbstractAtom<?, ?, ?>>>> boundingAtomsets, int globalMinCost, Set<CPF> minCostChangeExpressions) throws RepairException {

        Set<CPF> boundedChangeExpressions = boundChangeExpressions(changeExpression, boundingAtomsets);

        for (CPF boundedChangeExpression : boundedChangeExpressions) {

            Map<String, CostMapping<?>> inducedCostMappings = getCostModel().getInducedCostMappings(boundedChangeExpression, dataObject);
            Set<Set<String>> attributeSets = getAttributeSets(boundedChangeExpression);

            int currentCost = 0;
            Map<Set<String>, Set<Set<AbstractAtom<?, ?, ?>>>> ceAtomsets = new HashMap<>();

            for (Set<String> attributeSet : attributeSets) {

                if (attributeSet.size() == 1) {

                    String attribute = attributeSet.stream().findFirst().get();

                    CostMapping<T> costMappingForAttribute = (CostMapping<T>) inducedCostMappings.get(attribute);
                    int cost = costMappingForAttribute.getMinCostForOriginalValue((T) dataObject.get(attribute));
                    currentCost = cost == Integer.MAX_VALUE ? Integer.MAX_VALUE : currentCost + cost;

                    if (currentCost > globalMinCost) {
                        break;
                    }

                    Set<T> minCostValuesForAttribute = costMappingForAttribute.getMinCostValuesForOriginalValue((T) dataObject.get(attribute));
                    AbstractAtom<?, ?, ?> atom = buildAtomFromValueSet((SigmaContractor<T>) boundedChangeExpression.getContractor(attribute), attribute, minCostValuesForAttribute);

                    ceAtomsets.put(attributeSet, Set.of(Set.of(atom)));

                } else {
                    Pair<Integer, Set<Set<AbstractAtom<?, ?, ?>>>> costAtomsetPair = inspectAttributeSet(attributeSet, inducedCostMappings, dataObject, boundedChangeExpression, globalMinCost);
                    int cost = costAtomsetPair.getFirst();
                    currentCost = cost == Integer.MAX_VALUE ? Integer.MAX_VALUE : currentCost + cost;

                    if (currentCost > globalMinCost) {
                        break;
                    }

                    ceAtomsets.put(attributeSet, costAtomsetPair.getSecond());
                }

            }

            if (currentCost < globalMinCost) {
                Set<CPF> changeExpressions = combineCEAtomsets(ceAtomsets);
                minCostChangeExpressions.clear();
                minCostChangeExpressions.addAll(changeExpressions);
                globalMinCost = currentCost;
            } else if (globalMinCost == currentCost) {
                Set<CPF> changeExpressions = combineCEAtomsets(ceAtomsets);
                minCostChangeExpressions.addAll(changeExpressions);
            }

        }

        return globalMinCost;

    }

    private Set<CPF> boundChangeExpressions(CPF changeExpression, Map<String, Set<Set<AbstractAtom<?, ?, ?>>>> boundingAtomsets)
    {

        Set<CPF> boundingCEs = new HashSet<>(Collections.singleton(changeExpression));

        for (String attribute : changeExpression.getAttributes())
        {

            Set<CPF> newCEs = new HashSet<>();

            if (boundingAtomsets.containsKey(attribute)) {

                for (Set<AbstractAtom<?, ?, ?>> atomset : boundingAtomsets.get(attribute)) {

                    for (CPF currentCE : boundingCEs) {

                        Set<AbstractAtom<?, ?, ?>> currentCEAtoms = new HashSet<>(currentCE.getAtoms());
                        currentCEAtoms.addAll(atomset);

                        CPF newCE = CPFImplicator.imply(new CPF(currentCEAtoms));

                        if (!newCE.isContradiction()) {
                            newCEs.add(newCE);
                        }

                    }
                }
            } else {
                newCEs.add(changeExpression);
            }

            boundingCEs = newCEs;

        }

        return boundingCEs;
    }

    private Set<Set<String>> getAttributeSets(CPF cpf)
    {

        Set<Set<String>> attributeSets = cpf.getAttributes().stream().map(at -> new HashSet<>(Set.of(at))).collect(Collectors.toSet());

        Set<VariableAtom<?, ?, ?>> atoms = cpf.getVariableAtoms();

        for (VariableAtom<?, ?, ?> atom : atoms) {

            Set<String> leftAttributeSet = attributeSets.stream().filter(as -> as.contains(atom.getLeftAttribute())).findFirst().get();
            Set<String> rightAttributeSet = attributeSets.stream().filter(as -> as.contains(atom.getRightAttribute())).findFirst().get();

            attributeSets.remove(leftAttributeSet);
            attributeSets.remove(rightAttributeSet);
            leftAttributeSet.addAll(rightAttributeSet);
            attributeSets.add(leftAttributeSet);

        }

        return attributeSets;
    }

    private <T extends Comparable<? super T>> Pair<Integer, Set<Set<AbstractAtom<?, ?, ?>>>> inspectAttributeSet(Set<String> attributes, Map<String, CostMapping<?>> costMappings, DataObject dataObject, CPF cpf, int globalMinCost)
    {

        Map<String, List<Pair<Integer, Set<T>>>> costMappingsForAttributes = new HashMap<>();

        for (String attribute : attributes) {
            CostMapping<T> costMappingForAttribute = (CostMapping<T>) costMappings.get(attribute);
            TreeMap<Integer, Set<T>> costMapForOriginalValue = costMappingForAttribute.getCostMapForOriginalValue((T) dataObject.get(attribute));
            costMappingsForAttributes.put(attribute, costMapForOriginalValue.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).toList());
        }

        Map<String, Integer> firstIndices = new HashMap<>();
        Map<String, Integer> firstCosts = new HashMap<>();
        Map<String, AbstractAtom<?, ?, ?>> firstAtoms = new HashMap<>();

        for (String attribute : attributes) {
            firstIndices.put(attribute, 0);

            int firstCost = costMappingsForAttributes.get(attribute).get(0).getFirst();
            firstCosts.put(attribute, firstCost);

            Set<T> firstValues = costMappingsForAttributes.get(attribute).get(0).getSecond();
            firstAtoms.put(attribute, buildAtomFromValueSet((SigmaContractor<T>) cpf.getContractor(attribute), attribute, firstValues));
        }

        PriorityQueue<LCFCombination> toCheck = new PriorityQueue<>();
        Set<Map<String, Integer>> checkedIndices = new HashSet<>();
        int minCost = Integer.MAX_VALUE;

        toCheck.add(new LCFCombination(firstIndices, firstCosts, firstAtoms));
        checkedIndices.add(firstIndices);

        Set<Set<AbstractAtom<?, ?, ?>>> results = new HashSet<>();

        while (!toCheck.isEmpty()) {

            LCFCombination currentCombination = toCheck.poll();

            if (currentCombination.getCost() > minCost || currentCombination.getCost() > globalMinCost)
                break;

            Set<AbstractAtom<?, ?, ?>> impliedAtoms = checkCombination(currentCombination, cpf, attributes);

            impliedAtoms.removeIf(at -> !(at instanceof VariableAtom<?, ?, ?>) && at.test(dataObject));

            if (impliedAtoms.stream().anyMatch(at -> !at.equals(AbstractAtom.ALWAYS_FALSE))) {
                minCost = currentCombination.getCost();
                results.add(impliedAtoms);
            }

            for (String attribute : attributes) {

                Map<String, Integer> newIndices = new HashMap<>(currentCombination.indices);
                newIndices.put(attribute, newIndices.get(attribute) + 1);

                if (checkedIndices.contains(newIndices))
                    continue;

                if (newIndices.get(attribute) < costMappingsForAttributes.get(attribute).size()) {

                    Map<String, Integer> newCosts = new HashMap<>(currentCombination.costs);
                    int newCost = costMappingsForAttributes.get(attribute).get(newIndices.get(attribute)).getFirst();
                    newCosts.put(attribute, newCost);

                    Map<String, AbstractAtom<?, ?, ?>> newAtoms = new HashMap<>(currentCombination.atoms);
                    Set<T> newValues = costMappingsForAttributes.get(attribute).get(newIndices.get(attribute)).getSecond();
                    newAtoms.put(attribute, buildAtomFromValueSet((SigmaContractor<T>) cpf.getContractor(attribute), attribute, newValues));

                    toCheck.add(new LCFCombination(newIndices, newCosts, newAtoms));
                    checkedIndices.add(newIndices);

                }

            }

        }

        return new Pair<>(minCost, results);

    }

    private Set<AbstractAtom<?, ?, ?>> checkCombination(LCFCombination combination, CPF cpf, Set<String> attributes) {

        Set<AbstractAtom<?, ?, ?>> atoms = cpf
                .getAtoms()
                .stream()
                .filter(a -> a.getAttributes().anyMatch(attributes::contains))
                .collect(Collectors.toSet());

        atoms.addAll(combination.atoms.values());

        return CPFImplicator.imply(new CPF(atoms)).getAtoms();

    }

    private Set<CPF> combineCEAtomsets(Map<Set<String>, Set<Set<AbstractAtom<?, ?, ?>>>> ceAtomsets) {

        Set<Set<AbstractAtom<?, ?, ?>>> result = new HashSet<>();
        List<Set<Set<AbstractAtom<?, ?, ?>>>> input = new ArrayList<>(ceAtomsets.values());

        combine(input, 0, new ArrayList<>(), result);

        return result.stream().map(as -> CPFImplicator.imply(new CPF(as))).collect(Collectors.toSet());

    }

    private void combine(List<Set<Set<AbstractAtom<?, ?, ?>>>> input, int index, List<AbstractAtom<?, ?, ?>> current, Set<Set<AbstractAtom<?, ?, ?>>> result) {

        if (index == input.size()) {
            result.add(new HashSet<>(current));
            return;
        }

        for (Set<AbstractAtom<?, ?, ?>> atoms : input.get(index)) {
            current.addAll(atoms);
            combine(input, index + 1, current, result);
            current.removeAll(atoms);
        }

    }

    private <T extends Comparable<? super T>> AbstractAtom<?, ?, ?> buildAtomFromValueSet(SigmaContractor<T> contractor, String attribute, Set<T> values) {

        if (values.size() == 1 && contractor instanceof OrdinalContractor<T>) {
            T value = values.stream().findFirst().get();
            return new ConstantOrdinalAtom<>((OrdinalContractor<T>) contractor, attribute, EqualityOperator.EQ, value);
        } else if (values.size() == 1) {
            T value = values.stream().findFirst().get();
            return new ConstantNominalAtom<>((NominalContractor<T>) contractor, attribute, EqualityOperator.EQ, value);
        } else if (contractor instanceof OrdinalContractor<T>) {
            return new SetOrdinalAtom<>((OrdinalContractor<T>) contractor, attribute, SetOperator.IN, values);
        } else {
            return new SetNominalAtom<>((NominalContractor<T>) contractor, attribute, SetOperator.IN, values);
        }

    }

    private record LCFCombination(Map<String, Integer> indices, Map<String, Integer> costs, Map<String, AbstractAtom<?, ?, ?>> atoms) implements Comparable<LCFCombination>
    {

        public int getCost() {
            return costs.values().stream().mapToInt(Integer::intValue).sum();
        }

        @Override
        public int compareTo(LCFCombination other) {
            return Integer.compare(this.getCost(), other.getCost());
        }

    }
}
