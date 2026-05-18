package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.Dataset;
import java.util.List;

public class RepairEngineChain
{
    private List<RepairEngine<?,?>> engines;

    public RepairEngineChain(List<RepairEngine<?, ?>> engines) {
        this.engines = engines;
    }

    public List<RepairEngine<?, ?>> getEngines() {
        return engines;
    }
    
    public Dataset execute(Dataset input) throws RepairException
    {
        Dataset output = input;
        
        for(RepairEngine<?,?> engine: engines)
        {
            output = engine.repair(output);
        }
        
        return output;
    }
}
