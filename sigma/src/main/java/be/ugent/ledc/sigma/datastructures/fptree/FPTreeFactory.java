package be.ugent.ledc.sigma.datastructures.fptree;

import java.util.*;
import java.util.stream.Collectors;

public class FPTreeFactory {

    public static <I> FPTree<I> makeTree(List<Itemset<I>> dataset, int minimumSupportCount) {
        Map<Itemset<I>, Integer> datasetAsMap = new HashMap<>();

        for (Itemset<I> itemset : dataset) {
            datasetAsMap.merge(itemset, 1, Integer::sum);
        }

        return makeTree(datasetAsMap, minimumSupportCount);
    }

    /**
     * Create an FPTree object from given MapDataset object where items are only inserted if their count is higher than
     * countLowerBound
     *
     * @param dataset             MapDataset object to create FPTree object from
     * @param minimumSupportCount minimum count threshold for items to be included in the FPTree object
     * @param <I>                 datatype of the FPNode's items in the FPTree object
     * @return FPTree object created from given MapDataset object where items are only inserted if their count is higher
     * than countLowerBound
     */
    public static <I> FPTree<I> makeTree(Map<Itemset<I>, Integer> dataset, int minimumSupportCount) {
        // Generate item frequency table consisting of all items appearing in the Dataset object mapped to their
        // support if their support is higher than countLowerBound. The table is sorted decreasingly according to
        // the support count of the items
        LinkedHashMap<I, Integer> itemFrequencyTable = getFrequentItemsInOrder(dataset, minimumSupportCount);

        // Create an FPTree object with root FPNode object with null value
        FPTree<I> fpTree = new FPTree<>(new ArrayList<>(itemFrequencyTable.keySet()));

        //Build the tree
        fpTree.build(dataset, itemFrequencyTable);

        return fpTree;

    }

    /**
     * Given an FPTree object and an item, build a conditional FPTree consisting of all itemsets containing given item
     *
     * @param fpTree              original FPTree object to build conditional FPTree from with given item
     * @param item                item that should be included in all itemsets of conditional FPTree created from original FPTree object
     * @param minimumSupportCount only include itemsets with a support larger than this count
     * @param <I>                 datatype of the items
     * @return Conditional FPTree consisting of all itemsets containing given item created from given FPTree object
     */
    public static <I> FPTree<I> createConditionalFPT(FPTree<I> fpTree, I item, int minimumSupportCount) {

        // Make a new dataset from the current FPTree, conditioned on item 'item'
        Map<Itemset<I>, Integer> conditionalDataset = new HashMap<>();

        // Get all the paths of FPNode objects containing this item to the root of the FPTree object
        List<List<FPNode<I>>> paths = fpTree.getPathsToRoot(item);

        for (List<FPNode<I>> path : paths) {

            if (path.size() >= 2) {

                // Get path count
                int pathCount = path.get(0).getCount();

                // Drop first and last node (this is the same for all paths and is added to the suffix in the search algorithm)
                path.remove(0);
                path.remove(path.size() - 1);

                // Create new Itemset object
                Itemset<I> itemset = new Itemset<>();

                // Add path nodes to itemset
                for (FPNode<I> pathNode : path) {
                    itemset.add(pathNode.getItem());
                }

                // Add this itemset as much as indicated by the count of the path
                conditionalDataset.merge(itemset, pathCount, Integer::sum);
            }

        }

        return FPTreeFactory.makeTree(conditionalDataset, minimumSupportCount);

    }

    public static <I> FPTree<I> createConditionalFilteredFPT(FPTree<I> fpTree, I item, int minimumSupportCount, LinkedHashMap<I, Integer> datasetOneItems, int filterCount) {

        // Make a new dataset from the current FPTree, conditioned on item 'item'
        Map<Itemset<I>, Integer> conditionalDataset = new HashMap<>();

        // Get all the paths of FPNode objects containing this item to the root of the FPTree object
        List<List<FPNode<I>>> paths = fpTree.getPathsToRoot(item);

        for (List<FPNode<I>> path : paths) {

            if (path.size() >= 2) {

                // Get path count
                int pathCount = path.get(0).getCount();

                // Drop first and last node (this is the same for all paths and is added to the suffix in the search algorithm)
                path.remove(0);
                path.remove(path.size() - 1);

                path = path.stream().filter(n -> datasetOneItems.get(n.getItem()) >= filterCount).collect(Collectors.toList());

                // Create new Itemset object
                Itemset<I> itemset = new Itemset<>();

                // Add path nodes to itemset
                for (FPNode<I> pathNode : path) {
                    itemset.add(pathNode.getItem());
                }

                if (!itemset.isEmpty()) {
                    // Add this itemset as much as indicated by the count of the path
                    conditionalDataset.merge(itemset, pathCount, Integer::sum);
                }
            }
        }

        return FPTreeFactory.makeTree(conditionalDataset, minimumSupportCount);

    }

    public static <I> LinkedHashMap<I, Integer> getFrequentItemsInOrder(Map<Itemset<I>, Integer> dataset, int minimumSupportCount) {

        Map<I, Integer> tempMapping = new HashMap<>();

        // Loop over all Itemset objects in the Dataset object
        for (Map.Entry<Itemset<I>, Integer> entry : dataset.entrySet()) {
            Integer value = entry.getValue();
            // Loop over all items in the Itemset object
            for (I item : entry.getKey()) {
                tempMapping.merge(item, value, Integer::sum);
            }
        }

        return sortMap(tempMapping, minimumSupportCount);
    }

    public static <I> LinkedHashMap<I, Integer> getFrequentItemsInOrder(List<Itemset<I>> dataset, int minimumSupportCount) {

        Map<I, Integer> tempMapping = new HashMap<>();

        // Loop over all Itemset objects in the Dataset object
        for (Itemset<I> entry : dataset) {
            // Loop over all items in the Itemset object
            for (I item : entry) {
                tempMapping.merge(item, 1, Integer::sum);
            }
        }

        return sortMap(tempMapping, minimumSupportCount);

    }

    private static <I> LinkedHashMap<I, Integer> sortMap(Map<I, Integer> tempMapping, int minimumSupportCount) {

        List<Map.Entry<I, Integer>> tempList = new ArrayList<>(tempMapping.entrySet());

        Collections.sort(tempList, (e1, e2) -> (-1) * e1.getValue().compareTo(e2.getValue()));

        LinkedHashMap<I, Integer> map = new LinkedHashMap<>();

        for (Map.Entry<I, Integer> entrySet : tempList) {
            if (entrySet.getValue() >= minimumSupportCount) {
                map.put(entrySet.getKey(), entrySet.getValue());
            }
        }

        return map;

    }

}
