package be.ugent.ledc.sigma.repair.cost.iterators;

import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import java.util.TreeSet;

public class SortedSetRepairNavigator<T extends Comparable<? super T>> implements ForwardNavigator<T>
{
    private final TreeSet<T> navigatorSet;

    public SortedSetRepairNavigator(TreeSet<T> navigatorSet)
    {
        this.navigatorSet = navigatorSet;
    }
    
    @Override
    public boolean hasNext(T current)
    {
        return this.navigatorSet.higher(current) != null;
    }

    @Override
    public T next(T current)
    {
        return this.navigatorSet.higher(current);
    }

    @Override
    public T first()
    {
        return this.navigatorSet.first();
    }
}
