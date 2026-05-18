package be.ugent.ledc.sigma.repair.cost.models;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.AtomOperations;
import be.ugent.ledc.sigma.datastructures.atoms.VariableAtom;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.OrdinalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.bounding.Bounding;
import be.ugent.ledc.sigma.repair.bounding.RangedBounding;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.CostMapping;

import java.util.*;
import java.util.stream.Collectors;

public class NonConstantCostModel extends CostModel<IterableCostFunction<?>>
{

    Map<String, Bounding<?,?,?>> boundings;

    public NonConstantCostModel(Map<String, IterableCostFunction<?>> costFunctions, Map<String, Bounding<?,?,?>> boundings)
    {
        super(costFunctions);
        this.boundings = boundings;
    }

    public NonConstantCostModel(Map<String, Bounding<?,?,?>> boundings)
    {
        this(new HashMap<>(), boundings);
    }

    public NonConstantCostModel()
    {
        this(new HashMap<>(), new HashMap<>());
    }

    public Map<String, Bounding<?,?,?>> getBoundings()
    {
        return boundings;
    }

    public Bounding<?,?,?> getBounding(String attribute)
    {
        return boundings.get(attribute);
    }

    public boolean hasBounding(String attribute)
    {
        return boundings.containsKey(attribute);
    }

    public NonConstantCostModel addBounding(String attribute, Bounding<?,?,?> bounding)
    {
        this.boundings.put(attribute, bounding);
        return this;
    }

    public <T extends Comparable< ? super T>, C extends SigmaContractor<T>, I extends ValueIterator<T>>
    Map<String, Set<Set<AbstractAtom<?, ?, ?>>>> getBoundingAtomsets(DataObject dataObject, Map<String, SigmaContractor<?>> contractors)
    {

        Map<String, Set<Set<AbstractAtom<?, ?, ?>>>> boundingAtomsets = new HashMap<>();

        Set<String> attributes = contractors.keySet();

        for (String attribute : attributes) {

            Bounding<T,C,I> bounding = (Bounding<T,C,I>) boundings.get(attribute);

//            if (!(contractors.get(attribute) instanceof OrdinalContractor)) {
//                continue;
//            }

            if (bounding == null) {
                boundingAtomsets.put(attribute, Set.of(Set.of(AbstractAtom.ALWAYS_TRUE)));
                continue;
            }

            C contractor = (C) contractors.get(attribute);

            I boundedPermittedValues = bounding.getPermittedValues(
                    attribute, dataObject, contractor, getHints(attribute, dataObject)
            );

            Set<Set<AbstractAtom<?, ?, ?>>> domainAtoms = bounding.getDomainAtomsets(attribute, dataObject, contractor, boundedPermittedValues);

            boundingAtomsets.put(attribute, domainAtoms);

        }

        return boundingAtomsets;

    }

    public <T extends Comparable<? super T>> Map<String, CostMapping<?>> getInducedCostMappings(CPF cpf, DataObject dataObject) throws RepairException
    {

        Set<String> attributes = cpf.getAttributes();

        Map<String, CostMapping<?>> inducedCostMappings = new HashMap<>();

        for (String attribute : attributes) {
            ValueIterator<T> permittedValueIterator = getPermittedValueIterator(attribute, cpf);
            CostMapping<?> inducedCostMapping = buildInducedCostMapping(attribute, dataObject, permittedValueIterator);
            inducedCostMappings.put(attribute, inducedCostMapping);
        }

        return inducedCostMappings;

    }

    private <T extends Comparable<? super T>> ValueIterator<T> getPermittedValueIterator(String attribute, CPF cpf) throws RepairException
    {

        Set<AbstractAtom<?, ?, ?>> attributeAtoms = cpf
                .getAtoms()
                .stream()
                .filter(atom -> (!(atom instanceof VariableAtom<?, ?, ?>)
                        && atom.getAttributes().anyMatch(at -> at.equals(attribute))))
                .collect(Collectors.toSet());

        SigmaContractor<?> contractor = cpf.getContractor(attribute);

        if (contractor instanceof NominalContractor) {
            return getNominalValueIterator(attribute, (NominalContractor<T>) contractor, attributeAtoms);
        } else {
            return getOrdinalValueIterator(attribute, (OrdinalContractor<T>) contractor, attributeAtoms);
        }

    }

    private <T extends Comparable<? super T>> NominalValueIterator<T> getNominalValueIterator(String attribute, NominalContractor<?> contractor, Set<AbstractAtom<?, ?, ?>> attributeAtoms) throws RepairException
    {

        if (attributeAtoms.isEmpty())
        {
            throw new RepairException("Permitted values of nominal attribute " + attribute + " are not restricted." +
                    "Therefore, it is not possible to decide on which values are permitted.");
        }
        else
        {
            Set<T> valueset = AtomOperations.getValuesetFromNominalAtoms(attributeAtoms, attribute);
            return new NominalValueIterator<>(valueset);
        }

    }

    private <T extends Comparable<? super T>> OrdinalValueIterator<T> getOrdinalValueIterator(String attribute, OrdinalContractor<T> contractor, Set<AbstractAtom<?, ?, ?>> attributeAtoms)
    {

        List<Interval<T>> intervals = new ArrayList<>();

        if (attributeAtoms.isEmpty()) {
            Interval<T> interval = new Interval<>(contractor.first(), contractor.last());
            intervals.add(interval);
        } else {
            intervals = new ArrayList<>(AtomOperations.getIntervalsFromOrdinalAtoms(attributeAtoms, attribute));

            for (Interval<T> interval : intervals) {
                if (interval.getLeftBound() == null) {
                    interval.setLeftBound(contractor.first());
                }
                if (interval.getRightBound() == null) {
                    interval.setRightBound(contractor.last());
                }
            }

        }

        return new OrdinalValueIterator<>(intervals, contractor);

    }

    private <T extends Comparable<? super T>> CostMapping<?> buildInducedCostMapping(String attribute, DataObject original, ValueIterator<T> permittedValueIterator)
    {

        Map<T, TreeMap<Integer, Set<T>>> inducedCostMap = new HashMap<>();

        IterableCostFunction<T> costFunction = (IterableCostFunction<T>) getCostFunction(attribute);
        T originalValue = (T) original.get(attribute);
        inducedCostMap.put(originalValue, new TreeMap<>());

        for (T newValue : permittedValueIterator) {
            int cost = costFunction.cost(originalValue, newValue, original);
            inducedCostMap.get(originalValue).computeIfAbsent(cost, v -> new HashSet<>()).add(newValue);
        }

        return new CostMapping<>(inducedCostMap);

    }

    private <T extends Comparable<? super T>> Set<T> getHints(String attribute, DataObject dataObject)
    {

        if (dataObject.get(attribute) != null && getCostFunctions().containsKey(attribute)) {
            IterableCostFunction<T> costFunction = (IterableCostFunction<T>) getCostFunction(attribute);
            return costFunction.alternatives((T) dataObject.get(attribute), dataObject);
        }

        return new HashSet<>();

    }

}
