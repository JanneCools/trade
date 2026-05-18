package be.ugent.ledc.core.datastructures.kdtree;

import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KDTreeFactory
{
    public static <N extends Number & Comparable<? super N>> KDTree<N> create(ContractedDataset dataset, TypeContractor<N> contractor)
    {
        return create(
            dataset,
            contractor,
            dataset
                .getContract()
                .getAttributes()
                .stream()
                .filter(a -> dataset
                        .getContract()
                        .getAttributeContract(a)
                        .equals(contractor)
                )
                .collect(Collectors.toSet())
        );
    }
    
    public static <N extends Number & Comparable<? super N>> KDTree<N> create(ContractedDataset dataset, TypeContractor<N> contractor, Set<String> indexAttributes)
    {
        System.out.println("Creating KD-tree...");
        KDTree<N> kDTree = new KDTree<>(contractor, indexAttributes);
        
        update(kDTree, dataset, 0);
        System.out.println("Done!");
        return kDTree;
    }

    private static <N extends Number & Comparable<? super N>> void update(KDTree<N> kDTree, Dataset dataset, int index)
    {
        if(dataset.getSize() == 0)
            return;
        
        String a = kDTree
            .getSplitSchedule()
            .get(index);
        
        List<DataObject> sortedData = dataset
            .stream()
            .sorted(DataObject.getProjectionComparator(a))
            .collect(Collectors.toList());
        
        //Find the object with median value
        DataObject medianObject = sortedData.get((sortedData.size()-1)/2);
        
        N median = kDTree
            .getIndexContractor()
            .getFromDataObject(medianObject, a);
        
        kDTree.insert(medianObject);
        
        int nextIndex = (index == kDTree.getSplitSchedule().size() - 1)
            ? 0
            : index + 1;
        
        //Update left
        update(
            kDTree,
            dataset
                .select(o -> o.get(a) != null && kDTree
                    .getIndexContractor()
                    .getFromDataObject(o, a).compareTo(median) < 0),
            nextIndex
        );
        
        //Update right
        update(
            kDTree,
            dataset
                .select(o -> o.get(a) != null && !o.equals(medianObject)
                    && kDTree
                    .getIndexContractor()
                    .getFromDataObject(o, a).compareTo(median) >= 0),
            nextIndex
        );
    }
}
