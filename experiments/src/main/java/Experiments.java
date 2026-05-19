import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.repair.PartitionSlideRepair;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.io.RuleParser;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.binding.DataWriteException;
import be.ugent.ledc.core.binding.csv.CSVBinder;
import be.ugent.ledc.core.binding.csv.CSVDataReader;
import be.ugent.ledc.core.binding.csv.CSVDataWriter;
import be.ugent.ledc.core.binding.csv.CSVProperties;
import be.ugent.ledc.core.binding.jdbc.JDBCBinder;
import be.ugent.ledc.core.cost.ConstantCostFunction;
import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.ContractedDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.FixedTypeDataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.CPFRandomRepairSelection;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
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
    Map<String, CostFunction> costFunctions;
    PartitionSlideRepair<I> repairEngine;
    Map<String, TemporalDataset<I>> partitionedDatasets;

    // repair info
    int numRepairs;
    Map<String, Pair<TemporalDataset<I>, Long>> repairedDatasets;
    Map<String, List<Pair<I, String>>> repairLocations;


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
        this.costFunctions = new HashMap<>(other.costFunctions);
        this.repairEngine = new PartitionSlideRepair<>(other.repairEngine);
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
     * Creates the partitioned slide repair engine with a constant cost model
     */
    public void getConstantRepairEngine(Set<String> exclude, NullBehavior nullBehavior)
            throws ChronosException, RepairException {
        Map<String, ConstantCostFunction> costFunctions = new HashMap<>();
        for (String attr: fullDataset.getContract().getAttributes()) {
            if (!attr.equals(timeAttribute) && !attr.equals(partitionAttribute) && !exclude.contains(attr)) {
                costFunctions.put(attr, new ConstantCostFunction());
            }
        }

        this.costFunctions = new LinkedHashMap<>();
        this.costFunctions.putAll(costFunctions);

        ConstantCostModel costModel = new ConstantCostModel(TRuleset.unfold(costFunctions));

        this.repairEngine = new PartitionSlideRepair<>(
                ruleset,
                costModel,
                nullBehavior,
                new CPFRandomRepairSelection(),
                earlyStop
        );
    }

    /**
     * Creates a partitioned slide repair engine with a non-constant cost model
     */
    public void getNonConstantRepairEngine(Set<String> exclude, NullBehavior nullBehavior) throws ChronosException, RepairException {
        NonConstantCostModel costModel = getNonConstantCostModel(exclude);

        this.repairEngine = new PartitionSlideRepair<>(
                ruleset,
                costModel,
                nullBehavior,
                new CPFRandomRepairSelection(),
                earlyStop
        );
    }

    /**
     * Repair the datasets and return the repairs with their costs
     */
    public double repair(int numAnchors, Validator<I> validator, Set<String> considerAttributes) throws RepairException, ChronosException {
        repairedDatasets = new HashMap<>();
        List<String> keys = partitionedDatasets.keySet().stream().toList();
        System.out.println("Number of partitioned datasets: " + keys.size());
        double duration = 0.0;
        int i = 0;
        int divider = keys.size() < 10 ? 1 : keys.size() / 10;
        for (String key : keys) {
            if (i % divider == 0) {
                System.out.println("i: " + i + " (size: " + partitionedDatasets.get(key).size() + ")");
            }
            i++;
            I curr = partitionedDatasets.get(key).start();
            int index = 1;
            Set<I> elements = new HashSet<>();
            // add start of time series
            elements.add(curr);
            if (numAnchors > 1) {
                // add uniform points in time series
                int size = partitionedDatasets.get(key).size();
                int jumpSize = (size-2) / (numAnchors-1);
                for (int j = 0; j < numAnchors-2; j++) {
                    for (int k = 0; k < jumpSize; k++) {
                        curr = partitionedDatasets.get(key).getObjectSignal().nextIndex(curr);
                        index ++;
                    }
                    if (!considerAttributes.isEmpty()) {
                        // search for timestamp without errors
                        int tempIndex = index;
                        I temp = curr;
                        List<Validator.Location<I>> locations = validator.getPartitionedLocations().get(key);
                        int finalTempIndex = tempIndex;
                        Set<String> repairedAttributes = locations.stream()
                                .filter(loc -> loc.index == finalTempIndex)
                                .map(loc -> loc.attribute)
                                .collect(Collectors.toSet());
                        while (considerAttributes.stream().anyMatch(repairedAttributes::contains)) {
                            temp = partitionedDatasets.get(key).getObjectSignal().previousIndex(temp);
                            tempIndex--;
                            int finalTempIndex1 = tempIndex;
                            repairedAttributes = locations.stream()
                                    .filter(loc -> loc.index == finalTempIndex1)
                                    .map(loc -> loc.attribute)
                                    .collect(Collectors.toSet());
                        }
                        elements.add(temp);
                    } else {
                        elements.add(curr);
                    }
                }
                // add end of time series
                index = partitionedDatasets.get(key).size();
                curr = partitionedDatasets.get(key).end();
                if (!considerAttributes.isEmpty()) {
                    // search for timestamp without errors
                    int tempIndex = index;
                    I temp = curr;
                    List<Validator.Location<I>> locations = validator.getPartitionedLocations().get(key);
                    int finalTempIndex = tempIndex;
                    Set<String> repairedAttributes = locations.stream()
                            .filter(loc -> loc.index == finalTempIndex)
                            .map(loc -> loc.attribute)
                            .collect(Collectors.toSet());
                    while (considerAttributes.stream().anyMatch(repairedAttributes::contains)) {
                        temp = partitionedDatasets.get(key).getObjectSignal().previousIndex(temp);
                        tempIndex--;
                        int finalTempIndex1 = tempIndex;
                        repairedAttributes = locations.stream()
                                .filter(loc -> loc.index == finalTempIndex1)
                                .map(loc -> loc.attribute)
                                .collect(Collectors.toSet());
                    }
                    elements.add(temp);
                } else {
                    elements.add(curr);
                }
            }
            long start = System.currentTimeMillis();
            Pair<TemporalDataset<I>, Long> repair = repairEngine.repair(partitionedDatasets.get(key), elements);
            long stop = System.currentTimeMillis();
            duration += (stop - start)/1000.0/60.0;

            repairedDatasets.put(key, repair);
        }

        return duration;
    }


    /**
     * Gather all repair locations into a variable.
     * A repair location is defined by the partition key, the time value and the attribute that is repaired.
     */
    private void findRepairLocations() {
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
            int prevIndex = -1;
            for (Validator.Location<I> location: entry.getValue()) {
                if (location.index < prevIndex) {
                    System.out.println("test");
                }
                prevIndex = location.index;
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
        System.out.println("numer of partitions: " + this.partitionedDatasets.size());

        System.out.println("Started at time: " + new Date());
        long start = System.currentTimeMillis();

        // create repair engine
        if (constant) getConstantRepairEngine(noRepairs, nullBehavior);
        else getNonConstantRepairEngine(noRepairs, nullBehavior);

        long stop = System.currentTimeMillis();
        return (stop - start)/1000.0/60.0;

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
}
