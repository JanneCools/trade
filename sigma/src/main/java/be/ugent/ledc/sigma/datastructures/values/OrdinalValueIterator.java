package be.ugent.ledc.sigma.datastructures.values;

import be.ugent.ledc.core.datastructures.Interval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleException;
import java.util.HashSet;
import java.util.Set;

/**
 * A value iterator for ordinal data types. Such data types have (i) a natural
 * order and (ii) they have a successor and a predecessor function.
 * 
 * This value iterator is backed by a sorted list of intervals.
 * @author abronsel
 * @param <T> Type of data
 */
public class OrdinalValueIterator<T extends Comparable<? super T>> implements ValueIterator<T>
{
    private final OrdinalContractor<T> contractor;
    
    private final List<Interval<T>> intervals;

    public OrdinalValueIterator(List<Interval<T>> intervals, OrdinalContractor<T> contractor)
    {
        this.intervals = intervals;
        this.contractor = contractor;
        
        //Sort intervals according to their left bound first.
        Collections.sort(intervals, Interval.leftBoundComparator());
        
        for(int i=0; i<intervals.size()-1;i++)
        {
            Interval<T> current = intervals.get(i);
            Interval<T> next = intervals.get(i+1);
            
            if(!current.allenBefore(next))
                throw new SigmaRuleException("Intervals for ordinal value iterator are erroneous."
                    + " Found current " + current + " and next " + next);
            
        }
    }
    
    public OrdinalValueIterator(OrdinalContractor<T> contractor)
    {
        this(new ArrayList<>(), contractor);
    }
    
    @Override
    public boolean inSelection(T value)
    {
        return intervals.stream().anyMatch(in -> in.contains(value));
    }

    @Override
    public long cardinality()
    {
        if(contractor.cardinality(intervals.get(0)) == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        
        if(contractor.cardinality(intervals.get(intervals.size() - 1)) == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        
        return intervals.stream().mapToLong(in -> contractor.cardinality(in)).sum();
    }

    @Override
    public boolean isEmpty()
    {
        return intervals.isEmpty();
    }

    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            private T current = intervals.isEmpty()
                    ? null
                    : intervals.get(0).getLeftBound()  == null
                        ? contractor.first()
                        : intervals.get(0).getLeftBound();

            private int index = intervals.isEmpty() ? -1 : 0;
    
            @Override
            public boolean hasNext()
            {
                return index != -1 && current != null;
            }

            @Override
            public T next()
            {
                //To return
                T toReturn = current;
                
                //Update
                T next = contractor.next(current);
                
                if(index == -1)
                    return null;
                if(intervals.get(index).contains(next))
                    current = next;
                else if(index + 1 <= intervals.size() - 1)
                {
                    index++;
                    current = intervals.get(index).getLeftBound();
                }
                else
                    current = null;
                
                return toReturn;
            }
        };
    }

    public List<Interval<T>> getIntervals() {
        return intervals;
    }

    @Override
    public NominalValueIterator<T> convert() {

        Iterator<T> it = iterator();

        Set<T> values = new HashSet<>();

        while(it.hasNext())
        {
            values.add(it.next());
        }

        return new NominalValueIterator<>(values);
    }

}
