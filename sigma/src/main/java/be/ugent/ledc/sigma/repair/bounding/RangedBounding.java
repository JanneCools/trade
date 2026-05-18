package be.ugent.ledc.sigma.repair.bounding;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.core.datastructures.Multiset;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.ConstantOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.InequalityOperator;
import be.ugent.ledc.sigma.datastructures.values.OrdinalValueIterator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.values.IntervalOperations;
import be.ugent.ledc.sigma.datastructures.values.NominalValueIterator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

/**
 * Bounds the domain to a range between the minimum and maximum of observed values.
 * If an offset is specified, the offset is subtracted from the minimum and added
 * to the maximum. This allows to extend the range when too narrow.
 * for the attribute
 * @author abronsel
 * @param <T>
 */
public class RangedBounding<T extends Comparable< ? super T>> implements Bounding<T, OrdinalContractor<T>, OrdinalValueIterator<T>>
{
    private final int offset;
    private final int cap;
    public static int INTERVAL_THRESHOLD = 10;

    public RangedBounding(int offset, int cap)
    {
        this.offset = offset;
        this.cap = cap;
    }
    
    public RangedBounding(int offset)
    {
        this(offset,500);
    }
    
    public RangedBounding()
    {
        this(0);
    }

    @Override
    public OrdinalValueIterator<T> getPermittedValues(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, Set<T> hints)
    {
        if (bag.keySet().stream().allMatch(o -> o.get(a) == null))
            return new OrdinalValueIterator<>(contractor);

        T low   = getLowerBound(a, bag, contractor);
        T high  = getUpperBound(a, bag, contractor);
                
        Interval<T> range = Interval.closed(low, high);
        return getPermittedValues(a, range, bag, contractor, hints);

    }

    protected OrdinalValueIterator<T> getPermittedValues(String a, Interval<T> range, Multiset<DataObject> bag, OrdinalContractor<T> contractor, Set<T> hints) {

        List<Interval<T>> intervals = new ArrayList<>();

        long jump = contractor.cardinality(range) / cap;

        //Check if the range contains too many values
        if(jump > 1L) {

            T current = range.getLeftBound();

            Set<T> values = new HashSet<>();

            while(contractor.isBefore(current, range.getRightBound()))
            {
                values.add(current);
                current = contractor.add(current, jump);
            }

            //Values currently observed are also ok
            bag.keySet().stream().filter(o -> o.get(a) != null).map(o -> contractor.getFromDataObject(o, a)).forEach(values::add);

            //Hints are also ok
            values.addAll(hints);

            values.forEach(v -> intervals.add(Interval.closed(v, v)));

        } else {

            intervals.add(range);

            for(T hint: hints)
            {
                if(!range.contains(hint))
                {
                    intervals.add(Interval.closed(hint, hint));
                }
            }
        }

        return new OrdinalValueIterator<>(intervals, contractor);

    }

    @Override
    public OrdinalValueIterator<T> boundPermittedValues(String a, Multiset<DataObject> objectBag, OrdinalContractor<T> contractor, ValueIterator currentPermittedValuesIterator, Set<T> hints) {

        OrdinalValueIterator<T> permittedValuesIterator = getPermittedValues(a, objectBag, contractor, hints);
        List<Interval<T>> permittedValues = permittedValuesIterator.getIntervals();
        
        List<Interval<T>> currentPermittedValues;
                
        if(currentPermittedValuesIterator instanceof OrdinalValueIterator)
        {
            OrdinalValueIterator<T> ovi = (OrdinalValueIterator<T>)currentPermittedValuesIterator;
            
            currentPermittedValues = ovi.getIntervals();
            
        }
        else
        {
            NominalValueIterator<T> nvi = (NominalValueIterator<T>)currentPermittedValuesIterator;
            currentPermittedValues = nvi
                .getValues()
                .stream()
                .map(v -> Interval.closed(v, v))
                .collect(Collectors.toList());
        }
        
        List<Interval<T>> intersection = IntervalOperations.intersect(permittedValues, currentPermittedValues);

        return new OrdinalValueIterator<>(intersection, contractor);

    }

    @Override
    public Set<SigmaRule> getDomainRules(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor, OrdinalValueIterator<T> currentPermittedValuesIterator)
    {
        if(bag.keySet().stream().allMatch(o -> o.get(a) == null))
            return Stream.of(new SigmaRule(new HashSet<>())).collect(Collectors.toSet());

        Set<SigmaRule> domainRules = new HashSet<>();

        
        List<Interval<T>> permittedValueIntervals = currentPermittedValuesIterator.getIntervals();
        

        if(permittedValueIntervals.size() > INTERVAL_THRESHOLD)
        {
            Iterator<T> it = currentPermittedValuesIterator.iterator();
        
            Set<T> values = new HashSet<>();

            while(it.hasNext())
            {
                values.add(it.next());
            }

            domainRules.add(new SigmaRule(SetOperations.set(new SetOrdinalAtom<>(
                contractor,
                a,
                SetOperator.NOTIN,
                values)))
            );
        }  
        else
        {
            for (int i = -1; i < permittedValueIntervals.size(); i++)
            {
                Set<AbstractAtom<?, ?, ?>> atomSet = new HashSet<>();

                Interval<T> currentInterval = i == -1 ? null : permittedValueIntervals.get(i);
                Interval<T> nextInterval = i == permittedValueIntervals.size() - 1 ? null : permittedValueIntervals.get(i + 1);

                if (currentInterval != null
                    && currentInterval.getRightBound() != null
                    && currentInterval.getRightBound().compareTo(contractor.last()) != 0
                )
                {
                    atomSet.add(new ConstantOrdinalAtom<>(contractor, a, InequalityOperator.GT, currentInterval.isRightOpen() ? contractor.next(currentInterval.getRightBound()) : currentInterval.getRightBound()));
                }

                if (nextInterval != null && nextInterval.getLeftBound() != null && nextInterval.getLeftBound().compareTo(contractor.first()) != 0) {
                    atomSet.add(new ConstantOrdinalAtom<>(contractor, a, InequalityOperator.LT, nextInterval.isLeftOpen() ? contractor.previous(nextInterval.getLeftBound()) : nextInterval.getLeftBound()));
                }

                boolean empty = 
                    currentInterval != null
                &&  nextInterval != null
                &&  currentInterval.getRightBound() != null
                &&  nextInterval.getLeftBound() != null
                &&  contractor.isAfterOrEqual(
                        contractor.add(currentInterval.getRightBound(), 1),
                        nextInterval.getLeftBound()
                    );

                if (!atomSet.isEmpty() && !empty)
                {
                    domainRules.add(new SigmaRule(atomSet));
                }
            }
        }
        
        return domainRules;

    }

    @Override
    public Set<Set<AbstractAtom<?, ?, ?>>> getDomainAtomsets(String a, DataObject dataObject, OrdinalContractor<T> contractor, OrdinalValueIterator<T> currentPermittedValuesIterator) {

        if (currentPermittedValuesIterator.isEmpty()) {
            return Set.of(Set.of(AbstractAtom.ALWAYS_TRUE));
        }

        Set<Set<AbstractAtom<?, ?, ?>>> domainCPFs = new HashSet<>();

        List<Interval<T>> permittedValueIntervals = currentPermittedValuesIterator.getIntervals();

        if (permittedValueIntervals.size() > INTERVAL_THRESHOLD) {

            Iterator<T> iterator = currentPermittedValuesIterator.iterator();

            Set<T> values = new HashSet<>();

            while(iterator.hasNext()) {
                values.add(iterator.next());
            }

            domainCPFs.add(new HashSet<>(Set.of(new SetOrdinalAtom<>(contractor, a, SetOperator.IN, values))));

        } else {

            for (Interval<T> permittedValueInterval : permittedValueIntervals) {

                Set<AbstractAtom<?, ?, ?>> atoms = new HashSet<>();

                ConstantOrdinalAtom<T> leftBoundAtom = null;
                ConstantOrdinalAtom<T> rightBoundAtom = null;

                if (permittedValueInterval.getLeftBound() != null && permittedValueInterval.getLeftBound().compareTo(contractor.first()) != 0) {
                    leftBoundAtom = new ConstantOrdinalAtom<>(contractor, a, InequalityOperator.GEQ, permittedValueInterval.isLeftOpen() ? contractor.next(permittedValueInterval.getLeftBound()) : permittedValueInterval.getLeftBound());
                }

                if (permittedValueInterval.getRightBound() != null && permittedValueInterval.getRightBound().compareTo(contractor.last()) != 0) {
                    rightBoundAtom = new ConstantOrdinalAtom<>(contractor, a, InequalityOperator.LEQ, permittedValueInterval.isRightOpen() ? contractor.previous(permittedValueInterval.getRightBound()) : permittedValueInterval.getRightBound());
                }

                if (leftBoundAtom != null && rightBoundAtom != null && leftBoundAtom.getConstant().equals(rightBoundAtom.getConstant())) {
                    atoms.add(new ConstantOrdinalAtom<>(contractor, a, EqualityOperator.EQ, leftBoundAtom.getConstant()));
                } else {
                    if (leftBoundAtom != null) {
                        atoms.add(leftBoundAtom);
                    }

                    if (rightBoundAtom != null) {
                        atoms.add(rightBoundAtom);
                    }
                }

                if (!atoms.isEmpty()) {
                    domainCPFs.add(new HashSet<>(atoms));
                }

            }

        }

        return domainCPFs;

    }
    
    private T getLowerBound(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor)
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
    
    private T getUpperBound(String a, Multiset<DataObject> bag, OrdinalContractor<T> contractor)
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
