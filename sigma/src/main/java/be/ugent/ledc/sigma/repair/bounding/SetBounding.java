package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetBounding<T extends Comparable<? super T>> implements Bounding<T, SigmaContractor<T>, NominalValueIterator<T>> {

    Set<T> values;

    public SetBounding(Set<T> values) {
        this.values = values;
    }

    @Override
    public NominalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> objectBag, SigmaContractor<T> contractor, Set<T> hints) {
        return new NominalValueIterator<>(values);
    }

    @Override
    public NominalValueIterator<T> boundPermittedValues(String a, Multiset<DataObject> objectBag, SigmaContractor<T> contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {
        NominalValueIterator<T> permittedValuesIterator = getPermittedValues(a, objectBag, contractor, hints);
        Set<T> permittedValues = permittedValuesIterator.getValues();

        permittedValues.removeIf(v -> !currentPermittedValuesIterator.inSelection(v));

        return new NominalValueIterator<>(permittedValues);
    }

    @Override
    public Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> objectBag, SigmaContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {
        SigmaRule domainRule;

        if (contractor instanceof OrdinalContractor<T>) {
            domainRule = SigmaRuleFactory.createOrdinalDomainRule(
                    a, currentPermittedValuesIterator.getValues(), (OrdinalContractor<T>) contractor
            );
        } else {
            domainRule = SigmaRuleFactory.createNominalDomainRule(a, currentPermittedValuesIterator.getValues(), (NominalContractor<T>) contractor);
        }
        return Stream.of(domainRule).collect(Collectors.toSet());
    }

    @Override
    public Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, SigmaContractor<T> contractor, NominalValueIterator<T> currentPermittedValuesIterator) {
        Set<AbstractAtom<?,?,?>> atoms = new HashSet<>();

        if (contractor instanceof OrdinalContractor<T>) {
            AbstractAtom<?,?,?> atom = new SetOrdinalAtom<>(
                    (OrdinalContractor<T>) contractor,
                    a,
                    SetOperator.IN,
                    currentPermittedValuesIterator.getValues()
            );
            atoms.add(atom);
        } else {
            AbstractAtom<?,?,?> atom = new SetNominalAtom<>(
                    (NominalContractor<T>) contractor,
                    a,
                    SetOperator.IN,
                    currentPermittedValuesIterator.getValues()
            );
            atoms.add(atom);
        }

        return Stream.of(atoms).collect(Collectors.toSet());
    }
}
