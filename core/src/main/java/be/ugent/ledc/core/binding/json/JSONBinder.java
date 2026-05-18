package be.ugent.ledc.core.binding.json;

import java.io.File;

public class JSONBinder
{
    private final File jsonFile;
    
    public static final String ROOT_NAME = "root";

    public JSONBinder(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    public File getJsonFile() {
        return jsonFile;
    }
}
