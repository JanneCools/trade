package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.Key;
import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author antoon
 */
public class ReluctantReferenceFinder implements ReferenceFinder
{
    public static final int EXACT_ATTRIBUTE_NAME_CHECK = 1;
    public static final int SIGNATURE_NAME_CHECK = 2;
    
    /**
     * Mode for mapping attributes
     */
    private int mappingMode = EXACT_ATTRIBUTE_NAME_CHECK;
    
    /**
     * Make a list of the estimated missing references in the {@link DatabaseSchema}
     * @param schema
     * @return A mapping of a {@link TableSchema} name onto a {@link List} of suspected missing references.
     */
    @Override
    public Map<String, List<Reference>> findMissingReferences(DatabaseSchema schema)
    {
        //Initialize
        Map<String, List<Reference>> missingReferences = new HashMap<>();

        //Find all unconnected schema signatures
        List<TableSchema> unconnectedSignatures = findUnconnectedSignatures(schema);

        //For each unconnected signature
        for(int i=0; i<unconnectedSignatures.size(); i++)
        {
            //Fetch
            TableSchema unconnectedSignature = unconnectedSignatures.get(i);
                            
            //Iterate
            for(String signatureName: schema.keySet())
            {
                //Get signature
                TableSchema signature = schema.get(signatureName);

                //Update list of references (symmetrical!)
                updateReferences(unconnectedSignature, signature, missingReferences);
                updateReferences(signature, unconnectedSignature, missingReferences);
            }
        }

        //Return
        return missingReferences;
    }

    private void updateReferences(TableSchema referencing, TableSchema referenced, Map<String, List<Reference>> missingReferences)
    {
        //Get key of referenced
        Key key = referenced.getPrimaryKey();

        if(key!=null && !referencing.equals(referenced) && referencing.keySet().containsAll(key))
        {
            //New reference
            Reference candidateReference = new Reference(referenced);

            //Indicate that this is a non observed reference
            candidateReference.setObserved(false);

            //Check datatypes
            boolean datatypeMatch = true;

            //Iterate
            for(String keyAttribute: key)
            {
                String candidate = findCandidate(keyAttribute, referencing, referenced);
                
                if(candidate != null)
                {
                    //Check datatypes in both signatures
                    if(!referencing.getAttributeProperty(keyAttribute, TableSchema.PROP_DATATYPE).equals(referenced.getAttributeProperty(keyAttribute, TableSchema.PROP_DATATYPE)))
                    {
                        datatypeMatch = false;
                    }

                    //Map attributes
                    candidateReference.put(candidate, keyAttribute);
                }
                else
                {
                    datatypeMatch = false;
                }
            }

            if(datatypeMatch)
            {
                //Check key occurence
                if(!missingReferences.containsKey(referencing.getName()))
                {
                    missingReferences.put(referencing.getName(), new ArrayList<Reference>());
                }

                if(!missingReferences.get(referencing.getName()).contains(candidateReference))
                {
                    //Add
                    missingReferences.get(referencing.getName()).add(candidateReference);
                }
            }
        }
    }

    private List<TableSchema> findUnconnectedSignatures(DatabaseSchema schema)
    {
        //Find all signature names that are referred to by another signature
        Set<String> referredSignatureNames = new HashSet<>();

        for(String signatureName: schema.keySet())
        {
            //Signature
            TableSchema signature = schema.get(signatureName);

            //Get references
            List<Reference> signatureReferences = signature.getReferences();

            for(int i=0; i<signatureReferences.size(); i++)
            {
                //Get reference
                Reference signatureReference = signatureReferences.get(i);

                //Add referred signature name
                referredSignatureNames.add(signatureReference.getReferenced().getName());
            }
        }

        //Initialize
        List<TableSchema> unconnectedSignatures = new ArrayList<>();

        for (String signatureName: schema.keySet())
        {
            //Signature
            TableSchema signature = schema.get(signatureName);

            if(signature.getReferences().isEmpty() && !referredSignatureNames.contains(signatureName))
            {
                unconnectedSignatures.add(signature);
            }
        }

        //Return
        return unconnectedSignatures;
    }
    
    private String findCandidate(String keyAttribute, TableSchema referencing, TableSchema referenced)
    {
        switch(mappingMode)
        {
            case EXACT_ATTRIBUTE_NAME_CHECK:
                if(referencing.containsKey(keyAttribute))
                {
                    return keyAttribute;
                }
                break;
            case SIGNATURE_NAME_CHECK:
                for(String fkAttribute: referencing.keySet())
                {
                    if(fkAttribute.contains(referenced.getName()))
                    {
                        return fkAttribute;
                    }
                }
                break;
        }

        //Return null
        return null;
    }

    public int getMappingMode()
    {
        return mappingMode;
    }

    public void setMappingMode(int mappingMode)
    {
        this.mappingMode = mappingMode;
    }
}
