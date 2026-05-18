package be.ugent.ledc.core.datastructures.kdtree;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.operators.metric.Metric;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of K-D trees where points are DataObjects that are contracted
 * in such a way they contain comparable attributes
 * @author abronsel
 * @param <N>
 */
public class KDTree<N extends Number & Comparable<? super N>>
{
    private KDNode<N> root;
    
    private final List<String> splitSchedule;
    
    private final TypeContractor<N> indexContractor;

    public KDTree(TypeContractor<N> indexContractor, Set<String> indexAttributes)
    {   
        if(indexAttributes.isEmpty())
            throw new DataException("Cannot initialize kd-tree. "
                + "Cause: no attributes to index.");

        this.indexContractor = indexContractor;
        this.splitSchedule = indexAttributes
            .stream()
            .sorted()
            .collect(Collectors.toList());
    }

    public KDNode<N> getRoot()
    {
        return root;
    }

    public void setRoot(KDNode<N> root)
    {
        this.root = root;
    }

    public TypeContractor<N> getIndexContractor()
    {
        return indexContractor;
    }

    public List<String> getSplitSchedule()
    {
        return splitSchedule;
    }
    
    public void insert(DataObject o)
    {
        if(root == null)
            root = new KDNode<>(o, splitSchedule.get(0));
        else
            root.insert(o, splitSchedule, indexContractor);
    }
    
    public DataObject nearest(DataObject query, Metric<Double, DataObject> metric)
    {
        return kNearest(query, metric, 1).get(0);
    }
    
    /**
     * Returns a list of the k points in the KDTree that (i) are different from the query point
     * on at least one attribute and (ii) lie closest to the query point, where distance is calculated by a given metric.
     * 
     * Note: the metric will be applied to projections of objects to attributes that are indexed
     * in the KD tree. So "closest" in condition (ii) applies only to attributes in the KDTree, but "different"
     * in condition (i) applies to all attributes.
     * @param query
     * @param metric
     * @param k
     * @return 
     */
    public List<DataObject> kNearest(DataObject query, Metric<Double, DataObject> metric, int k)
    {
        //Sanity check
        if(root == null || query == null)
            return null;
        
        return root
        .kNearest(
            query.project(splitSchedule),
            metric,
            new NearestNeighbours(k),
            indexContractor,
            k)
        .getNeighbours()
        .stream()
        .filter(nn -> !nn.equals(query))
        .collect(Collectors.toList());
    }
    
    /**
     * Returns the set of points that lie in a hypersphere around the given center point and with a given radius.
     * Only points that (i) are different from the center point on at least one attribute are included.
     * 
     * Note: the metric will be applied to projections of objects to attributes that are indexed
     * in the KD tree.
     * @param center
     * @param metric
     * @param radius
     * @return 
     */
    public List<DataObject> radialSearch(DataObject center, Double radius, Metric<Double, DataObject> metric)
    {
        //Sanity check
        if(root == null || center == null)
            return new ArrayList<>();
        
        return root
        .radialSearch(
            center.project(splitSchedule),
            metric,
            new ArrayList<>(),
            indexContractor,
            radius,
            new BoundingBox<>(splitSchedule))
        .stream()
        .filter(nn -> !nn.equals(center))
        .collect(Collectors.toList());
    }
}
