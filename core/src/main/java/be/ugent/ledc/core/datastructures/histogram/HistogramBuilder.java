package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;

public interface HistogramBuilder<D extends Comparable<? super D>, C extends TypeContractor<D>>
{
    Histogram<D> buildHistogram(Dataset dataset, int bins, String attribute, C contractor);
}
