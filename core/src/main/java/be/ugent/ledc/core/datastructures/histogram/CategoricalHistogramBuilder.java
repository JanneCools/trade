package be.ugent.ledc.core.datastructures.histogram;

import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds a categorical histogram by counting occurrences of each unique value.
 * The number of bins is ignored in this histogram builder
 * @author abronsel
 */
public class CategoricalHistogramBuilder implements HistogramBuilder<String, TypeContractor<String>>
{
    @Override
    public Histogram<String> buildHistogram(Dataset dataset, int bins, String a, TypeContractor<String> contractor)
    {
        return new CategoricalHistogram<>(
            dataset
                .stream()
                .filter(o -> o.get(a) != null)
                .map(o -> contractor.getFromDataObject(o, a)) //Get value
                .sorted()
                .map(v -> new Interval<>(v,v,false,false))    //Map to degenerate interval
                .collect(Collectors.groupingBy(
                    Function.identity(),
                    Collectors.counting())
                )
        );
    }
     
}
