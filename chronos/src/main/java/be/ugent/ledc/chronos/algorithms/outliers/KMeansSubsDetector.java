package be.ugent.ledc.chronos.algorithms.outliers;

import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.core.operators.metric.DataObjectMetrics;
import be.ugent.ledc.core.operators.metric.Metric;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * A subs detector that performs a K-means clustering on the data and computes scores
 * based on the distances to cluster centres.The metric is used is the Euclidean distance
 * on the metric spaces spanned by the numerical attributes that are passed on.
 * @author abronsel
 * @param <I>
 */
public class KMeansSubsDetector<I extends Comparable<? super I>> extends SubsDetector<I>
{
    private final int k;
    
    private final int iterations;
    
    private final Metric<Double, DataObject> metric = DataObjectMetrics.FAST_EUCLIDEAN;
    
    public KMeansSubsDetector(int k, int iterations, int window, double scoreThreshold)
    {
        super(window, scoreThreshold);
        this.k = k;
        this.iterations = iterations;
    }
    
    public KMeansSubsDetector(int k, int window, double scoreThreshold)
    {
        this(k, 100, window, scoreThreshold);
    }

    @Override
    public Map<DataObject, Double> processSubSignals(ContractedDataset data, Set<String> attributes)
    {
        List<Set<DataObject>> clusters = init(data);
        
        //Cluster
        for(int i=0; i<iterations; i++)
        {
            process(clusters, attributes);
        }
        
        //Compute means
        List<DataObject> means = means(clusters, attributes);
    
        Map<DataObject, Double> subSignalScores = new HashMap<>();
        
        //Compute scores
        for(int i=0;i<k; i++)
        {
            Set<DataObject> cluster = clusters.get(i);
            
            DataObject mean = means.get(i);
            
            for(DataObject o: cluster)
            {
                subSignalScores.put(o, metric.distance(mean, o.project(attributes)));
            }
        }
        
        return subSignalScores;
    }
    
    private List<Set<DataObject>> init(ContractedDataset data)
    {
        List<Set<DataObject>> clusters = new ArrayList<>();
        
        for(int i=1; i<=k; i++)
            clusters.add(new HashSet<>());
        
        //Assign each data point to a random cluster
        data
            .stream()
            .forEach(o -> clusters
                .get((int)Math.round(Math.random() * (k-1)))
                .add(o));
        
        return clusters;
    
    }
    
    private void process(List<Set<DataObject>> clusters, Set<String> attributes)
    {
        List<DataObject> clusterMeans = means(clusters, attributes);
        
        Map<DataObject, Integer> assignments = new HashMap<>();
        
        //Compute assignments and register differences
        for(int c=0; c<clusters.size(); c++)
        {
            Set<DataObject> cluster = clusters.get(c);
            
            for(DataObject o: cluster)
            {
                int newCluster = IntStream
                    .range(0, k)
                    .mapToObj(i -> new Pair<Integer,Double>(
                        i,
                        this.metric.distance(
                            clusterMeans.get(i),
                            o.project(attributes))
                    ))
                    .sorted(Comparator.comparing(Pair::getSecond))
                    .map(Pair::getFirst)
                    .findFirst()
                    .get();
                
                if(newCluster != c)
                    assignments.put(o, c);
                
            }
        }
        
        //In each cluster: remove objects that are scheduled for re-assignment
        clusters
            .stream()
            .forEach(cluster -> cluster.removeIf(assignments::containsKey));
            
        //Assign objects to their new clusters
        assignments
            .entrySet()
            .stream()
            .forEach(e -> clusters.get(e.getValue()).add(e.getKey()));
        
    }
    
    private List<DataObject> means(List<Set<DataObject>> clusters, Set<String> attributes)
    {
        List<DataObject> means = new ArrayList<>();
        
        for(Set<DataObject> cluster: clusters)
        {
            DataObject mean = new DataObject();
            
            for(String a: attributes)
            {
                mean.setDouble(
                    a,
                    cluster
                        .stream()
                        .mapToDouble(o -> o.getDouble(a))
                        .average()
                        .orElse(0.0)
                );
            }
            
            means.add(mean);
        }
            
        return means;
    }
}
