package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.dataset.Contract;
import be.ugent.ledc.core.dataset.Contract.ContractBuilder;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A temporal dataset is basically a wrapper around a Signal with index type I
 * that has DataObject values. The main purpose is to support common functionality on such
 * a signal.
 * @author abronsel
 * @param <I>
 */
public class TemporalDataset<I extends Comparable<? super I>>
{
    /**
     * A temporal dataset encodes data as a temporal signal with DataObject values
     */
    private final Signal<I, DataObject> objectSignal;

    /**
     * The contract dictates, for attributes in the objects, what their contract is.
     */
    private final Contract contract;

    /**
     * The attribute in the object that marks the time.
     */
    private final String timeAttribute;

    private TemporalDataset(Signal<I, DataObject> objectSignal, Contract contract, String timeAttribute)
    {
        this.objectSignal = objectSignal;
        this.contract = contract;
        this.timeAttribute = timeAttribute;
    }

    /**
     * Retrieve a signal that holds values for a specific attribute.
     * @param attribute
     * @return
     * @throws ChronosException
     */
    public <T> Signal<I, T> getAttributeSignal(String attribute) throws ChronosException
    {
        if(!contract.getAttributes().contains(attribute))
            throw new ChronosException("Unknown attribute " + attribute);

        //Create a signal in which attribute values are stored
        Signal<I, T> attrSignal = new Signal<>(objectSignal.getIndexContractor());

        for(I idx: objectSignal.indexSet())
        {
            if(objectSignal.get(idx).getAttributes().contains(attribute))
            {
                attrSignal.put(
                    idx,
                    objectSignal.get(idx) == null
                        ? null
                        : (T)contract
                            .getAttributeContract(attribute)
                            .getFromDataObject(objectSignal.get(idx), attribute)
                );
            }
        }

        return attrSignal;
    }

    public boolean hasAttribute(String attribute)
    {
        return contract.getAttributes().contains(attribute);
    }

    public OrdinalContractor<I> indexContractor()
    {
        return objectSignal.getIndexContractor();
    }

    public int size()
    {
        return objectSignal.size();
    }

    public I start()
    {
        return objectSignal.start();
    }

    public I end()
    {
        return objectSignal.end();
    }

    public boolean compatibleWith(TemporalDataset<I> other)
    {
        return indexContractor().equals(other.indexContractor());
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.objectSignal);
        hash = 59 * hash + Objects.hashCode(this.contract);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final TemporalDataset<?> other = (TemporalDataset<?>) obj;
        if (!Objects.equals(this.objectSignal, other.objectSignal))
        {
            return false;
        }
        if (!Objects.equals(this.contract, other.contract))
        {
            return false;
        }
        return true;
    }

    public Signal<I, DataObject> getObjectSignal()
    {
        return objectSignal;
    }

    public Contract getContract()
    {
        return contract;
    }

    public String getTimeAttribute() {
        return timeAttribute;
    }

    /**
     * Returns a temporal dataset from the given contracted dataset.
     * @param <X>
     * @param data
     * @param timeAttribute
     * @param contractor
     * @return
     * @throws ChronosException
     */
    public static <X extends Comparable<? super X>> TemporalDataset<X> create(ContractedDataset data, String timeAttribute, OrdinalContractor<X> contractor) throws ChronosException
    {
        if(!data.getContract().getAttributes().contains(timeAttribute))
            throw new ChronosException("Time attribute '"
                + timeAttribute
                + "' does not appear in the dataset.");

        //Create a new signal
        Signal<X, DataObject> signal = new Signal<>(contractor);

        for(DataObject o: data)
        {
            if(o.get(timeAttribute) == null)
                continue;

            X time = contractor.getFromDataObject(o, timeAttribute);

            if(signal.hasValueAt(time))
                System.out.println("Warning: multiple data points observed at time " + time);

            signal.put(time, o);

        }

        //Encode the signal
        return new TemporalDataset<>(signal, data.getContract(), timeAttribute);
    }

    /**
     * Returns a temporal dataset from the given contracted dataset by using
     * the available contractor for the time attribute. This requires that the
     * available contract is an OrdinalContractor
     * @param <X>
     * @param data
     * @param timeAttribute
     * @return
     * @throws ChronosException If the contractor for indexAttribute is not an OrdinalContractor
     */
    public static <X extends Comparable<? super X>> TemporalDataset<X> create(ContractedDataset data, String timeAttribute) throws ChronosException
    {
        if(!(data.getContract().getAttributeContract(timeAttribute) instanceof OrdinalContractor))
            throw new ChronosException("Contractor for "
                + timeAttribute
                + " is not an ordinal contractor.");

        return create(
            data,
            timeAttribute,
            (OrdinalContractor<X>)data
                .getContract()
                .getAttributeContract(timeAttribute)
        );
    }

    /**
     * Returns a mapping of partition identifiers to temporal datasets from the given contracted dataset.
     * The partition is done by values for a given partition attribute.
     * This signal is built from an index attribute, for which a specific contractor is given.
     * @param <X>
     * @param data
     * @param timeAttribute
     * @param contractor
     * @param partitionAttribute
     * @return
     * @throws ChronosException
     */
    public static <X extends Comparable<? super X>> Map<String, TemporalDataset<X>> create(ContractedDataset data, String timeAttribute, OrdinalContractor<X> contractor, String partitionAttribute) throws ChronosException
    {
        if(!data.getContract().getAttributes().contains(timeAttribute))
            throw new ChronosException("Index attribute '"
                + timeAttribute
                + "' does not appear in the dataset.");

        if(!data.getContract().getAttributes().contains(partitionAttribute))
            throw new ChronosException("Partition attribute '"
                + partitionAttribute
                + "' does not appear in the dataset.");

        //Create a new signal
        Map<String, Signal<X, DataObject>> signalMap = new HashMap<>();

        for(DataObject o: data)
        {
            if(o.get(timeAttribute) == null)
                continue;

            if(o.get(partitionAttribute) == null)
                continue;

            String partition = o
                .get(partitionAttribute)
                .toString();

            if(signalMap.get(partition) == null)
                signalMap.put(
                    partition,
                    new Signal<>(contractor)
                );

            X time = contractor.getFromDataObject(o, timeAttribute);

            if(signalMap.get(partition).hasValueAt(time))
                System.out.println("Warning: multiple data points observed in partition "
                    + partition
                    + " at time "
                    + time);

            signalMap
                .get(partition)
                .put(time,o);
        }

        return signalMap
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new TemporalDataset<>(
                        e.getValue(),
                        data.getContract(),
                        timeAttribute)
            ));
    }

    public static <X extends Comparable<? super X>> Map<String, TemporalDataset<X>> create(ContractedDataset data, String timeAttribute, String partitionAttribute) throws ChronosException
    {
        if(!(data.getContract().getAttributeContract(timeAttribute) instanceof OrdinalContractor))
            throw new ChronosException("Contractor for "
                + timeAttribute
                + " is not an ordinal contractor.");

        return create(
            data,
            timeAttribute,
            (OrdinalContractor<X>)data
                .getContract()
                .getAttributeContract(timeAttribute),
            partitionAttribute
        );
    }

    public static <X extends Comparable<? super X>> TemporalDataset<X> merge(String leftPrefix, TemporalDataset<X> left, String rightPrefix, TemporalDataset<X> right) throws ChronosException
    {
        return merge(leftPrefix, left, rightPrefix, right, true, true);
    }

    public static <X extends Comparable<? super X>> TemporalDataset<X> merge(String leftPrefix, TemporalDataset<X> left, String rightPrefix, TemporalDataset<X> right, boolean leftIndex, boolean keepOther) throws ChronosException
    {
        if(!left.compatibleWith(right))
            throw new ChronosException("Cannot merge datasets with different "
                    + "contractors for the time attribute.");

        ContractBuilder joined = new Contract.ContractBuilder();

        left.getContract()
            .getAttributes()
            .stream()
            .forEach(a -> joined.addContractor(
                leftPrefix.isBlank() ? a : leftPrefix.concat(".").concat(a),
                left.getContract().getAttributeContract(a))
            );

        right.getContract()
            .getAttributes()
            .stream()
            .forEach(a -> joined.addContractor(
                rightPrefix.isBlank() ? a : rightPrefix.concat(".").concat(a),
                right.getContract().getAttributeContract(a))
            );

        Set<X> indices = SetOperations.union(
            left.getObjectSignal().indexSet(),
            right.getObjectSignal().indexSet()
        );

        Signal<X, DataObject> mergedSignal = new Signal<>(left.indexContractor());

        String mergedTimeAttribute = leftIndex
            ? left.getTimeAttribute()
            : right.getTimeAttribute();

        Signal<X, DataObject> lSignal = left.getObjectSignal();
        Signal<X, DataObject> rSignal = right.getObjectSignal();

        for(X idx: indices)
        {
            DataObject merged = new DataObject();

            if(lSignal.hasValueAt(idx))
            {
                for(String a: lSignal.valueAt(idx).getAttributes())
                {
                    if(!a.equals(left.getTimeAttribute()))
                        merged.set(
                            name(leftPrefix, a),
                            lSignal.valueAt(idx).get(a)
                        );
                }
            }
            if(rSignal.hasValueAt(idx))
            {
                for(String a: rSignal.valueAt(idx).getAttributes())
                {
                    if(!a.equals(right.getTimeAttribute()))
                        merged.set(
                            name(rightPrefix, a),
                            rSignal.valueAt(idx).get(a)
                        );
                }
            }

            //Add the time attribute
            merged.set(mergedTimeAttribute, idx);

            if(leftIndex && keepOther)
            {
                merged.set(name(rightPrefix, right.getTimeAttribute()), idx);
            }

            if(!leftIndex && keepOther)
            {
                merged.set(name(leftPrefix, left.getTimeAttribute()), idx);
            }

            mergedSignal.put(idx, merged);
        }

        return new TemporalDataset<>(
            mergedSignal,
            joined.build(),
            mergedTimeAttribute
        );
    }

    private static String name(String prefix, String a)
    {
        return prefix.isBlank() ? a : prefix.concat(".").concat(a);
    }

    public ContractedDataset mergeWithContracted(ContractedDataset dataset) {
        // add the data objects to the dataset
        I curr = objectSignal.start();
        while (curr != null) {
            DataObject o = objectSignal.get(curr);
            o.set(timeAttribute, curr);
            curr = objectSignal.nextIndex(curr);
            dataset.addDataObject(o);

        }

        return dataset;
    }
}
