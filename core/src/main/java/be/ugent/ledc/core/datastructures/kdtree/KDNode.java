package be.ugent.ledc.core.datastructures.kdtree;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.operators.metric.Metric;
import java.util.List;
import java.util.Objects;

public class KDNode<N extends Number & Comparable<? super N>>
{
    private final DataObject point;
    
    private final String splitAttribute;
    
    private KDNode leftChild;
    
    private KDNode rightChild;
    
    private int count;

    public KDNode(DataObject point, String splitAttribute)
    {
        this.point = point;
        this.splitAttribute = splitAttribute;
        this.count = 1;
    }

    public DataObject getPoint()
    {
        return point;
    }

    public String getSplitAttribute()
    {
        return splitAttribute;
    }

    public KDNode getLeftChild()
    {
        return leftChild;
    }

    public KDNode getRightChild()
    {
        return rightChild;
    }

    public void setLeftChild(KDNode leftChild)
    {
        this.leftChild = leftChild;
    }

    public void setRightChild(KDNode rightChild)
    {
        this.rightChild = rightChild;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.point);
        hash = 41 * hash + Objects.hashCode(this.splitAttribute);
        hash = 41 * hash + Objects.hashCode(this.leftChild);
        hash = 41 * hash + Objects.hashCode(this.rightChild);
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
        final KDNode other = (KDNode) obj;
        if (!Objects.equals(this.splitAttribute, other.splitAttribute))
        {
            return false;
        }
        if (!Objects.equals(this.point, other.point))
        {
            return false;
        }
        if (!Objects.equals(this.leftChild, other.leftChild))
        {
            return false;
        }
        if (!Objects.equals(this.rightChild, other.rightChild))
        {
            return false;
        }
        return true;
    }

    public int getCount()
    {
        return count;
    }

    public void incrementCount()
    {
        this.count++;
    }
    
    public void insert(DataObject o, List<String> splitSchedule, TypeContractor<N> contractor)
    {
        //If the current node has a data object that equals o, insert it here again
        if(point.equals(o))
        {
            incrementCount();
            return;
        }
        
        int splitIndex = splitSchedule.indexOf(splitAttribute);
        
        if(splitIndex < 0)
            throw new DataException("Unexpected error in kdTree insert. Unknown split attribute '" + splitAttribute + "'");
        
        //Get attribute
        String a = splitSchedule.get(splitIndex);
        
        //Get the values
        N v = contractor.getFromDataObject(o, a);

        if(v == null)
            throw new DataException("Error in insert operation of kd-tree."
                + " Cause: null values are not allowed in kd-trees.");
        
        String nextAttribute = (splitIndex == splitSchedule.size() - 1)
            ? splitSchedule.get(0) 
            : splitSchedule.get(splitIndex+1);
        
        if(v.compareTo(contractor.getFromDataObject(point, a)) < 0)
        {
            if(getLeftChild() == null)
                setLeftChild(new KDNode<>(o, nextAttribute));
            else
                getLeftChild().insert(o, splitSchedule, contractor);
        }
        else
        {
            if(getRightChild() == null)
                setRightChild(new KDNode<>(o, nextAttribute));
            else
                getRightChild().insert(o, splitSchedule, contractor);
        }
            
    }
    
    public NearestNeighbours kNearest(DataObject query, Metric<Double, DataObject> metric, NearestNeighbours nn, TypeContractor<N> contractor, int k)
    {
        //Get the value of the query for the current dimension
        N queryValue = contractor.getFromDataObject(query, splitAttribute);
        N nodeValue = contractor.getFromDataObject(point, splitAttribute);
        
        //Count how many elements currently present
        int neighbours = nn.neighbourhoodSize();
        
        //Step 1: pruning condition
        Double dimDistance = Math.abs(queryValue.doubleValue() - nodeValue.doubleValue());
        
        boolean prune = neighbours == k
                    &&  dimDistance > nn.maxDistance();
        
        boolean pruneToLeft = queryValue.compareTo(nodeValue) >= 0;
        boolean pruneToRight = !pruneToLeft;
        
//        if(prune)
//        {
//            if(pruneToLeft)
//                System.out.println("Can prune left child of " + point);
//            else
//                System.out.println("Can prune right child of " + point);
//        }
        
        //Step 2: Update the nearest neighbour structure
        nn.update(
            point,
            metric.distance(query, point.project(query.getAttributes())),
            count);
        
        
        //Step 3: decide on which subtrees to process and in what order.
        
        //Return if this node has no children
        if(getLeftChild() == null && getRightChild() == null)
            return nn;
        //If only left child is null, explore right
        else if(getLeftChild() == null)
        {
            if(!(prune && pruneToRight))
                nn = getRightChild()
                    .kNearest(
                        query,
                        metric,
                        nn,
                        contractor,
                        k
                    );
        }
        //If only right child is null, explore left
        else if(getRightChild() == null)
        {
            if(!(prune && pruneToLeft))
                nn = getLeftChild()
                    .kNearest(
                        query,
                        metric,
                        nn,
                        contractor,
                        k
                    );
        }
        else if(queryValue.compareTo(nodeValue) < 0)
        {
            //Go left first: prune to left is always false here
            nn = getLeftChild()
                .kNearest(
                    query,
                    metric,
                    nn,
                    contractor,
                    k
                );
            if(!prune)
                nn = getRightChild()
                    .kNearest(
                        query,
                        metric,
                        nn,
                        contractor,
                        k
                    );
        }
        else
        {
            //Go right first: prune to right is always false here
            nn = getRightChild()
                .kNearest(
                    query,
                    metric,
                    nn,
                    contractor,
                    k
                );
            
            if(!prune)
                nn = getLeftChild()
                    .kNearest(
                        query,
                        metric,
                        nn,
                        contractor,
                        k
                    );
        }
        return nn;
    }
    
    public List<DataObject> radialSearch(DataObject query, Metric<Double, DataObject> metric, List<DataObject> neighborhood, TypeContractor<N> contractor, Double radius, BoundingBox<N> box)
    {
        Double queryPointDistance = metric.distance(query, point.project(query.getAttributes()));
        
        if(queryPointDistance.compareTo(radius) <= 0)
        {
            for(int i=0; i<count; i++)
                neighborhood.add(point);
        }
        
        N nodeValue = contractor.getFromDataObject(point, splitAttribute);
        
        BoundingBox lBox = box.leftUpdate(splitAttribute, nodeValue);
        BoundingBox rBox = box.rightUpdate(splitAttribute, nodeValue);
        
        if(getLeftChild() != null && lBox.overlapsWithSphere(query, radius, contractor))
            neighborhood = getLeftChild().radialSearch(
                query,
                metric,
                neighborhood,
                contractor,
                radius,
                lBox);
        
        if(getRightChild() != null && rBox.overlapsWithSphere(query, radius, contractor))
            neighborhood = getRightChild().radialSearch(
                query,
                metric,
                neighborhood,
                contractor,
                radius,
                rBox);
        
        return neighborhood;
    }
}
