package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleFactory;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumeratedOffsetBounding<T extends Comparable< ? super T>> implements Bounding<T, OrdinalContractor<T>, NominalValueIterator<T>>
{
    private final int offset;

    public EnumeratedOffsetBounding(int offset)
    {
        this.offset = offset;
    }
    
    public EnumeratedOffsetBounding()
    {
        this(0);
    }
    
    @Override
    public NominalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, Set<T> hints)
    {
        if(bag.keySet().stream().allMatch(o -> o.get(a) == null))
            return new NominalValueIterator<>(new HashSet<>());
        
        Set<T> values = bag
            .keySet()
            .stream()
            .filter(o -> o.get(a) != null)
            .map(o -> contractor.getFromDataObject(o, a))
            .collect(Collectors.toSet());
            
        values.add(getLowOffset(a, bag, contractor));
        values.add(getHighOffset(a, bag, contractor));
        
        values.addAll(hints);
            
        return new NominalValueIterator<>(values);
    }

    @Override
    public NominalValueIterator<T> boundPermittedValues(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {

        NominalValueIterator<T> permittedValuesIterator = getPermittedValues(a, objectBag, contractor, hints);
        Set<T> permittedValues = permittedValuesIterator.getValues();

        permittedValues.removeIf(v -> !currentPermittedValuesIterator.inSelection(v));

        return new NominalValueIterator<>(permittedValues);

    }

    @Override
    public Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator)
    {   
        Set observedValues = currentPermittedValuesIterator.getValues();
        
        if(!observedValues.isEmpty())
        {
            observedValues.add(getLowOffset(a, bag, contractor));
            observedValues.add(getHighOffset(a, bag, contractor));
        }
            
        SigmaRule domainRule = SigmaRuleFactory
                .createOrdinalDomainRule(
                    a,
                    observedValues,
                    contractor
                );

        return Stream.of(domainRule).collect(Collectors.toSet());
    }

    @Override
    public Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, OrdinalContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {
        Set observedValues = currentPermittedValuesIterator.getValues();
        Multiset<DataObject> bag = new Multiset<>();
        bag.add(dataObject);

        if(!observedValues.isEmpty())
        {
            observedValues.add(getLowOffset(a, bag, contractor));
            observedValues.add(getHighOffset(a, bag, contractor));
        }

        Set<AbstractAtom<?,?,?>> atoms = new HashSet<>();
        atoms.add(new SetOrdinalAtom<>(
                contractor,
                a,
                SetOperator.IN,
                currentPermittedValuesIterator.getValues()
        ));

        return Stream.of(atoms).collect(Collectors.toSet());
    }

    private T getLowOffset(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor)
    {
        return contractor.subtract(
            bag
                .keySet()
                .stream()
                .filter(o -> o.get(a) != null)
                .map(o -> contractor.getFromDataObject(o, a))
                .min(Comparator.naturalOrder())
                .get(),
            offset);
    }
    
    private T getHighOffset(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor)
    {
        return contractor.add(
            bag
                .keySet()
                .stream()
                .filter(o -> o.get(a) != null)
                .map(o -> contractor.getFromDataObject(o, a))
                .max(Comparator.naturalOrder())
                .get(),
            offset);
    }
}
