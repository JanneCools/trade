package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.Atom;
import be.ugent.ledc.sigma.datastructures.atoms.VariableAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;
import be.ugent.ledc.sigma.repair.selection.CPFRandomRepairSelection;
import be.ugent.ledc.sigma.repair.selection.CPFRepairSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation that:
 * (i) internally transforms the set of SigmaRules into a positive
 * set of CPF formulas 
 * 
 * (ii) constructs a compact representation of all minimal cost repairs
 * in the form of a ChangeExpression.
 * 
 * The second property is a strong property whenever the set of minimal-cost repairs
 * is large and sampling one particular values might not be very meaningful.
 * 
 * @author abronsel
 * @param <M> 
 */
public abstract class CPFRepairEngine<M extends CostModel<?>> extends ObjectLevelRepairEngine<M,SigmaRuleset>
{
    public enum AttributesToKeep
    {
        ALL,
        MINIMAL_SETS,
        MAXIMAL_SETS
    }
    
    private final CPFRepairSelection selector;
    
    private final Set<CPF> cpfs;
    protected Set<CPF> updatedCPFs;

    public CPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, M costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException {
        super(sigmaRuleset, costModel, nullBehavior);

        this.cpfs = cpfs;
        this.updatedCPFs = cpfs == null ? null : new HashSet<>(cpfs);

        this.selector = selector;
    }

    public CPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, M costModel) throws RepairException {
        this(
                cpfs,
                sigmaRuleset,
                costModel,
                NullBehavior.CONSTRAINED_REPAIR, // Default behavior
                new CPFRandomRepairSelection()   // Default behavior
        );
    }

    public CPFRepairEngine(SigmaRuleset sigmaRuleset, M costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException
    {
        super(sigmaRuleset, costModel, nullBehavior);
        
        this.cpfs = SigmaRulesetInverter.invert(sigmaRuleset);
        this.updatedCPFs = new HashSet<>(this.cpfs);

        this.selector = selector;
    }

    public CPFRepairEngine(SigmaRuleset sigmaRuleset, M costModel) throws RepairException
    {
        this(
            sigmaRuleset,
            costModel,
            NullBehavior.CONSTRAINED_REPAIR, //Default behavior
            new CPFRandomRepairSelection()   //Default behavior
        );
    }

    public Set<CPF> getCpfs()
    {
        return cpfs;
    }
    
    public Map<String, SigmaContractor<?>> getContractors()
    {
        return getRules().getContractors();
    }

    public  SigmaContractor<?> getContractor(String attribute)
    {
        return getContractors().get(attribute);
    }

    public CPFRepairSelection getSelector()
    {
        return selector;
    }

    public void updateCPFs(Set<CPF> newCPFs) {
        this.updatedCPFs = newCPFs;
    }

    public void resetUpdatedCPFs()
    {
        this.updatedCPFs = cpfs == null ? null : new HashSet<>(this.cpfs);
    }

    @Override
    public DataObject repair(DataObject dirty) throws RepairException
    {
        // fix attributes without a cost function
        updatedCPFs = fixAttributes(this.cpfs, dirty).stream()
                .filter(cpf -> !cpf.getAtoms().contains(AbstractAtom.ALWAYS_FALSE))
                .collect(Collectors.toSet());
        DataObject clean = selector.selectRepair(getMinCostChangeExpressions(dirty), dirty);
        resetUpdatedCPFs();
        return clean;
    }

    public Pair<Integer, DataObject> repairWithCost(DataObject dirty) throws RepairException
    {
        // fix attributes without a cost function
        updatedCPFs = fixAttributes(this.cpfs, dirty).stream()
                .filter(cpf -> !cpf.getAtoms().contains(AbstractAtom.ALWAYS_FALSE))
                .collect(Collectors.toSet());
        Pair<Integer, Set<CPF>> minCostChangeExpressions = getMinCostChangeExpressionsWithCost(dirty);
        DataObject repaired = selector.selectRepair(minCostChangeExpressions.getSecond(), dirty);
        resetUpdatedCPFs();
        return new Pair<>(minCostChangeExpressions.getFirst(), repaired);
    }

    public Map<CPF, Set<CPF>> search(DataObject dataObject, AttributesToKeep attributesToKeep)
    {
        Map<CPF, Set<CPF>> changeExpressions = new HashMap<>();

        for (CPF cpf : updatedCPFs)
        {

            Set<AbstractAtom<?, ?, ?>> cpfFailedAtoms = getFailedAtoms(cpf, dataObject);
            
            Set<Set<String>> cpfAttributesToChange = getAttributesToChange(cpfFailedAtoms, dataObject, attributesToKeep);

            if (cpfAttributesToChange.isEmpty())
                continue;

            Set<CPF> cpfChangeExpression = getChangeExpressions(cpfAttributesToChange, cpf, dataObject);
            changeExpressions.put(cpf, cpfChangeExpression);

        }

        return changeExpressions;

    }

    public int getMinCost(DataObject dataObject) throws RepairException
    {
        return getMinCostChangeExpressionsWithCost(dataObject).getFirst();
    }

    public Set<CPF> getMinCostChangeExpressions(DataObject dataObject) throws RepairException
    {
        return getMinCostChangeExpressionsWithCost(dataObject).getSecond();
    }
    
    public abstract Pair<Integer, Set<CPF>> getMinCostChangeExpressionsWithCost(DataObject dataObject) throws RepairException;
    
    private Set<AbstractAtom<?, ?, ?>> getFailedAtoms(CPF cpf, DataObject dataObject)
    {

        Set<String> nonNullAttributes = dataObject
                .getAttributes()
                .stream()
                .filter(a -> dataObject.get(a) != null)
                .collect(Collectors.toSet());

        return cpf.project(nonNullAttributes)
                .getAtoms()
                .stream()
                .filter(a -> !a.test(dataObject))
                .collect(Collectors.toSet());
    }

    private Set<Set<String>> getAttributesToChange(Set<AbstractAtom<?, ?, ?>> failedAtoms, DataObject dataObject, AttributesToKeep attributesToKeep)
    {

        Set<VariableAtom<?, ?, ?>> failedVariableAtoms = failedAtoms
                .stream()
                .filter(a -> a instanceof VariableAtom<?, ?, ?>)
                .map(a -> (VariableAtom<?, ?, ?>) a)
                .collect(Collectors.toSet());

        Set<AbstractAtom<?, ?, ?>> failedOtherAtoms = failedAtoms
                .stream()
                .filter(a -> !(a instanceof VariableAtom<?, ?, ?>))
                .collect(Collectors.toSet());

        Set<String> otherAtomAttributes = failedOtherAtoms.stream().flatMap(Atom::getAttributes).collect(Collectors.toSet());

        Set<String> variableAtomAttributes = failedVariableAtoms.stream().flatMap(Atom::getAttributes).collect(Collectors.toSet());
        Set<Set<String>> attributesToChange = new HashSet<>();

        generateVariableAtomCovers(0, new HashSet<>(), new ArrayList<>(variableAtomAttributes), failedVariableAtoms, attributesToChange);

        for (Set<String> attributes : attributesToChange) {
            attributes.addAll(otherAtomAttributes);
        }

        addNullAttributes(attributesToChange, dataObject);

        return switch (attributesToKeep) {
            case MAXIMAL_SETS -> attributesToChange
                    .stream()
                    .filter(ats1 -> attributesToChange.stream().noneMatch(ats2 -> !ats1.equals(ats2) && ats2.containsAll(ats1)))
                    .collect(Collectors.toSet());
            case MINIMAL_SETS -> attributesToChange
                    .stream()
                    .filter(ats1 -> attributesToChange.stream().noneMatch(ats2 -> !ats1.equals(ats2) && ats1.containsAll(ats2)))
                    .collect(Collectors.toSet());
            case ALL -> attributesToChange;
        };

    }

    private void generateVariableAtomCovers(int index, Set<String> currentCover, List<String> attributes, Set<VariableAtom<?, ?, ?>> variableAtoms, Set<Set<String>> covers)
    {

        if (index == attributes.size()) {
            if (variableAtoms.stream().allMatch(a -> a.getAttributes().anyMatch(currentCover::contains))) {
                covers.add(new HashSet<>(currentCover));
            }
            return;
        }

        currentCover.add(attributes.get(index));
        generateVariableAtomCovers(index + 1, currentCover, attributes, variableAtoms, covers);

        currentCover.remove(attributes.get(index));
        generateVariableAtomCovers(index + 1, currentCover, attributes, variableAtoms, covers);

    }

    private void addNullAttributes(Set<Set<String>> attributesToChange, DataObject dataObject)
    {

        Set<String> nullAttributes = dataObject
                .getAttributes()
                .stream()
                .filter(at -> dataObject.get(at) == null && getNullBehavior().repairIfNull(getRules(), at))
                .collect(Collectors.toSet());

        for (Set<String> attributes : attributesToChange)
        {
            attributes.addAll(nullAttributes);
        }

    }

    private Set<CPF> getChangeExpressions(Set<Set<String>> attributesToChange, CPF cpf, DataObject dataObject)
    {
        Set<CPF> changeExpressions = new HashSet<>();

        for (Set<String> attributes : attributesToChange) {

            Set<AbstractAtom<?, ?, ?>> changeExpressionAtoms = new HashSet<>();

            for (AbstractAtom<?, ?, ?> cpfAtom : cpf.getAtoms()) {

                if (cpfAtom.getAttributes().allMatch(at -> attributes.stream().anyMatch(at::equals))) {
                    changeExpressionAtoms.add(cpfAtom);
                } else if (cpfAtom.getAttributes().anyMatch(at -> attributes.stream().anyMatch(at::equals))) {

                    if (cpfAtom instanceof VariableAtom<?, ?, ?> cpfVariableAtom) {

                        String nonMatchingAttribute = attributes.contains(cpfVariableAtom.getLeftAttribute()) ?
                                cpfVariableAtom.getRightAttribute() : cpfVariableAtom.getLeftAttribute();

                        DataObject project = dataObject.project(nonMatchingAttribute);

                        changeExpressionAtoms.add(cpfAtom.fix(project));

                    }

                }
            }

            changeExpressions.add(CPFImplicator.imply(new CPF(changeExpressionAtoms)));

        }

        return changeExpressions;

    }

    private Set<CPF> fixAttributes(Set<CPF> cpfs, DataObject dataObject) {
        final Set<String> allAttributes = cpfs.stream()
                .map(CPF::getAttributes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        final Set<String> unrepairableAttributes = allAttributes.stream()
                .filter(attr ->
                        getCostModel().getCostFunction(attr) == null ||
                                (dataObject.get(attr) == null && !getNullBehavior().repairIfNull(getRules(), attr)))
                .collect(Collectors.toSet());

        final DataObject projected = dataObject.project(unrepairableAttributes);

        // extract and fix all unique atoms
        Map<Integer, AbstractAtom<?,?,?>> uniqueAtoms = new HashMap<>();
        Set<CPF> updatedCPFs = new HashSet<>();
        for (CPF cpf : cpfs) {
            Set<AbstractAtom<?,?,?>> fixedAtoms = new HashSet<>(cpf.getAtoms().size());
            for (AbstractAtom<?,?,?> atom : cpf.getAtoms()) {
                int hashcode = atom.hashCode();
                AbstractAtom<?,?,?> fixedAtom = uniqueAtoms.get(hashcode);
                if (fixedAtom == null) {
                    // set to True if an unrepairable attribute is null or if a null attribute should not be repaired
                    fixedAtom = atom.getAttributes().anyMatch(a -> (unrepairableAttributes.contains(a) && projected.get(a) == null))
                            ? AbstractAtom.ALWAYS_TRUE
                            : atom.fix(projected);
                    uniqueAtoms.put(hashcode, fixedAtom);
                }
                fixedAtoms.add(fixedAtom);
            }
            updatedCPFs.add(new CPF(fixedAtoms));
        }

        return updatedCPFs;
    }
}
