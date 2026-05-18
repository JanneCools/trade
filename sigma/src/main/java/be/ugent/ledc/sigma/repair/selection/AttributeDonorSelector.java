package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.util.ItemSelector;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.NoDonorException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributeDonorSelector extends FrequencyDonorSelector {

    private final Set<String> attributes;
    private final Map<DataObject, Set<DataObject>> potentialDonorMapping;

    public AttributeDonorSelector(Dataset cleanData, SCCSigmaRuleset rules, Set<String> attributes) throws RepairException {

        super(cleanData, rules);

        if(cleanData.getDataObjects().stream().anyMatch(o -> rules.getRules().stream().anyMatch(r -> !r.isSatisfied(o))))
        {
            throw new RepairException("The given clean do not satisfy all rules. Cannot compute repairs.");
        }

        if (cleanData.getDataObjects().stream().anyMatch(o -> !o.getAttributes().containsAll(attributes)))
        {
            throw new RepairException("Any of the attributes in the given set of attributes does not appear in the clean data.");
        }

        this.attributes = attributes;
        this.potentialDonorMapping = new HashMap<>();

        for (DataObject cleanObject : cleanData) {

            DataObject attributeObject = new DataObject();

            for (String attribute : attributes) {
                attributeObject.set(attribute, cleanObject.get(attribute));
            }

            potentialDonorMapping.computeIfAbsent(attributeObject, os -> new HashSet<>()).add(cleanObject);

        }

    }

    @Override
    public DataObject selectDonor(Set<String> solution, DataObject dirtyOriginal, Map<String, Set<AbstractAtom<?, ?, ?>>> permittedValuesMapping, Set<SigmaRule> variableConditions) throws NoDonorException {

        Map<DataObject, Integer> donors = new HashMap<>();

        Set<DataObject> potentialDonors = potentialDonorMapping.get(dirtyOriginal.project(attributes));

        if (potentialDonors == null) {
            return super.selectDonor(solution, dirtyOriginal, permittedValuesMapping, variableConditions);
        }

        for (DataObject potentialDonor : potentialDonors) {

            DataObject currentPotentialDonor = potentialDonor.project(getRulesetAttributes());

            if (currentPotentialDonor.getAttributes().stream().anyMatch(at -> currentPotentialDonor.get(at) == null)) {
                continue;
            }

            if (permittedValuesMapping.values().stream().allMatch(atomSet -> atomSet.stream().allMatch(atom -> atom.test(currentPotentialDonor))) && variableConditions.stream().noneMatch(r -> r.isFailed(currentPotentialDonor))) {
                donors.merge(currentPotentialDonor.project(solution), 1, Integer::sum);
            }

        }

        if (donors.isEmpty()) {
            return super.selectDonor(solution, dirtyOriginal, permittedValuesMapping, variableConditions);
        } else {
            return ItemSelector.selectItemByCount(donors);
        }

    }

}
