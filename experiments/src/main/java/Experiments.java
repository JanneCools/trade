import chronos.ChronosException;
import chronos.algorithms.repair.TimeSeriesRepair;
import chronos.datastructures.Signal;
import chronos.datastructures.TemporalDataset;
import chronos.io.RuleParser;
import chronos.rules.TRuleset;
import core.ParseException;
import core.RepairException;
import core.binding.BindingException;
import core.binding.DataReadException;
import core.binding.csv.CSVBinder;
import core.binding.csv.CSVDataReader;
import core.binding.csv.CSVProperties;
import core.binding.jdbc.JDBCBinder;
import core.cost.ConstantCostFunction;
import core.dataset.ContractedDataset;
import core.dataset.DataObject;
import core.dataset.FixedTypeDataset;
import core.dataset.contractors.TypeContractorFactory;
import core.datastructures.Pair;
import sigma.repair.NullBehavior;
import sigma.repair.cost.models.ConstantCostModel;
import sigma.repair.cost.models.NonConstantCostModel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Experiments<I extends Comparable<? super I>> {
    // metadata
    private final boolean earlyStop;
    private final String datasetPath;
    private final String rulesetPath;
    final String timeAttribute;
    final String partitionAttribute;
    String separator = ";";

    final String groundTruthPath;
    protected final String validationPath;

    // data
    List<String> attributes;
    ContractedDataset fullDataset;
    TRuleset ruleset;
    Map<String, TemporalDataset<I>> partitionedDatasets;
    TimeSeriesRepair<I> timeSeriesRepair;

    // repair info
    int numRepairs;
    Map<String, Pair<TemporalDataset<I>, Long>> repairedDatasets;
    Map<String, List<Pair<I, String>>> repairLocations;
    Validator<I> validator;


    public Experiments(
            String path, String datasetFilename, String rulesetFilename,
            String timeAttribute, String partitionAttribute, boolean earlyStop) {
        this.earlyStop = earlyStop;
        this.datasetPath = path + "/" + datasetFilename;
        this.rulesetPath = path + "/" + rulesetFilename;
        this.timeAttribute = timeAttribute;
        this.partitionAttribute = partitionAttribute;
        this.groundTruthPath = path + "/error_locations.txt";
        this.validationPath = path + "/validation.toml";
    }

    public Experiments(Experiments<I> other) {
        this.earlyStop = other.earlyStop;
        this.datasetPath = other.datasetPath;
        this.rulesetPath = other.rulesetPath;
        this.timeAttribute = other.timeAttribute;
        this.partitionAttribute = other.partitionAttribute;
        this.groundTruthPath = other.groundTruthPath;
        this.validationPath = other.validationPath;

        this.attributes = new ArrayList<>(other.attributes);
        this.fullDataset = new ContractedDataset(other.fullDataset.getContract());
        for (DataObject d: other.fullDataset.getDataObjects()) {
            this.fullDataset.addDataObject(new DataObject(d));
        }
        this.ruleset = new TRuleset(other.ruleset.getContractors(), other.ruleset.getRules());
        this.timeSeriesRepair = new TimeSeriesRepair<>(other.timeSeriesRepair);
    }

    public static double getStd(List<Double> values, Double mean) {
        double sumSquaredDiffs = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / (values.size() - 1));
    }

    /**
     * Sort the list of string values
     */
    public List<String> sortList(List<String> list) {
        return list.stream().sorted().toList();
    }

    /**
     * Partitions the dataset based on the partitionAttribute
     */
    public abstract void createTemporalDatasets() throws ChronosException;

    public void readDatasetFromJDBC(JDBCBinder binder) throws SQLException, BindingException, IOException {
        // connect to the database
        Connection connection = binder.connect();
        String query = String.join(" ", Files.readAllLines(new File(Paths.get(datasetPath).toUri()).toPath()));
        ResultSet resultSet = connection.createStatement().executeQuery(query);

        attributes = new ArrayList<>();
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++)
            attributes.add(resultSet.getMetaData().getColumnName(i));

        // create dataset
        FixedTypeDataset<String> dataset = new FixedTypeDataset<>(
                new HashSet<>(attributes),
                TypeContractorFactory.STRING
        );

        // add data
        while (resultSet.next()) {
            DataObject object = new DataObject();
            for (String columnName : attributes) {
                object.setString(columnName, resultSet.getString(columnName));
            }
            dataset.addDataObject(object);
        }

        this.fullDataset = dataset;
        connection.close();
    }

    /**
     * Reads the dataset from the given path
     * @param separator the separator in the csv file
     */
    public void readDatasetFromPath(String separator) throws DataReadException {
        this.separator = separator;
        CSVProperties properties =  new CSVProperties(true, separator, null);
        CSVBinder binder = new CSVBinder(properties, new File(Paths.get(datasetPath).toUri()));
        CSVDataReader reader = new CSVDataReader(binder);

        this.fullDataset = reader.readData();
        attributes = reader.getColumnNames();
    }

    /**
     * Read the rules from a given path.
     */
    public void readRulesFromPath() throws ParseException, IOException, ChronosException {
        List<String> lines = Files.readAllLines(new File(Paths.get(rulesetPath).toUri()).toPath().toAbsolutePath(), Charset.defaultCharset());
        Pair<TRuleset, ContractedDataset> pair = RuleParser.parseRulesetAndContractors(lines, this.fullDataset);

        this.ruleset = pair.getFirst();
        this.fullDataset = pair.getSecond();
    }

    /**
     * Creates a cost model with cost functions for attributes, but excludes some attributes.
     */
    protected abstract NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException;

    /**
     * Creates  a constant cost model
     */
    public ConstantCostModel getConstantCostModel(Set<String> exclude, NullBehavior nullBehavior)
            throws ChronosException {
        Map<String, ConstantCostFunction> costFunctions = new HashMap<>();
        for (String attr: fullDataset.getContract().getAttributes()) {
            if (!attr.equals(timeAttribute) && !attr.equals(partitionAttribute) && !exclude.contains(attr)) {
                costFunctions.put(attr, new ConstantCostFunction());
            }
        }

        return new ConstantCostModel(TRuleset.unfold(costFunctions));
    }


    /**
     * Gather all repair locations into a variable.
     * A repair location is defined by the partition key, the time value and the attribute that is repaired.
     */
    public void findRepairLocations() {
        this.repairLocations = new LinkedHashMap<>();

        for (String key: partitionedDatasets.keySet()) {
            Signal<I, DataObject> origSignal = partitionedDatasets.get(key).getObjectSignal();
            Signal<I, DataObject> repairedSignal = repairedDatasets.get(key).getFirst().getObjectSignal();

            I curr = origSignal.start();
            while (curr != null) {
                DataObject origO = origSignal.get(curr);
                DataObject repairedO = repairedSignal.get(curr);

                if (!origO.getAttributes().equals(repairedO.getAttributes()))
                    System.out.println("WARNING: different attributes between orig and repaired at function findRepairLocations");
                for (String attr: origO.getAttributes()) {
                    if (!Objects.equals(origO.get(attr), repairedO.get(attr))) {
                        numRepairs++;
                        if (!repairLocations.containsKey(key)) repairLocations.put(key, new ArrayList<>());
                        repairLocations.get(key).add(new Pair<>(curr, attr));
                    }
                }

                curr = origSignal.nextIndex(curr);
            }
        }
    }


    /**
     * Gather all repair locations into a variable.
     * A repair location is defined by the partition key, the time value and the attribute that is repaired.
     */
    private void findRepairLocations(Set<String> validateAttributes) {
        this.repairLocations = new LinkedHashMap<>();

        for (String key: partitionedDatasets.keySet()) {
            Signal<I, DataObject> origSignal = partitionedDatasets.get(key).getObjectSignal();
            Signal<I, DataObject> repairedSignal = repairedDatasets.get(key).getFirst().getObjectSignal();

            I curr = origSignal.start();
            while (curr != null) {
                DataObject origO = origSignal.get(curr);
                DataObject repairedO = repairedSignal.get(curr);

                if (!origO.getAttributes().equals(repairedO.getAttributes()))
                    System.out.println("WARNING: different attributes between orig and repaired at function findRepairLocations");
                for (String attr: validateAttributes) {
                    if (!Objects.equals(origO.get(attr), repairedO.get(attr))) {
                        numRepairs++;
                        if (!repairLocations.containsKey(key)) repairLocations.put(key, new ArrayList<>());
                        repairLocations.get(key).add(new Pair<>(curr, attr));
                    }
                }

                curr = origSignal.nextIndex(curr);
            }
        }
    }

    /**
     * Replaces all time indices I to their corresponding index in the signal
     */
    protected Map<String, List<Validator.Location<I>>> convertRepairLocations() {
        Map<String, List<Validator.Location<I>>> convertedLocations = new HashMap<>();

        // visit every partition that has repairs
        for (Map.Entry<String, List<Pair<I,String>>> entry: repairLocations.entrySet()) {
            String key = entry.getKey();
            convertedLocations.put(key, new ArrayList<>());
            Signal<I, DataObject> signal = partitionedDatasets.get(key).getObjectSignal();
            Signal<I, DataObject> repairSignal = repairedDatasets.get(key).getFirst().getObjectSignal();

            // visit every repair location of the partition
            int index = 1;
            I curr = signal.start();
            List<Pair<I, String>> sortedLocations = entry.getValue();
            sortedLocations.sort((p1,p2) -> {
                if (p1.getFirst().equals(p2.getFirst())) return p1.getSecond().compareTo(p2.getSecond());
                return p1.getFirst().compareTo(p2.getFirst());
            });
            for (Pair<I, String> pair: sortedLocations) {
                // search correct index corresponding to the time value of the repair
                while (!pair.getFirst().equals(curr)) {
                    index++;
                    curr = signal.nextIndex(curr);
                }

                String attribute = pair.getSecond();
                String origValue = signal.get(curr).allAsString().getString(attribute);
                String repairedValue = repairSignal.get(curr).allAsString().getString(attribute);
                convertedLocations.get(key).add(new Validator.Location<>(index, curr, attribute, origValue, repairedValue));
            }
        }
        return convertedLocations;
    }

    protected Map<String, List<Validator.Location<I>>> convertRepairLocations(Map<String, List<Validator.Location<I>>> locations) {
        Map<String, List<Validator.Location<I>>> convertedLocations = new HashMap<>();

        for (Map.Entry<String, List<Validator.Location<I>>> entry: locations.entrySet()) {
            String key = entry.getKey();
            convertedLocations.put(key, new ArrayList<>());
            Signal<I, DataObject> signal = partitionedDatasets.get(key).getObjectSignal();

            int index = 1;
            I curr = signal.start();
            for (Validator.Location<I> location: entry.getValue()) {
                while (index != location.index) {
                    index++;
                    curr = signal.nextIndex(curr);
                }

                convertedLocations.get(key).add(new Validator.Location<>(
                        location.index, curr, location.attribute, location.originalValue, location.repairedValue
                ));
            }
        }

        return convertedLocations;
    }


    /**
     * Initialize some variables
     */
    public double initialize(boolean constant, Set<String> noRepairs, NullBehavior nullBehavior)
            throws ChronosException, RepairException {
        // partition the dataset
        createTemporalDatasets();
        System.out.println("number of partitions: " + this.partitionedDatasets.size());

        System.out.println("Started at time: " + new Date());
        long start = System.currentTimeMillis();

        // create repair engine
        timeSeriesRepair = new TimeSeriesRepair<>(this.partitionedDatasets);
        if (constant) {
            timeSeriesRepair.initialize(
                    getConstantCostModel(noRepairs, nullBehavior),
                    ruleset, nullBehavior, this.earlyStop
            );
        } else {
            timeSeriesRepair.initialize(
                    getNonConstantCostModel(noRepairs),
                    ruleset, nullBehavior, this.earlyStop
            );
        }

        long stop = System.currentTimeMillis();
        return (stop - start)/1000.0;
    }

    public boolean isMatch(Pair<String, Integer> location, Set<String> attributes) {
        List<Validator.Location<I>> locations = validator.getPartitionedLocations().get(location.getFirst());
        int index = location.getSecond();
        Set<String> repairedAttributes = locations.stream()
                .filter(loc -> loc.index == index)
                .map(loc -> loc.attribute)
                .collect(Collectors.toSet());

        return attributes.stream().anyMatch(repairedAttributes::contains);
    }

    /**
     * Repair the datasets and return the repairs with their costs
     */
    public double repair(int numAnchors, Validator<I> validator, Set<String> considerAttributes) throws RepairException, ChronosException {
        this.validator = validator;
        repairedDatasets = new HashMap<>();
        double duration;

        if (considerAttributes.isEmpty()) {
            duration = timeSeriesRepair.repair(numAnchors, repairedDatasets);
        } else {
            duration = timeSeriesRepair.repair(numAnchors, considerAttributes, this::isMatch, repairedDatasets);
        }

        return duration;
    }

    /**
     * Run the full repair process. This includes partitioning the dataset into multiple time series and
     * creating the repair engine (if this isn't done yet), executing the repair, and analyzing the repair.
     */
    public double run(int numAnchors, Validator<I> validator, Set<String> considerAttributes)
            throws ChronosException, RepairException {

        // partition the dataset
        createTemporalDatasets();

        // execute the repair
        double duration = repair(numAnchors, validator, considerAttributes);

        findRepairLocations();

        return duration;
    }

    /**
     * Run the full repair process. This includes partitioning the dataset into multiple time series and
     * creating the repair engine (if this isn't done yet), executing the repair, and analyzing the repair.
     */
    public double run(int numAnchors, Validator<I> validator, Set<String> considerAttributes, Set<String> validateAttributes)
            throws ChronosException, RepairException {

        // partition the dataset
        createTemporalDatasets();

        // execute the repair
        double duration = repair(numAnchors, validator, considerAttributes);

        findRepairLocations(validateAttributes);

        return duration;
    }
}
