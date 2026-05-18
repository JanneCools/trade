package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetNominalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleFactory;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Bounds the domain by enumerating observed values within a partition and around the current data object
 */
public class PartitionedTemporalBounding<T extends Comparable<? super T>, S extends Comparable<? super S>>
        implements Bounding<T, OrdinalContractor<T>, NominalValueIterator<T>> {

    Map<String, List<Pair<S,T>>> values;
    T minValue;
    T maxValue;

    String partitionAttribute;
    String partitionAttributeCurr;
    String sortAttribute;
    String sortAttributeCurr;
    OrdinalContractor<S> sortContractor;

    int range;
    int offset = 0;

    public PartitionedTemporalBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<S> sortContractor, String sortAttribute,
            OrdinalContractor<T> contractor, String attribute,
            int range, T minValue, T maxValue
    ) {
        values = new HashMap<>();
        for (DataObject dataObject: dataset.getDataObjects()) {
            String partitionValue = dataObject.get(partitionAttribute).toString();
            boolean present = values.containsKey(partitionValue);
            List<Pair<S,T>> curValues = present ? values.get(partitionValue) : new ArrayList<>();
            S sortValue = sortContractor.getFromDataObject(dataObject,  sortAttribute);
            T value = contractor.getFromDataObject(dataObject,  attribute);
            if (value != null) {
                curValues.add(new Pair<>(sortValue, value));
                values.put(partitionValue, curValues);
            } else if (!present) {
                values.put(partitionValue, new ArrayList<>());
            }
        }

        for (String key: values.keySet()) {
            List<Pair<S,T>> keyValues = values.get(key);
            keyValues.sort(Comparator.comparing(Pair::getFirst));
            values.put(key, keyValues);
        }

        this.partitionAttribute = partitionAttribute;
        this.partitionAttributeCurr = partitionAttribute + "#curr";
        this.sortAttribute = sortAttribute;
        this.sortAttributeCurr = sortAttribute + "#curr";
        this.sortContractor = sortContractor;
        this.range = range;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public PartitionedTemporalBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<S> sortContractor, String sortAttribute,
            OrdinalContractor<T> contractor, String attribute, int range
    ) {
        this(dataset, partitionAttribute, sortContractor,  sortAttribute, contractor, attribute, range, null, null);
    }

    public PartitionedTemporalBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<S> sortContractor, String sortAttribute,
            OrdinalContractor<T> contractor, String attribute,
            int range, int offset, T minValue, T maxValue
    ) {
        values = new HashMap<>();
        for (DataObject dataObject: dataset.getDataObjects()) {
            String partitionValue = dataObject.get(partitionAttribute).toString();
            boolean present = values.containsKey(partitionValue);
            List<Pair<S,T>> curValues = present ? values.get(partitionValue) : new ArrayList<>();
            S sortValue = sortContractor.getFromDataObject(dataObject,  sortAttribute);
            T value = contractor.getFromDataObject(dataObject,  attribute);
            if (value != null) {
                curValues.add(new Pair<>(sortValue, value));
                values.put(partitionValue, curValues);
            } else if (!present) {
                values.put(partitionValue, new ArrayList<>());
            }
        }

        for (String key: values.keySet()) {
            List<Pair<S,T>> keyValues = values.get(key);
            keyValues.sort(Comparator.comparing(Pair::getFirst));
            values.put(key, keyValues);
        }

        this.partitionAttribute = partitionAttribute;
        this.partitionAttributeCurr = partitionAttribute + "#curr";
        this.sortAttribute = sortAttribute;
        this.sortAttributeCurr = sortAttribute + "#curr";
        this.sortContractor = sortContractor;
        this.range = range;
        this.offset = offset;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public PartitionedTemporalBounding(
            Dataset dataset, String partitionAttribute,
            OrdinalContractor<S> sortContractor, String sortAttribute,
            OrdinalContractor<T> contractor, String attribute, int range, int offset
    ) {
        this(dataset, partitionAttribute, sortContractor, sortAttribute, contractor, attribute, range, offset, null, null);
    }

    private Pair<String, S> getPartitionAndSortValue(DataObject dataObject) {
        Set<String> attributes = dataObject.getAttributes();
        if (attributes.contains(partitionAttribute)) {
            String partitionValue = dataObject.get(partitionAttribute).toString();
            S sortValue = sortContractor.getFromDataObject(dataObject,  sortAttribute);
            return new Pair<>(partitionValue, sortValue);
        } else {
            String partitionValue = dataObject.get(partitionAttributeCurr).toString();
            S sortValue = sortContractor.getFromDataObject(dataObject,  sortAttributeCurr);
            return new Pair<>(partitionValue, sortValue);
        }
    }

    private Set<T> getValueRange(String partitionValue, S sortValue) {
        List<Pair<S,T>> partitionValues = values.get(partitionValue);
        int index = IntStream.range(0, partitionValues.size())
                .filter(i -> partitionValues.get(i).getFirst().equals(sortValue))
                .findFirst().orElse(-1);

        Set<T> result = new HashSet<>();

        if (index == -1) {
            result = partitionValues.stream().map(Pair::getSecond).collect(Collectors.toSet());
        } else {
            int min = Math.max(index - range, 0);
            int max = Math.min(index + range + 1, partitionValues.size());
            for (int i = min; i <= max && i < partitionValues.size(); i++) {
                result.add(partitionValues.get(i).getSecond());
            }
        }

        return result;
    }

    private String changeTemporalRole(String attribute) {
        if (attribute.endsWith("#curr"))
            return attribute.substring(0, attribute.length() - 4) + "next";
        else
            return attribute.substring(0, attribute.length() - 4) + "curr";
    }

    @Override
    public NominalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, Set<T> hints) {
        Set<Pair<String, S>> partitionSortValues = objectBag.keySet().stream()
                .map(this::getPartitionAndSortValue)
                .collect(Collectors.toSet());


        Set<T> boundingValues = partitionSortValues.stream()
                .map(p -> getValueRange(p.getFirst(), p.getSecond()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        for (DataObject dataObject: objectBag.keySet()) {
            T other = contractor.getFromDataObject(dataObject,  changeTemporalRole(a));
            if (other != null)
                boundingValues.add(other);
        }
        if (offset != 0) {
            Set<T> lowOffsets = boundingValues.stream()
                    .map(o -> contractor.subtract(o, offset))
                    .collect(Collectors.toSet());
            Set<T> highOffsets = boundingValues.stream()
                    .map(o -> contractor.add(o, offset))
                    .collect(Collectors.toSet());
            boundingValues.addAll(lowOffsets);
            boundingValues.addAll(highOffsets);
        }
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
