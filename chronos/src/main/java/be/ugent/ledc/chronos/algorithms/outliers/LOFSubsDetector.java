package be.ugent.ledc.chronos.algorithms.outliers;

import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.dino.outlierdetection.TreeNeighbourSearch;
import be.ugent.ledc.dino.outlierdetection.lof.LOFDetector;
import java.util.Map;
import java.util.Set;

/**
 * A subs detector that performs a LOF outlier detection.
 * The metric is used is the Euclidean distance on the metric spaces spanned by
 * the numerical attributes that are passed on.
 * @author abronsel
 * @param <I>
 */
public class LOFSubsDetector <I extends Comparable<? super I>> extends SubsDetector<I>
{
    private final int k;

    public LOFSubsDetector(int k, int window, double scoreThreshold)
    {
        super(window, scoreThreshold);
        this.k = k;
    }
    
    @Override
    public Map<DataObject, Double> processSubSignals(ContractedDataset data, Set<String> attributes)
    {
        return new LOFDetector(
            new TreeNeighbourSearch(
                attributes,
                data,
                TypeContractorFactory.DOUBLE),
            k,
            0.0
        )
        .findOutliers(data, attributes);
    }
}