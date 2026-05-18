package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.util.ItemSelector;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.fptree.*;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.NoDonorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FrequencyDonorSelector implements DonorSelector {

    private final Set<String> rulesetAttributes;
    private final FPTree<AttributeValue<?>> fpTree;

    public FrequencyDonorSelector(Dataset cleanData, SCCSigmaRuleset rules) throws RepairException {

        if(cleanData.getDataObjects().stream().anyMatch(o -> rules.getRules().stream().anyMatch(r -> !r.isSatisfied(o))))
        {
            throw new RepairException("The given clean do not satisfy all rules. Cannot compute repairs.");
        }

        this.rulesetAttributes = rules.getContractors().keySet();
        fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rulesetAttributes), true), 1);
    }

    @Override
    public DataObject selectDonor(Set<String> solution, DataObject dirtyOriginal, Map<String, Set<AbstractAtom<?, ?, ?>>> permittedValues, Set<SigmaRule> variableConditions) throws NoDonorException {

        //Select donor tuples
        Map<DataObject, Integer> donors = new HashMap<>();

        //Iterate over paths in FP tree
        for(FPNode<AttributeValue<?>> leafNode : fpTree.getLeafNodes())
        {
            //Select a path
            List<FPNode<AttributeValue<?>>> path = fpTree.getPathToRoot(leafNode);

            //Remove root node
            path.remove(path.size()-1);

            //If the path matches the value sets for attributes that are not imputed and has no null values for the attributes to impute, we add it as donor
            if(solution.stream().allMatch(attribute -> path.stream().anyMatch(node -> node.getItem().getAttribute().equals(attribute) && node.getItem().getValue() != null)) && matches(permittedValues, path))
            {
                List<AttributeValue<?>> potentialDonorAttributeValues = path
                        .stream()
                        .filter(node -> solution.contains(node.getItem().getAttribute())) //Keep only those nodes that involve an attribute that needs to be imputed
                        .map(FPNode::getItem)
                        .collect(Collectors.toList());

                DataObject potentialDonor = new DataObject();

                for(AttributeValue<?> attributeValue:  potentialDonorAttributeValues)
                {
                    potentialDonor.set(attributeValue.getAttribute(), attributeValue.getValue());
                }

                if (variableConditions.stream().noneMatch(r -> r.isFailed(potentialDonor))) {
                    donors.merge(potentialDonor, path.get(0).getCount(), Integer::sum);
                }


            }
        }

        if(donors.isEmpty())
        {
            //Need sequential imputation in this case
            throw new NoDonorException("The joint imputation strategy could not find any donors. Attribute value sets are: " + permittedValues.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n", "\n","")));
        }

        return ItemSelector.selectItemByCount(donors);

    }

    private boolean matches(Map<String, Set<AbstractAtom<?, ?, ?>>> attributeValueMap, List<FPNode<AttributeValue<?>>> path)
    {
        DataObject o = new DataObject();

        for(String a: attributeValueMap.keySet())
        {
            if(path.stream().noneMatch(n -> n.getItem().getAttribute().equals(a)))
                return false;

            o.set(a, path
                    .stream()
                    .filter(n -> n.getItem().getAttribute().equals(a))
                    .findFirst()
                    .get()
                    .getItem()
                    .getValue());
        }

        return attributeValueMap
                .values()
                .stream()
                .allMatch(atomSet -> atomSet
                        .stream()
                        .allMatch(atom -> atom.test(o))
                );
    }

    public Set<String> getRulesetAttributes() {
        return this.rulesetAttributes;
    }

}
