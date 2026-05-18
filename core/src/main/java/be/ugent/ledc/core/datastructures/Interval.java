package be.ugent.ledc.core.datastructures;

import be.ugent.ledc.core.DataException;

import java.util.*;

public class Interval<T extends Comparable<? super T>>
{
    private T leftBound = null;
    private T rightBound = null;
    
    private boolean leftOpen = true;
    private boolean rightOpen = true;
    
    private static final Comparator<Interval> LCOMPARATOR = Comparator.comparing(Interval::getLeftBound, Comparator.nullsFirst(Comparator.naturalOrder()));
    private static final Comparator<Interval> RCOMPARATOR = Comparator.comparing(Interval::getRightBound, Comparator.nullsLast(Comparator.naturalOrder()));
    
    public Interval(T leftBound, T rightBound, boolean leftOpen, boolean rightOpen)
    {
        setLeftBound(leftBound);
        setRightBound(rightBound);
        setLeftOpen(leftOpen);
        setRightOpen(rightOpen);
    }
    
    public Interval(Interval<T> i)
    {
        this(i.getLeftBound(), i.getRightBound(), i.isLeftOpen(), i.isRightOpen());
    }
    
    public Interval(T leftBound, T rightBound)
    {
        this(leftBound, rightBound, true, true);
    }

    public Interval()
    {
        this(null, null);
    }
    
    public T getLeftBound()
    {
        return leftBound;
    }

    public final void setLeftBound(T leftBound)
    {
        this.leftBound = leftBound;
        validate();
    }

    public T getRightBound()
    {
        return rightBound;
    }

    public final void setRightBound(T rightBound)
    {   
        this.rightBound = rightBound;
        validate();
    }
    
    private void validate()
    {
        if(leftBound != null && rightBound != null && leftBound.compareTo(rightBound) > 0)
        {
            throw new DataException("Left bound (" + leftBound + ") can not be strictly greater than right bound (" + rightBound + ")");
        }
        
        if(leftBound == null)
        {
            leftOpen = true;
        }
        
        if(rightBound == null)
        {
            rightOpen = true;
        }
    }

    public boolean isLeftOpen()
    {
        return leftOpen;
    }

    public final void setLeftOpen(boolean leftOpen)
    {
        this.leftOpen = leftOpen;
        validate();
    }

    public boolean isRightOpen()
    {
        return rightOpen;
    }

    public final void setRightOpen(boolean rightOpen)
    {
        this.rightOpen = rightOpen;
        validate();
    }
    
    public boolean isOpen()
    {
        return isLeftOpen() && isRightOpen();
    }
        
    //
    // Point-to-Interval relationships
    //
    
    public boolean contains(T point)
    {
        if(point == null)
        {
            return false;
        }
        
        return (leftBound == null || (isLeftOpen() && point.compareTo(leftBound) > 0) || (!isLeftOpen() && point.compareTo(leftBound) >= 0)) && (rightBound == null || (isRightOpen() && point.compareTo(rightBound) < 0) || (!isRightOpen() && point.compareTo(rightBound) <= 0));
    }
    
    public boolean isBefore(T point)
    {
        if(point == null || rightBound == null)
        {
            return false;
        }
        
        return (isRightOpen() && rightBound.compareTo(point) <= 0) || (!isRightOpen() && rightBound.compareTo(point) < 0);
    }
    
    public boolean isAfter(T point)
    {
        if(point == null || leftBound == null)
        {
            return false;
        }
        
        return (isLeftOpen()&& leftBound.compareTo(point) >= 0) || (!isLeftOpen() && leftBound.compareTo(point) > 0);
    }
    
    //
    // Implementation of Allen Relationships for intervals
    //
    
    /**
     * Allen relationship "allenBefore".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenBefore interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenBefore(Interval<T> i)
    {
        if(i == null)   
            return false;
        
        return rlBefore(i);
    }
    
    /**
     * Allen relationship "allenMeets".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenMeets interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenMeets(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return !isRightOpen() && !i.isLeftOpen() && isEqual(rightBound, i.getLeftBound());
    }
    
    /**
     * Allen relationship "allenOverlaps".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenOverlaps interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenOverlaps(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return  llBefore(i) && isAfter(rightBound, i.getLeftBound()) && rrBefore(i);
    }
    
    /**
     * Allen relationship "allenStarts".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenStarts interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenStarts(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return  llEqual(i) && rrBefore(i);
    }
    
    /**
     * Allen relationship "allenDuring".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenDuring interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenDuring(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return llAfter(i) && rrBefore(i);
    }
    
    /**
     * Allen relationship "allenFinishes".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenFinishes interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenFinishes(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return llAfter(i) && rrEqual(i);
    }
    
    /**
     * Allen relationship "allenEquals".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenEquals interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenEquals(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return llEqual(i) && rrEqual(i);
    }
    
    /**
     * Allen relationship "allenFinishedBy".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is finished by interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenFinishedBy(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return i.allenFinishes(this);
    }
    
    /**
     * Allen relationship "allenContains".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenContains interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenContains(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return i.allenDuring(this);
    }
    
    /**
     * Allen relationship "allenStartedBy".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is started by interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenStartedBy(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return i.allenStarts(this);
    }  
      
    /**
     * Allen relationship "allenOverlappedBy".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is overlapped by interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenOverlappedBy(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return i.allenOverlaps(this);
    }
    
    /**
     * Allen relationship "allenMetBy".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is met by interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenMetBy(Interval<T> i)
    {
        if(i == null)
            return false;
        
        return i.allenMeets(this);
    }
    
    /**
     * Allen relationship "allenAfter".
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenAfter interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean allenAfter(Interval<T> i)
    {
        if(i == null)
            return false;       
        
        return i.allenBefore(this);
    }
    
    
    //
    // Usefull combinations of Allen Relationships
    //
    
    /**
     * Pointed out by Allen, the relationship "dur" holds when this interval allenStarts, allenFinishes or is allenDuring interval i.
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenDuring interval i, allenStarts interval i or allenFinishes interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean dur(Interval<T> i)
    {       
        return allenDuring(i) || allenStarts(i) || allenFinishes(i);
    }
    
    /**
     * Pointed out by Allen, the relationship "con" holds when this interval is started by, is finished by or allenContains interval i.
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval allenContains interval i, is started by interval i or is finished by interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean con(Interval<T> i)
    {       
        return allenContains(i) || allenStartedBy(i) || allenFinishedBy(i);
    }
    
    /**
     * Utility method to check if two intervals are disjunct, meaning the intersection of two intervals is empty. This is true if this interval is allenBefore or allenAfter interval i in the sense of Allen's relationships.
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenBefore or allenAfter interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean isDisjunctWith(Interval<T> i)
    {
        return allenBefore(i) || allenAfter(i);
    }
    
    /**
     * Utility method to check if two intervals are weakly disjunct, meaning the intersection of two intervals may only contain interval boundaries. This is true if this interval is allenBefore or allenAfter interval i in the sense of Allen's relationships, or if the two intervals meet.
     * @param i The interval to which this interval is compared.
     * @return Returns true if this interval is allenBefore or allenAfter interval i in the sense of Allen's relationships. Returns false otherwise.
     */
    public boolean isWeakDisjunctWith(Interval<T> i)
    {
        return allenBefore(i) || allenAfter(i) || allenMeets(i) || allenMetBy(i);
    }
    
    //
    // Utility methods
    //
    public boolean llBefore(Interval<T> i)
    {
        // If current has -inf bound and i has a finite bound, return true
        if(leftBound == null && i.getLeftBound() != null)
            return true;
        
        if(!leftOpen && i.isLeftOpen())
            return isBefore(leftBound, i.getLeftBound()) || isEqual(leftBound, i.getLeftBound());
        
        return isBefore(leftBound, i.getLeftBound());
    }
    
    public boolean llEqual(Interval<T> i)
    {
        return  ((leftBound == null && i.getLeftBound() == null) || (leftOpen == i.isLeftOpen() && isEqual(leftBound, i.getLeftBound())));
    }
    
    public boolean llAfter(Interval<T> i)
    {
        // If current has a finite left bound and i has a -inf bound, return true
        if(leftBound != null && i.getLeftBound() == null)
            return true;
        
        if(leftOpen && !i.isLeftOpen())
            return isAfter(leftBound, i.getLeftBound()) || isEqual(leftBound, i.getLeftBound());
        
        return isAfter(leftBound, i.getLeftBound());
    }
    
    public boolean rrBefore(Interval<T> i)
    {
        // If current has finite bound and i has a +inf bound, return true
        if(rightBound != null && i.getRightBound() == null)
            return true;
        
        if(rightOpen && !i.isRightOpen())
            return isBefore(rightBound, i.getRightBound()) || isEqual(rightBound, i.getRightBound());
        
        return isBefore(rightBound, i.getRightBound());
    }
    
    public boolean rrEqual(Interval<T> i)
    {
        return  ((rightBound == null && i.getRightBound() == null) || (rightOpen == i.isRightOpen() && isEqual(rightBound, i.getRightBound())));
    }
    
    public boolean rlBefore(Interval<T> i)
    {
        if(rightOpen || i.isLeftOpen())
            return isBefore(rightBound, i.getLeftBound()) || isEqual(rightBound, i.getLeftBound());
                    
        return isBefore(rightBound, i.getLeftBound());
    }

    private boolean isBefore(T point1, T point2)
    {
        if(point1 == null || point2 == null)
            return false;
        
        return point1.compareTo(point2) < 0;
    }
    
    private boolean isAfter(T point1, T point2)
    {
        if(point1 == null || point2 == null)
            return false;
        
        return point1.compareTo(point2) > 0;
    }
    
    private boolean isEqual(T point1, T point2)
    {
        if(point1 == null || point2 == null)
            return false;
        
        return point1.compareTo(point2) == 0;
    }

    @Override
    public String toString()
    {
        return (!leftOpen?"[":"]") + leftBound + ", " + rightBound + (rightOpen?"[":"]");
    }
    
    //
    // Operations on intervals
    //
    
    public Interval<T> intersect(Interval<T> interval)
    {
        if(isDisjunctWith(interval))
            return null;
        
        //Determine left and right bound of intersection
        T iLeftBound    = llBefore(interval) ? interval.getLeftBound() : this.leftBound;
        T iRightBound   = rrBefore(interval) ? this.rightBound : interval.getRightBound();
        
        //Determine whether or not left and right bounds are open
        boolean iLeftOpen  = llBefore(interval) ? interval.isLeftOpen(): this.leftOpen;
        boolean iRightOpen = rrBefore(interval) ? this.rightOpen : interval.isRightOpen();
        
        return new Interval<>(iLeftBound, iRightBound, iLeftOpen, iRightOpen);
    }

    public Set<Interval<T>> union(Interval<T> interval) {

        if (isDisjunctWith(interval)) {
            return new HashSet<>(Arrays.asList(this, interval));
        }

        //Determine left and right bound of union
        T iLeftBound = llBefore(interval) ? this.leftBound : interval.getLeftBound();
        T iRightBound = rrBefore(interval) ? interval.getRightBound() : this.rightBound;

        //Determine whether or not left and right bounds are open
        boolean iLeftOpen  = llBefore(interval) ? this.leftOpen : interval.isLeftOpen();
        boolean iRightOpen = rrBefore(interval) ? interval.isRightOpen() : this.rightOpen;

        return Collections.singleton(new Interval<>(iLeftBound, iRightBound, iLeftOpen, iRightOpen));

    }
    
    //
    // Comparators
    //    
    public static <T extends Comparable<? super T>> Comparator<Interval<T>> leftBoundComparator()
    {
        return (i1,i2) ->  LCOMPARATOR.compare(i1, i2) == 0 ? Boolean.compare(i1.isLeftOpen(), i2.isLeftOpen()) : LCOMPARATOR.compare(i1, i2);
    }
    
    public static <T extends Comparable<? super T>> Comparator<Interval<T>> rightBoundComparator()
    {
        return (i1,i2) ->  RCOMPARATOR.compare(i1, i2) == 0 ? Boolean.compare(i1.isRightOpen(), i2.isRightOpen()) : RCOMPARATOR.compare(i1, i2);
    }

    /**
     * A convenience creator method for closed intervals.
     * @param <T>
     * @param left
     * @param right
     * @return 
     */
    public static <T extends Comparable<? super T>> Interval<T> closed(T left, T right)
    {
        return new Interval<>(left,right,false,false);
    }
    
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.leftBound);
        hash = 59 * hash + Objects.hashCode(this.rightBound);
        hash = 59 * hash + (this.leftOpen ? 1 : 0);
        hash = 59 * hash + (this.rightOpen ? 1 : 0);
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
        final Interval<?> other = (Interval<?>) obj;
        if (this.leftOpen != other.leftOpen)
        {
            return false;
        }
        if (this.rightOpen != other.rightOpen)
        {
            return false;
        }
        if (!Objects.equals(this.leftBound, other.leftBound))
        {
            return false;
        }
        if (!Objects.equals(this.rightBound, other.rightBound))
        {
            return false;
        }
        return true;
    } 
}
