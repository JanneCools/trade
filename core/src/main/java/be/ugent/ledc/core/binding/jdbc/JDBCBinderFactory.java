package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.config.FeatureBuilder;
import be.ugent.ledc.core.config.FeatureConsumer;
import be.ugent.ledc.core.config.FeatureMap;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JDBCBinderFactory implements FeatureConsumer<JDBCBinder>, FeatureBuilder<JDBCBinder>
{
    public static final String ENCODING = "encoding";
    public static final String HOST     = "host";
    public static final String PORT     = "port";
    public static final String USER     = "user";
    public static final String PASSWORD = "password";
    public static final String DATABASE = "database";
    public static final String SCHEMA   = "schema";
    public static final String VENDOR   = "vendor";
    
    private final String[] requiredFeatures = new String[]{
        HOST,
        USER, 
        PORT,
        DATABASE,
        VENDOR};
    
    @Override
    public FeatureMap buildFeatureMap(JDBCBinder binder)
    {
        return new FeatureMap()
            .addFeature(ENCODING, binder.getEncoding().name())
            .addFeature(HOST, binder.getRdb().getHost())
            .addFeature(PORT, binder.getRdb().getPort())
            .addFeature(USER, binder.getRdb().getUsername())
            .addFeature(PASSWORD, binder.getRdb().getPassword())
            .addFeature(DATABASE, binder.getRdb().getDatabaseName())
            .addFeature(SCHEMA, binder.getRdb().getSchemaName())
            .addFeature(VENDOR, binder.getRdb().getVendor().name());
    }
    
    @Override
    public JDBCBinder buildFromFeatures(FeatureMap featureMap) throws ParseException
    {
        for(String requiredFeature: requiredFeatures)
            if(featureMap.get(requiredFeature) == null)
                throw new ParseException("Cannot instantiate JDBCBinder from JSON file: required feature '" + requiredFeature + "' is missing");
        
        return new JDBCBinder(
            new RelationalDB(
                DBMS.valueOf(featureMap.getString(VENDOR)),
                featureMap.getString(HOST),
                featureMap.getString(PORT),
                featureMap.getString(USER),
                featureMap.getString(PASSWORD),
                featureMap.getString(DATABASE),
                featureMap.getString(SCHEMA) == null
                    ? "public"
                    : featureMap.getString(SCHEMA)),
            featureMap.getString(ENCODING) == null
                ? StandardCharsets.UTF_8
                : Charset.forName(featureMap.getString("encoding"))
        );
    }
    
}
