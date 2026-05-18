package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A FixedTypeDataset is a ContractedDataset that requires all attributes to have
 * values of the same fixed type T. This might be necessary for example to model
 * a metric space, where all attributes take real numbers as values.
 * @author abronsel
 * @param <T> The type of values each attribute in this Dataset must take.
 */
public class FixedTypeDataset<T> extends ContractedDataset
{
    /**
     * The contractor that does not only dictate the type, but also the necessary operations
     */
    private final TypeContractor<T> fixedContractor;
    
    public FixedTypeDataset(Set<String> attributes, TypeContractor<T> contractor)
    {
        super(attributes.stream().collect(Collectors.toMap(a->a, a-> contractor)));
        this.fixedContractor = contractor;
    }

    public TypeContractor<T> getFixedContractor()
    {
        return fixedContractor;
    }    
}
