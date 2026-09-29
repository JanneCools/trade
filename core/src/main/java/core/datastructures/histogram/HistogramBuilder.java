package core.datastructures.histogram;

import core.dataset.Dataset;
import core.dataset.contractors.TypeContractor;

public interface HistogramBuilder<D extends Comparable<? super D>, C extends TypeContractor<D>>
{
    Histogram<D> buildHistogram(Dataset dataset, int bins, String attribute, C contractor);
}
