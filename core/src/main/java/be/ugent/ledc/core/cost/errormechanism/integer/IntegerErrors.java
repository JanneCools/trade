package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.List;

public enum IntegerErrors implements ErrorMechanism<Integer>
{
    SINGLE_DIGIT(new SingleDigitError(), "SingleDigitError", "sd"),
    ADJACENT_TRANSPOSITION(new AdjacentTranspositionError(), "AdjacentTranspositionError", "at"),
    TWIN(new TwinError(), "TwinError", "t"),
    PHONETIC(new PhoneticError(), "PhoneticError", "p"),
    SIGN_ERROR(new LinearError(0, -1), "SignError", "s"),
    ROUNDING_ERROR(new RoundingError(), "RoundingError", "r")
    ;
        
    private final ErrorMechanism<Integer> embeddedMechanism;
    private final String name;
    private final String shortName;

    IntegerErrors(ErrorMechanism<Integer> embeddedMechanism, String name, String shortName)
    {
        this.embeddedMechanism = embeddedMechanism;
        this.name = name;
        this.shortName = shortName;
    }

    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        return this.embeddedMechanism.explains(originalValue, repairedValue, originalObject);
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        return this.embeddedMechanism.explanations(originalValue, originalObject);
    }

    public ErrorMechanism<Integer> getEmbeddedMechanism()
    {
        return embeddedMechanism;
    }

    public String getName()
    {
        return name;
    }
    
    public String getShortName()
    {
        return shortName;
    }
    
    
    
}
