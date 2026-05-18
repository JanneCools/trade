package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetNominalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.HashSet;

/**
 * Bounds the domain by enumerating observed values.
 * @author abronsel
 * @param <T>
 */
public class EnumeratedBounding<T extends Comparable<? super T>> implements Bounding<T, SigmaContractor<T>, NominalValueIterator<T>>
{
    @Override
    public NominalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> bag, SigmaContractor<T> contractor, Set<T> hints)
    {
        if(bag.keySet().stream().allMatch(o -> o.get(a) == null))
            return new NominalValueIterator<>(new HashSet<>());
        
        return new NominalValueIterator<>(
            Stream.concat(
                bag
                    .keySet()
                    .stream()
                    .filter(o -> o.get(a) != null)
                    .map(o -> contractor.getFromDataObject(o, a)),
                hints.stream()
            )
            .collect(Collectors.toSet())
        );
    }

    @Override
    public NominalValueIterator<T> boundPermittedValues(String a, Multiset<DataObject> objectBag, SigmaContractor<T> contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {

        NominalValueIterator<T> permittedValuesIterator = getPermittedValues(a, objectBag, contractor, hints);
        Set<T> permittedValues = permittedValuesIterator.getValues();

        permittedValues.removeIf(v -> !currentPermittedValuesIterator.inSelection(v));

        return new NominalValueIterator<>(permittedValues);

    }

    @Override
    public Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> bag, SigmaContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {

        SigmaRule domainRule;

        if (contractor instanceof NominalContractor) {
            domainRule = SigmaRuleFactory.createNominalDomainRule(a, currentPermittedValuesIterator.getValues(), (NominalContractor<?>) contractor);
        }
        else {
            domainRule = SigmaRuleFactory.createOrdinalDomainRule(a, currentPermittedValuesIterator.getValues(), (OrdinalContractor<?>) contractor);
        }

        return Stream.of(domainRule).collect(Collectors.toSet());
    }

    @Override
    public Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, SigmaContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {
        Set<AbstractAtom<?, ?, ?>> atoms = new HashSet<>();

        if (contractor instanceof NominalContractor) {
            SetAtom<?, NominalContractor<?>> atom = new SetNominalAtom(
                    (NominalContractor) contractor,
                    a,
                    SetOperator.IN,
                    currentPermittedValuesIterator.getValues());
            atoms.add(atom);
        } else {
            SetAtom<?, OrdinalContractor<?>> atom = new SetOrdinalAtom(
                    (OrdinalContractor) contractor,
                    a,
                    SetOperator.IN,
                    currentPermittedValuesIterator.getValues()
            );
            atoms.add(atom);
        }

        return Stream.of(atoms).collect(Collectors.toSet());
    }
}
