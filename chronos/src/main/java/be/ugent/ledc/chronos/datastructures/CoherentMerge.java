package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.conflict.ConflictGraph;
import be.ugent.ledc.chronos.algorithms.conflict.Fusion;
import be.ugent.ledc.chronos.algorithms.conflict.FusionOptimizer;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A coherent merge is a representation of a time signal based on intervals.
 * 
 * In a regular interval-based representation, intervals are mapped to data values.
 * Hereby, an entry I -> v (with I an interval and v a value) encodes that the signal
 * takes the value v at each point in I. To ensure consistency, intervals cannot overlap
 * in this representation. For example:
 * 
 * [0,3]  -> Home
 * [4,6]  -> Work
 * [7,10] -> Home
 * 
 * represents that a signal takes the value 'Home' at times 0,..,3 and 7,...,10.
 * 
 * A coherent merge is also a map from intervals I to values v but where intervals
 * can be nested (one interval can be a subset of another).
 * Now, an entry I -> v (with I an interval and v a value) encodes that the signal
 * takes the value v at each point in I except at the points from intervals nested in I
 * For example:
 * 
 * [0,10]  -> Home
 * [4,6]  -> Work
 * 
 * now means the signal takes the value 'Home' in 0,...,10 expect at 4,...,6.
 * A consistency condition is now that two intervals cannot have the same starting 
 * time or ending time.
 * @author abronsel
 * @param <T> 
 */
public class CoherentMerge<T>
{  
    /**
     * The key data structure of a coherent merge keeps a sequence of intervals
     * mapped to data entries.
     */
    private final TreeMap<Interval<Long>, T> sequence;
    
    /**
     * The public constructor creates an empty coherent merge. Addition of data
     * must be done via the put method.
     */
    public CoherentMerge()
    {
        this.sequence = new TreeMap<>(Interval.leftBoundComparator());
    }

    /**
     * The private constructor allows to create a coherent merge representation
     * of an existing Signal. This constructor is called from the static create method
     * that requires a strategy for approximate optimization of the coherent merge
     * representation.
     */
    private CoherentMerge(TreeMap<Interval<Long>, T> sequence)
    {
        this.sequence = sequence;
    }

    public void put(Interval<Long> next, T value) throws ChronosException
    {              
        //Sanity check: no negative indices allowed
        if(next.getLeftBound() < 0L)
            throw new ChronosException("Coherent merges accept positive indices only."
                + "Received put request for :" + next);
        
        //Sanity check: intervals must be closed
        if(next.isLeftOpen() || next.isRightOpen())
            throw new ChronosException("Coherent merges accept closed intervals only."
                + "Received put request for :" + next);
        
        if(sequence.isEmpty())
        {
            sequence.put(next, value);
            return;
        }
        
        Long lastIndex = lastIndex();
        
        //Sanity check: intervals must be added in order
        if(next.getLeftBound() <= lastIndex)
            throw new ChronosException("Put operations for coherent merges must be done in order of increasing index intervals.\n"
                + "Received put request for :" + next + ".\n"
                + "Maximal known index value was: " + lastIndex);
    
        long nStart = next.getLeftBound();
        
        Interval<Long> connectingInterval = findConnectingInterval(value);
            
        if(connectingInterval == null)
        {
            //Add as a new interval
            sequence.put(next, value);
        }
        else
        {            
            long cEnd = connectingInterval.getRightBound();

            if (cEnd + 1 == nStart)
            {
                //Fuse
                connectingInterval.setRightBound(next.getRightBound());
            }
            else
            {   
                //First condition to fuse is that there are no gaps.
                boolean canFuse = lastIndex + 1 == next.getLeftBound();

                if(canFuse)
                {
                    TreeSet<Interval<Long>> innerGap = new TreeSet<>(Interval.leftBoundComparator());

                    for(Interval<Long> intv: sequence.keySet())
                    {
                        if(intv.getLeftBound() < cEnd && intv.getRightBound() > cEnd && intv.getRightBound() < nStart)
                        {
                            canFuse = false;
                            break;
                        }

                        //Check for gaps
                        if(intv.allenAfter(connectingInterval) && intv.allenBefore(next))
                        {
                            innerGap.add(intv);
                        }
                    }
                    
                    if(canFuse)
                    {
                        //Clean
                        innerGap.removeIf(iv -> innerGap.stream().anyMatch(siv -> siv.allenContains(iv)));

                        //Check for gaps
                        if(innerGap.first().getLeftBound() != connectingInterval.getRightBound() + 1l
                        || innerGap.last().getRightBound() != next.getLeftBound() -1l        
                        || innerGap
                            .stream()
                            .anyMatch(giv ->
                                innerGap.higher(giv) != null
                            && innerGap.higher(giv).getLeftBound() - 1 != giv.getRightBound())
                        )
                        {
                            canFuse = false;
                        }
                    }
                }

                if (canFuse)
                {
                    //Fuse
                    connectingInterval.setRightBound(next.getRightBound());
                }
                else
                {
                    //Add as a new interval
                    sequence.put(next, value);
                }
            }
        }
    }
    
    public T valueAt(Long idx)
    {
        Interval<Long> key = sequence
            .keySet()
            .stream()
            .filter(itv -> itv.contains(idx)) //Find intervals that contain idx
            .sorted(Interval.<Long>leftBoundComparator().reversed())
            .findFirst()
            .orElse(null);
        
        return key == null ? null : sequence.get(key);
    }
    
    public boolean hasInterval(Interval<Long> key)
    {
        return sequence.containsKey(key);
    }
    
    public int countIntervals()
    {
        return this.sequence.size();
    }
    
    public Long lastIndex()
    {   
        for(Interval<Long> interval: sequence.descendingKeySet())
        {
            Interval<Long> pInterval = sequence.lowerKey(interval);
            
            if(pInterval == null || !pInterval.allenContains(interval))
            {
                return interval.getRightBound();
            }
        }
        
        return null;
    }
    
    public Interval<Long> findConnectingInterval(T value)
    {   
        for(Interval<Long> interval: sequence.descendingKeySet())
        {
            if(sequence.get(interval).equals(value))
                return interval;
        }
        
        return null;
    }
    
    /**
     * Returns a coherent merge representation of the time series after filtering
     * on the given test
     * @param predicate
     * @return 
     * @throws be.ugent.ledc.chronos.ChronosException 
     */
    public CoherentMerge<T> filter(Predicate<T> predicate) throws ChronosException
    {
        CoherentMerge<T> filtered = new CoherentMerge<>();
        
        TreeMap<Interval<Long>, T> buffer = new TreeMap<>(Interval.leftBoundComparator());

        for(Interval<Long> itv: this.sequence.keySet())
        {
            //Flush buffer
            flush(buffer, filtered, itv);
            
            T value = this.sequence.get(itv);
   
            if(predicate.test(value))
            {   
                Interval<Long> next = this.sequence.higherKey(itv);
                
                if(next == null || !itv.allenContains(next))
                {
                    filtered.put(new Interval(itv), value);
                }
                else
                {
                    Long left1      = itv.getLeftBound();
                    Long right1     = next.getLeftBound() - 1;
                    Long left2      = next.getRightBound() + 1;
                    Long right2     = itv.getRightBound();

                    while(itv.allenContains(next))
                    {
                        left2 = Math.max(left2, next.getRightBound() + 1);
                        next = this.sequence.higherKey(next);
                    }

                    filtered.put(Interval.closed(left1, right1), value);
                    buffer.put(Interval.closed(left2,right2), value);
                }
            }
        }
        
        //Flush buffer
        flush(buffer, filtered, null);
        
        return filtered;
    }
    
    private void flush(TreeMap<Interval<Long>, T> buffer, CoherentMerge<T> filtered, Interval<Long> itv) throws ChronosException
    {
        boolean stop = false;

        while(!stop && !buffer.isEmpty())
        {   
            if(itv == null || buffer.firstKey().getRightBound() < itv.getLeftBound())
            {
                Map.Entry<Interval<Long>, T> fe = buffer.pollFirstEntry();
                filtered.put(new Interval<>(fe.getKey()), fe.getValue());
            }
            else
            {
                stop = true;
            }
        }
    }
        
    /**
     * Create a coherent merge representation of an existing Signal.
     * @param <T> Type of data held by the coherent merge
     * @param signal Signal that must be transformed into a coherent merge representation.
     * @param fusionOptimizer A strategy for approximate optimization of the coherent
     * merge in terms of entries in the sequence map.
     * @return A coherent merge representation of the signal
     * @throws be.ugent.ledc.chronos.ChronosException
     */
    public static <T> CoherentMerge<T> create(Signal<Long, T> signal, FusionOptimizer<T> fusionOptimizer) throws ChronosException
    {
        Long from = signal.start();
        
        TreeMap<Interval<Long>, T> treeMap = new TreeMap<>(Interval.leftBoundComparator());
        
        //Find maximal contiguous sets
        for (Long idx: signal.indexSet())
        {
            if(!idx.equals(signal.start()) && !Objects.equals(idx, signal.previousIndex(idx) + 1))
            {
                produce(
                    treeMap,
                    signal.subSignal(from, idx),
                    fusionOptimizer
                );
                from = idx;
            }
        }

        if(from < signal.end())
            produce(
                treeMap,
                signal.subSignal(
                    from,
                    signal.getIndexContractor().add(signal.end(), 1)
                ),
                fusionOptimizer);
        
        return new CoherentMerge<>(treeMap);
    }
    
    private static <T> void produce(TreeMap<Interval<Long>, T> treeMap, Signal<Long, T> mcs, FusionOptimizer<T> optimizer) throws ChronosException
    {
        //First: make a local merge where value-equivalent values are merged
        TreeMap<Interval<Long>, T> localMerge = new TreeMap<>(Interval.leftBoundComparator());
        
        for(Long idx: mcs.indexSet())
        {
            if(localMerge.isEmpty() || localMerge.lastKey().getRightBound() != idx + 1)
                localMerge.put(Interval.closed(idx, idx), mcs.get(idx));
            else
                localMerge.lastKey().setRightBound(idx);
        }
        
        
        Map<T, List<Interval<Long>>> valueEquivalentMap = new HashMap<>();

        for (Interval<Long> itv : localMerge.keySet())
        {
            T value = localMerge.get(itv);

            if (!valueEquivalentMap.containsKey(value))
            {
                valueEquivalentMap.put(value, new ArrayList<>());
            }

            valueEquivalentMap.get(value).add(itv);
        }

        ConflictGraph<T> graph = new ConflictGraph<>();
        
        //Add all possible fusions to graph
        for (T value : valueEquivalentMap.keySet())
        {
            List<Interval<Long>> intervals = valueEquivalentMap.get(value);

            for (int j = 0; j < intervals.size() - 1; j++)
            {
                graph.addFusion(new Fusion<>(
                    value,
                    intervals.get(j),
                    intervals.get(j + 1)));
            }
        }

        //Add edges for blocking fusions
        for (Fusion<T> fusion : graph.fusionSet())
        {
            //Find blocking fusions
            for (Fusion<T> blockingCandidate : graph.fusionSet())
            {
                //Condition for the fusion rule
                if ((fusion.getFirst().allenAfter(blockingCandidate.getFirst())
                && fusion.getFirst().allenBefore(blockingCandidate.getLast())
                && fusion.getLast().allenAfter(blockingCandidate.getLast())
                    )
                || (fusion.getLast().allenAfter(blockingCandidate.getFirst())
                && fusion.getLast().allenBefore(blockingCandidate.getLast())
                && fusion.getFirst().allenBefore(blockingCandidate.getFirst())))
                {
                    graph.addConflict(fusion, blockingCandidate);
                }
            }
        }

        List<Fusion<T>> optimalOrder = optimizer
            .getOptimalFusions(graph)
            .stream()
            .sorted(Comparator.comparing(f -> f.getFirst().getLeftBound()))
            .collect(Collectors.toList());
        
        //For each remaining fusion in the optimal order, apply it
        for (Fusion<T> fusion : optimalOrder)
        {
            for (Interval<Long> mcsi: localMerge.keySet())
            {
                //Is this object value equivalent to the fusion?
                if (localMerge.get(mcsi).equals(fusion.getValue()))
                {
                    if (mcsi.contains(fusion.getFirst().getLeftBound()))
                    {
                        //Extend the validity interval
                        mcsi.setRightBound(Math.max(fusion.getLast().getRightBound(), mcsi.getRightBound()));
                    }

                    if (mcsi.getLeftBound().equals(fusion.getLast().getLeftBound()))
                    {
                        //Delete
                        localMerge.remove(mcsi);
                        break;
                    }
                }
            }

        }

        treeMap.putAll(localMerge);
    }
    
    @Override
    public String toString()
    {
        return this
            .sequence
            .entrySet()
            .stream()
            .map(e -> e.getKey().toString() + ": " + e.getValue().toString())
            .collect(Collectors.joining("\n"));
    }
}
