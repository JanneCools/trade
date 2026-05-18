package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that represents a simple signal by means of a map. 
 * @author abronsel
 * @param <I> Datatype of the index
 * @param <T> Datatype of the value
 */
public class Signal<I extends Comparable<? super I>, T>
{
    /**
     * Encodes the signal as a mapping from indices to values
     */
    private final TreeMap<I, T> sequence;
    
    /**
     * Provides a contract for the indices, which fixes the unit of the index.
     */
    private final OrdinalContractor<I> indexContractor;
    
    public Signal(OrdinalContractor<I> indexContractor)
    {
        this.sequence = new TreeMap<>(Comparator.naturalOrder());
        this.indexContractor = indexContractor;
    }

    public OrdinalContractor<I> getIndexContractor()
    {
        return indexContractor;
    }
    
    public void put(I index, T value) throws ChronosException
    {
        sequence.put(indexContractor.get(index), value);
    }
    
    public void merge(I index, T value, BiFunction<T,T,T> merger) throws ChronosException
    {
        sequence.merge(indexContractor.get(index), value, merger);
    }
    
    public T get(I index)
    {
        return index == null ? null : sequence.get(indexContractor.get(index));
    }
    
    public void clear()
    {
        this.sequence.clear();
    }
    
    public Set<I> indexSet()
    {
        return new TreeSet<>(sequence.keySet());
    }
    
    public Set<Entry<I,T>> entrySet()
    {
        return sequence.entrySet();
    }
    
    public int size()
    {
        return sequence.size();
    }
    
    public boolean isEmpty()
    {
        return sequence.isEmpty();
    }
    
    //
    // Index manipulation methods
    //
    
    /**
     * Returns the greatest index strictly less than the given index, or null if there is no such index.
     * @param index
     * @return 
     */
    public I previousIndex(I index)
    {
        return this.sequence.lowerKey(index);
    }
    
    /**
     * Returns the least index strictly greater than the given index, or null if there is no such index.
     * @param index
     * @return 
     */
    public I nextIndex(I index)
    {
        return this.sequence.higherKey(index);
    }
    
    /**
     * Returns the first index of the signal
     * @return 
     */
    public I start()
    {
        return this.sequence.firstKey();
    }
    
    /**
     * Returns the last index of the signal
     * @return 
     */
    public I end()
    {
        return this.sequence.lastKey();
    }
    
    /**
     * Returns the index that is n positions away from the given index.
     * If n is positive, the direction is to the right (i.e. towards the end of the signal).
     * Is n is negative, the direction is ti the left (i.e. towards the start of the signal).
     * If n=0, the given index is returned.
     * 
     * If the signal does not contain a value for index, the first index is the higher
     * (if n is positive) or lower (if n is negative) index.
     * 
     * Note that application of jumpRight on index i is equivalent
     * to n applications of nextIndex on i (if n is positive) or n applications
     * of previousIndex on i (if n is negative).
     * 
     * @param index The starting point
     * @param n The amount of jumps to be taken
     * @return 
     */
    public I jumpRight(I index, int n)
    {
        if(get(index) == null)
            return null;

        I offsetIndex = index;
        
        if(n == 0)
            return offsetIndex;
        
        int count = 0;
        
        while(count < Math.abs(n) && offsetIndex != null)
        {
            offsetIndex = n > 0
                ? nextIndex(offsetIndex)
                : previousIndex(offsetIndex);
            
            count++;
        }
        
        return offsetIndex;
    }
    
    /**
     * A convenience method that is equivalent to jumpRight(index, -n).
     * 
     * @param index The starting point
     * @param n The amount of jumps to be taken
     * @return 
     */
    public I jumpLeft(I index, int n)
    {
        return jumpRight(index, (-1) * n);
    }
    
    public Stream<I> indexStream()
    {
        return indexSet().stream();
    }

    public boolean hasValueAt(I index)
    {
        return this.sequence.containsKey(index);
    }
    
    public T valueAt(I index)
    {
        return get(index);
    }
    
    public T valueBefore(I index)
    {
        return valueAt(previousIndex(index));
    }
    
    public T valueAfter(I index)
    {
        return valueAt(nextIndex(index));
    }
    
    /**
     * Returns part of signal for which indices are contained within
     * the range captured by lowerbound (inclusive) and upperbound (exclusive)
     * @param lowerBound
     * @param upperBound
     * @return 
     * @throws be.ugent.ledc.chronos.ChronosException 
     */
    public Signal<I,T> subSignal(I lowerBound, I upperBound) throws ChronosException
    {
        lowerBound = lowerBound == null || indexContractor.isBefore(lowerBound, start())
            ? start()
            : lowerBound;
        
        upperBound = upperBound == null
            ? this.indexContractor.add(end(),1)
            : upperBound;
        
        Signal<I,T> subSignal = new Signal<>(this.indexContractor);
        
        //Empty selection check
        if(sequence.comparator().compare(lowerBound, upperBound) > 0)
            return subSignal;
        
        SortedMap<I, T> subMap = sequence.subMap(lowerBound, upperBound);
        
        for(Entry<I,T> e: subMap.entrySet())
        {
            subSignal.put(e.getKey(), e.getValue());
        }
        
        return subSignal;
    }
    
    /**
     * Computes the gaps in between indices in terms of the units provided
     * by the index contractor. These measures can be important for computation
     * of seasonality or FFT, where evenly spaced measures are an important assumption.
     * @return 
     */
    public List<Long> interMeasureGap()
    {
        List<Long> gaps = new ArrayList<>();
        
        for(I idx: sequence.keySet())
        {
            if(idx.equals(sequence.lastKey()))
                continue;
            
            gaps.add(this.indexContractor.cardinality(
                new Interval<>(
                    idx,
                    nextIndex(idx),
                    false,
                    false)) - 1l);
        }
        
        return gaps;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.sequence);
        hash = 19 * hash + Objects.hashCode(this.indexContractor);
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
        final Signal<?, ?> other = (Signal<?, ?>) obj;
        if (!Objects.equals(this.sequence, other.sequence))
        {
            return false;
        }
        if (!Objects.equals(this.indexContractor, other.indexContractor))
        {
            return false;
        }
        return true;
    }
        
    /**
     * Lists the change points of a signal: points where the value differs
     * from the values held by the previous index.
     * @param withStart When true, the first value of the signal is also considered
     * a change point.
     * @return
     * @throws ChronosException 
     */
    public List<ChangePoint<I,T>> changePoints(boolean withStart) throws ChronosException
    {   
        return sequence
        .keySet()
        .stream()
        .filter(t -> changesAt(t, withStart))
        .map(t -> new ChangePoint<>(t, previousIndex(t), valueAt(t), valueBefore(t)))
        .collect(Collectors.toList());
    }
    
    /**
     * Returns true if the signal has a change at time idx.
     * That is: the value at idx is different from the previous value
     * @param idx
     * @param withStart When true, the first value of the signal is also considered
     * a change point.
     * @return True if the signal has a change
     */
    public boolean changesAt(I idx, boolean withStart)
    {
        return (withStart || !idx.equals(start()))
            && sequence.containsKey(idx)
            && !Objects.equals(valueAt(idx),valueBefore(idx));
    }
    
    @Override
    public String toString()
    {
        return this
        .sequence
        .entrySet()
        .stream()
        .map(e -> e.getKey().toString() + "=" + e.getValue())
        .collect(Collectors.joining(",", "[", "]"));
    }
    
    public <O> Signal<I,O> convert(Function<T,O> valueConvertor) throws ChronosException
    {
        Signal<I,O> converted = new Signal(indexContractor);
        
        for(I index: indexSet())
        {
            converted.put(index, valueConvertor.apply(valueAt(index)));
        }
        
        return converted;
    }
    
}
