package be.ugent.ledc.sigma.datastructures.values;

import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that contains centralized reason about CPFs in terms of intervals and sets.
 * 
 * @author abronsel
 */
public class IntervalOperations
{
    /**
     * Provides a list of intervals that represents the intersection of two 
     * sequences of intervals. More specifically, a value is represented in 
     * some interval in the output if that values appears in some interval in each
     * of both input lists.
     * @param <O>
     * @param left
     * @param right
     * @return 
     */
    public static <O extends Comparable<? super O>> List<Interval<O>> intersect(List<Interval<O>> left, List<Interval<O>> right)
    {
        List<Interval<O>> intersection = new ArrayList<>();

        for (Interval<O> a : left)
        {
            for (Interval<O> b : right)
            {
                if (!a.isDisjunctWith(b))
                    intersection.add(a.intersect(b));
            }
        }

        return intersection;
    }
    
    /**
     * Provides a list of intervals that represents the union of two 
     * sequences of intervals.
     * @param <O>
     * @param left
     * @param right
     * @param ctr
     * @return 
     */
    public static <O extends Comparable<? super O>> List<Interval<O>> union(List<Interval<O>> left, List<Interval<O>> right, OrdinalContractor<O> ctr)
    {
        //Init the union
        List<Interval<O>> union = Stream
            .concat(left.stream(),right.stream())
            .distinct()
            .sorted(Interval.<O>leftBoundComparator().thenComparing(Interval.rightBoundComparator()))
            .collect(Collectors.toList());

        union.removeIf(i -> union.stream().anyMatch(oi -> !oi.equals(i) && oi.con(i)));
        
        for(int i=1;i<union.size();i++)
        {
            if(canMergeWith(ctr, union.get(i), union.get(i-1)))
            {
                O rBound = union.get(i).getRightBound();
                union.get(i-1).setRightBound(rBound);
                union.get(i-1).setRightOpen(rBound == null);
                union.remove(i);
                
                i--;
            }
        }

        return union;
    }
    
    /**
     * Returns true if the union of two given intervals, would also be an interval
     * @param <O>
     * @param contractor
     * @param i1
     * @param i2
     * @return 
     */
    public static <O extends Comparable<? super O>> boolean canMergeWith(OrdinalContractor<O> contractor, Interval<O> i1, Interval<O> i2)
    {
        if(i1 == null || i2 == null)
            return false;

        if(!i1.isDisjunctWith(i2))
            return true;

        return connectsWith(contractor, i1, i2) || connectsWith(contractor, i2, i1);
    }

    private static <O extends Comparable<? super O>> boolean connectsWith(OrdinalContractor<O> contractor, Interval<O> i1, Interval<O> i2)
    {
        O r = i1.getRightBound();
        O l = i2.getLeftBound();

        if(r == null || l == null)
            return false;

        if(r.equals(l))
            return !(i1.isRightOpen() && i2.isLeftOpen());

        if(contractor.next(r).equals(l))
            return !i1.isRightOpen() && !i2.isLeftOpen();

        return false;
    }
}
