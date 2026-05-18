package be.ugent.ledc.chronos.algorithms.outliers;

import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.dino.outlierdetection.isolationforest.IsolationForestDetector;
import be.ugent.ledc.dino.outlierdetection.isolationforest.splitter.DoubleSplitter;
import java.util.Map;
import java.util.Set;

/**
 * A subs detector that performs a Isolation forest outlier detection.
 * @author abronsel
 * @param <I>
 */
public class IFSubsDetector <I extends Comparable<? super I>> extends SubsDetector<I>
{
    public IFSubsDetector(int window, double scoreThreshold)
    {
        super(window, scoreThreshold);
    }
    
    @Override
    public Map<DataObject, Double> processSubSignals(ContractedDataset data, Set<String> attributes)
    {
        return new IsolationForestDetector<>(
            new DoubleSplitter(),
            0.0).findOutliers(data, attributes);
    }
}
