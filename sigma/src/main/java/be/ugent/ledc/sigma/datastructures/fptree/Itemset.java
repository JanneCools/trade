package be.ugent.ledc.sigma.datastructures.fptree;

import java.util.*;

/**
 * This class represents a set of items
 *
 * @param <I> datatype of the items in the Itemset
 */
public class Itemset<I> extends HashSet<I> {

    /**
     * Create a new Itemset object from a given HashSet object
     *
     * @param c HashSet object from which an Itemset object should be created
     */
    public Itemset(Collection<? extends I> c) {
        super(c);
    }
    
    /**
     * Empty constructor
     */
    public Itemset() {
    }

    /**
     * Create an Itemset from an array of items
     *
     * @param items items of the Itemset
     */
    public Itemset(I ... items) {
        this(Arrays.asList(items));
    }

    /**
     * Compute the union of this Itemset object with the given Itemset object
     *
     * @param other Itemset object which should be taken the union from with this Itemset object
     * @return union of this Itemset object with the given Itemset object
     */
    public Itemset<I> union(Itemset<I> other) {
        Itemset<I> itemset = new Itemset<>();

        itemset.addAll(this);
        itemset.addAll(other);

        return itemset;
    }

    /**
     * Compute the intersection of this Itemset object with the given Itemset object
     * @param other Itemset object which should be intersected with this Itemset object
     * @return intersection of this Itemset object with the given Itemset object
     */
    public Itemset<I> intersection(Itemset<I> other) {

        Itemset<I> itemset = new Itemset<>(this);

        itemset.retainAll(other);

        return itemset;

    }

    /**
     * Get all subsets of this Itemset object
     * @return List of Itemset objects which are the subsets of this Itemset object
     */
    public List<Itemset<I>> getSubSets() {
        int n = size();
        List<I> list = new ArrayList(this);

        List<Itemset<I>> subsets = new ArrayList<>();

        for (int i = 0; i < (1 << n); i++) {
            Itemset<I> subset = new Itemset<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    subset.add(list.get(j));
                }
            }
            subsets.add(subset);
        }

        return subsets;
    }

}
