package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Contract
{
    private final Map<String, TypeContractor<?>> contractors;

    private Contract(Map<String, TypeContractor<?>> contractors)
    {
        this.contractors = contractors;
    }
    
    /**
     * Verify if this object satisfies all predicates required by this Contract
     * on behalf of it's value for a specific attribute.
     * 
     * If the attribute is unknown to the contract, the result of verification is false
     * as only contracted attributes are allowed in a contracted dataset.
     * @param o The DataObject that is tested 
     * @param attribute The attribute for which we verify.
     * @return 
     */
    public boolean verify(DataObject o, String attribute)
    {
        if(contractors.get(attribute) == null)
            return false;
        
        return contractors.get(attribute).test(o.get(attribute));
    }
    
    /**
     * Verify if this object satisfies all predicates required by this Contract
     * for all it's attributes.
     * @param dataObject The DataObject that is tested 
     * @return 
     */
    public boolean verify(DataObject dataObject)
    {
        if(dataObject == null)
            return false;
                
        //If the dataobject has an uncontracted attribute, return false
        if(dataObject.getAttributes().stream().anyMatch(a -> contractors.get(a) == null))
            return false;
        
        for(String a: contractors.keySet())
        {
            if(!verify(dataObject,a))
                System.out.println("Object " + dataObject.project(a) + " fails for " + a + " on contract " + contractors.get(a).name());
        }
        
        return contractors
            .keySet()
            .stream()
            .allMatch(a -> verify(dataObject,a));
    }
    
    public Set<String> getAttributes()
    {
        return new HashSet<>(this.contractors.keySet());
    }
    
    public TypeContractor<?> getAttributeContract(String attribute)
    {
        return this.contractors.get(attribute);
    }
    
    /**
     * We user the Builder design pattern to ensure that once the contract is built,
     * no changes can be made to the Contract. This way, the Contract definition
     * can be made once and only once.
     */
    public static class ContractBuilder
    {
        private final Map<String, TypeContractor<?>> contractors;
        
        public ContractBuilder()
        {
            this.contractors = new HashMap<>();
        }
        
        public ContractBuilder(Contract c)
        {
            this.contractors = new HashMap<>();
            
            for(String a: c.getAttributes())
            {
                this.addContractor(a, c.getAttributeContract(a));
            }
        }
        
        public final ContractBuilder addContractor(String attribute, TypeContractor<?> contractor)
        {
            this.contractors.put(attribute, contractor);
            return this;
        }
        
        public Contract build()
        {
            return new Contract(contractors);
        }
    }

    public static Contract merge(Contract leftContract, Contract rightContract)
    {
        Map<String, TypeContractor<?>> predicateMap = new HashMap<>();
        
        Set<String> attributes = Stream.concat(
            leftContract.getAttributes().stream(),
            rightContract.getAttributes().stream())
                .collect(Collectors.toSet());
                
        
        for(String a: attributes)
        {
            TypeContractor<?> composedPredicate;
            
            if(leftContract.getAttributes().contains(a) && rightContract.getAttributes().contains(a))
            {
                composedPredicate = leftContract.getAttributeContract(a);
                composedPredicate.refine(rightContract
                    .getAttributeContract(a)
                    .getEmbeddedPredicate());
            }
            else if(leftContract.getAttributes().contains(a))
                composedPredicate = leftContract.getAttributeContract(a);
            else
                composedPredicate = rightContract.getAttributeContract(a);

            
            predicateMap.put(a, composedPredicate);
        }
        
        return new Contract(predicateMap);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return Objects.equals(contractors, contract.contractors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractors);
    }

    @Override
    public String toString() {
        return "Contract{" +
                "contractors=" + contractors +
                '}';
    }
}
