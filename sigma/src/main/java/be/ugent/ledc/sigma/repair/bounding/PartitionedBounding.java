package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleFactory;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bounds the domain by enumerating observed values within a specific partition of the dataset.
 * For each observed value, two additional values are enumerated based on an offset.
 * @param <T>
 */
public class PartitionedBounding<T extends Comparable<? super T>> implements Bounding<T, OrdinalContractor<T>, NominalValueIterator<T>> {

    Map<String, Set<T>> values;
    T minValue;
    T maxValue;
    String partitionAttribute;
    String partitionAttributeCurr;
    int offset;

    public PartitionedBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<T> contractor, String attribute,
            int offset, T minValue, T maxValue
    ) {
        // retrieve set of values per partition
        values = new HashMap<>();
        for (DataObject dataObject : dataset.getDataObjects()) {
            String partitionValue = dataObject.get(partitionAttribute).toString();
            boolean present = values.containsKey(partitionValue);
            Set<T> curValues = present ? values.get(partitionValue) : new HashSet<>();
            T value = contractor.getFromDataObject(dataObject, attribute);
            if (value != null) {
                curValues.add(value);
                values.put(partitionValue, curValues);
            } else if (!present) {
                values.put(partitionValue, new HashSet<>());
            }
        }

        this.partitionAttribute = partitionAttribute;
        this.partitionAttributeCurr = partitionAttribute + "#curr";
        this.offset = offset;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public PartitionedBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<T> contractor, String attribute,
            int offset
    ) {
        this(dataset, partitionAttribute, contractor, attribute, offset, null, null);
    }

    private String getPartitionValue(DataObject dataObject) {
        Set<String> attributes = dataObject.getAttributes();
        if (attributes.contains(partitionAttribute)) {
            return dataObject.get(partitionAttribute).toString();
        } else {
            return dataObject.get(partitionAttributeCurr).toString();
        }
    }

    @Override
    public NominalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, Set<T> hints) {
        Set<String> partitionValues = objectBag.keySet().stream()
                .map(this::getPartitionValue)
                .collect(Collectors.toSet());

        Set<T> boundingValues = partitionValues.stream()
                .map(v -> values.get(v))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Set<T> lowOffsets = boundingValues.stream()
                .map(o -> contractor.subtract(o, offset))
                .collect(Collectors.toSet());
        Set<T> highOffsets = boundingValues.stream()
                .map(o -> contractor.add(o, offset))
                .collect(Collectors.toSet());
        boundingValues.addAll(lowOffsets);
        boundingValues.addAll(highOffsets);
        if (minValue != null) boundingValues.add(minValue);
        if (maxValue != null) boundingValues.add(maxValue);
        return new NominalValueIterator<>(boundingValues);
    }

    @Override
    public NominalValueIterator<T> boundPermittedValues(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {
        NominalValueIterator<T> permittedValuesIterator = getPermittedValues(a, objectBag, contractor, hints);
        Set<T> permittedValues = permittedValuesIterator.getValues();

        permittedValues.removeIf(v -> !currentPermittedValuesIterator.inSelection(v));

        return new NominalValueIterator<>(permittedValues);
    }

    @Override
    public Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {

        SigmaRule domainRule = SigmaRuleFactory.createOrdinalDomainRule(a, currentPermittedValuesIterator.getValues(), contractor);

        return Stream.of(domainRule).collect(Collectors.toSet());
    }

    @Override
    public Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, OrdinalContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {
        Set<AbstractAtom<?, ?, ?>> atoms = new HashSet<>();

        SetAtom<?, OrdinalContractor<?>> atom = new SetOrdinalAtom(
                contractor,
                a,
                SetOperator.IN,
                currentPermittedValuesIterator.getValues()
        );
        atoms.add(atom);

        return Stream.of(atoms).collect(Collectors.toSet());
    }
}
