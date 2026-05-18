package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.util.ItemSelector;
import be.ugent.ledc.sigma.datastructures.fptree.AttributeValue;
import be.ugent.ledc.sigma.datastructures.fptree.FPTree;
import be.ugent.ledc.sigma.datastructures.fptree.FPTreeFactory;
import be.ugent.ledc.sigma.datastructures.fptree.Itemset;
import be.ugent.ledc.sigma.datastructures.fptree.ItemsetConvertor;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.ValueSelector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ConditionalSequentialRepair is an extension of the SequentialRepair engine that
 * works in mostly the same way: it chooses a set cover and then repairs attributes
 * one by one. ConditionalSequentialRepair differs from the regular SequentialRepair
 * in the fact that selection of a set cover among the all minimal set covers is not random.
 * Random selection of set covers has been criticized by several authors because it can lead
 * in some case to repairs that would be very infrequent. The risk is hereby
 * that extensive repairing inflates frequencies of rare events. To deal with this issue,
 * selection of a set cover accounts for the frequency of the attributes that are kept.
 *  
 * @author abronsel
 */
public class ConditionalSequentialRepair extends SequentialRepair
{   
    private final int cleanDataCount;
    
    private final FPTree<AttributeValue<?>> fpTree;

    public ConditionalSequentialRepair(Map<String, ValueSelector> valueSelectors, SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior, Dataset cleanData)
    {
        super(valueSelectors, rules, costModel, nullBehavior);
        this.cleanDataCount = cleanData.getSize();
        this.fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
    }

    public ConditionalSequentialRepair(Map<String, ValueSelector> valueSelectors, SCCSigmaRuleset rules, ConstantCostModel costModel, Dataset cleanData)
    {
        super(valueSelectors, rules, costModel);
        this.cleanDataCount = cleanData.getSize();
        this.fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
    }
    
    public ConditionalSequentialRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior, Dataset cleanData) throws RepairException
    {
        super(rules, costModel, nullBehavior, cleanData);
        this.cleanDataCount = cleanData.getSize();
        this.fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
        
    }
    
    public ConditionalSequentialRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, Dataset cleanData) throws RepairException
    {
        super(rules, costModel, cleanData);
        this.cleanDataCount = cleanData.getSize();
        this.fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
    }

    public ConditionalSequentialRepair(SCCSigmaRuleset rules, Dataset cleanData) throws RepairException
    {
        super(rules, cleanData);
        this.fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
        this.cleanDataCount = cleanData.getSize();
    }

    @Override
    public Set<String> selectSolution(Set<Set<String>> minimalCovers, DataObject dirty)
    {
        if(minimalCovers.size() == 1)
            return minimalCovers.stream().findFirst().get();
        
        Map<Set<String>, Double> coverProbabilities = new HashMap<>();
        
        Set<String> allCoverAttributes = minimalCovers
            .stream()
            .flatMap(cover -> cover.stream())
            .collect(Collectors.toSet());
        
        Set<String> nonCoverAttributes = getRules()
            .getRules()
            .stream()
            .filter(rule -> rule.getInvolvedAttributes().size() > 1)
            .flatMap(rule -> rule.getInvolvedAttributes().stream())
            .distinct()
            .filter(a -> !allCoverAttributes.contains(a))
            .collect(Collectors.toSet());
        
        for(Set<String> cover: minimalCovers)
        {      
            double lift = 1.0;
            
            Set<String> nonCovered = new HashSet<>(allCoverAttributes);
            nonCovered.removeAll(cover);
            
            Itemset<AttributeValue<?>> nctemset = ItemsetConvertor.convert(dirty.project(nonCovered), true);
            
            int ncSupport = fpTree.support(nctemset);

            double ncProb = ((double)ncSupport) / ((double) cleanDataCount);
            
            for(String nca: nonCoverAttributes)
            {
                Set<String> combination = new HashSet<>(nonCovered);
                combination.add(nca);
                
                //Get the attributes kept under this cover and convert to an itemset
                Itemset<AttributeValue<?>> combinationItemset = ItemsetConvertor.convert(dirty.project(combination), true);
                Itemset<AttributeValue<?>> ncaItemset = ItemsetConvertor.convert(dirty.project(nca), true);

                int combinationSupport = fpTree.support(combinationItemset);
                int ncaSupport = fpTree.support(ncaItemset);

                double combinationProb = ((double)combinationSupport) / ((double) cleanDataCount);
                double ncaProb = ((double)ncaSupport) / ((double) cleanDataCount);
                
                if(Math.abs(ncProb * ncaProb) <= 0)
                    lift *= 0.5;
                else
                    lift *= combinationProb / (ncProb * ncaProb);
            }
            
            //Compute the count
            coverProbabilities.put(cover, lift);
           
        }
        
        //If no project object appears, then we choose at random
        if(coverProbabilities.isEmpty())
        {
            coverProbabilities.putAll(minimalCovers
            .stream()
            .collect(Collectors.toMap(cover -> cover, cover -> 1.0)));
        }
        
        //Normalize
        double sum = coverProbabilities.values().stream().mapToDouble(d->d).sum();
            
        for(Set<String> minimalCover: minimalCovers)
        {
            if(coverProbabilities.containsKey(minimalCover))
            {
                coverProbabilities.put(minimalCover, coverProbabilities.get(minimalCover)/sum);
            }
        }
//        System.out.println(coverProbabilities);
        
        //Draw a sample accounting for the count
        return ItemSelector.selectItemByProbability(coverProbabilities);
    }
}
