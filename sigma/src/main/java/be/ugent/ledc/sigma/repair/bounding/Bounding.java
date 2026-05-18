package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.Set;

/**
 * If the set of permitted values of an attribute is too large, iterating over
 * all values becomes intractable.When this situation is detected, a Bounding
 aims at restricting the domain to a set of values for which it is
 reasonable to expect that it will contain the repair value and for which iterating
 remains tractable
 * @author abronsel
 * @param <T>
 * @param <C>
 * @param <I>
 */
public interface Bounding<T extends Comparable<? super T>, C extends SigmaContractor<T>, I extends ValueIterator<T>>
{

    I getPermittedValues(String a, Multiset<DataObject> objectBag, C contractor, Set<T> hints);

    default I getPermittedValues(String a, DataObject dataObject, C contractor, Set<T> hints) {

        Multiset<DataObject> bag = new Multiset<>();
        bag.add(dataObject);

        return getPermittedValues(a, bag, contractor, hints);

    }

    I boundPermittedValues(String a, Multiset<DataObject> objectBag, C contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints);

    default I boundPermittedValues(String a, DataObject dataObject, C contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {

        Multiset<DataObject> bag = new Multiset<>();
        bag.add(dataObject);

        return boundPermittedValues(a, bag, contractor, currentPermittedValuesIterator, hints);

    }

    Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> objectBag, C contractor, I currentPermittedValuesIterator);

    default Set<SigmaRule> getDomainRules(String a, DataObject dataObject, C contractor, I currentPermittedValuesIterator) {

        Multiset<DataObject> bag = new Multiset<>();
        bag.add(dataObject);

        return getDomainRules(a, bag, contractor, currentPermittedValuesIterator);

    }

    Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, C contractor, I currentPermittedValuesIterator);

}
