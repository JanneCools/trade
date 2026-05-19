import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.cost.ConstantCostFunction;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.bounding.PartitionedBounding;
import be.ugent.ledc.sigma.repair.bounding.PartitionedTemporalBounding;
import be.ugent.ledc.sigma.repair.bounding.SetBounding;
import be.ugent.ledc.sigma.repair.cost.functions.CalculationCostFunction;
import be.ugent.ledc.sigma.repair.cost.functions.DistanceCostFunction;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunctionWrapper;
import be.ugent.ledc.sigma.repair.cost.models.NonConstantCostModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class  ExperimentsNBA extends Experiments<Integer> {


    public ExperimentsNBA(String path, String datasetFilename, String rulesetFilename,
                          String timeAttribute, String partitionAttribute, boolean earlyStop) {
        super(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
    }

    public ExperimentsNBA(ExperimentsNBA other) {
        super(other);
    }

    private <T extends Comparable<? super T>> Map<String, T> getMedians(
            String attribute, BinaryOperator<T> averager
    ) throws ChronosException {
        Map<String, T> medians = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<Integer> dataset = this.partitionedDatasets.get(key);
            Signal<Integer, T> signal = dataset.getAttributeSignal(attribute);

            List<T> values = new ArrayList<>();
            Integer curr = signal.start();
            while (curr != null) {
                T value = signal.get(curr);
                if (value != null)
                    values.add(value);
                curr = signal.nextIndex(curr);
            }

            values.sort(T::compareTo);
            T median = values.get(values.size()/2);
            if (values.size() % 2 == 0) {
                median = averager.apply(values.get(values.size()/2), values.get(values.size()/2 - 1));
            }

            medians.put(key, median);
        }

        return medians;
    }

    @Override
    public void createTemporalDatasets() throws ChronosException {
        this.partitionedDatasets = TemporalDataset.create(fullDataset, timeAttribute, SigmaContractorFactory.INTEGER, partitionAttribute);
    }

    @Override
    protected NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException {
        Map<String, IterableCostFunction<?>> costFunctions = new HashMap<>();

        costFunctions.put("age", new IterableCostFunctionWrapper<>(new DistanceCostFunction<>(SigmaContractorFactory.INTEGER)));
        costFunctions.put("yrs_experience", new IterableCostFunctionWrapper<>(new DistanceCostFunction<>(SigmaContractorFactory.INTEGER)));
        costFunctions.put("team", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));
        costFunctions.put("position", new IterableCostFunctionWrapper<>(new ConstantCostFunction<String>()));

        Function<List<Integer>, Integer> operatorGames = (l) -> l.get(0) / 48;
        Function<List<Integer>, Integer> operatorMinutes = (l) -> l.get(0) * 48;
        costFunctions.put("games", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "games", SigmaContractorFactory.INTEGER,
                List.of("minutes_played"), List.of(SigmaContractorFactory.INTEGER), operatorGames
        )));
        costFunctions.put("minutes_played", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "minutes_played", SigmaContractorFactory.INTEGER,
                List.of("games"), List.of(SigmaContractorFactory.INTEGER), operatorMinutes
        )));

        BigDecimal bd1 = new BigDecimal("-0.48");
        BigDecimal bd2 = new BigDecimal("3996650");
        BigDecimal bd3 = new BigDecimal("1942910");

        Function<List<Integer>, BigDecimal> operatorVorp = (l) -> BigDecimal.valueOf(l.get(0)).setScale(2, RoundingMode.HALF_UP).subtract(bd3).divide(bd2, RoundingMode.HALF_UP);
        Function<List<BigDecimal>, Integer> operatorSalary = (l) -> l.get(0).compareTo(bd1) <= 0 ? 500000 : l.get(0).multiply(bd2).add(bd3).setScale(0, RoundingMode.HALF_UP).intValueExact();
        UnaryOperator<Long> vorpUpdater = (dist) -> new BigDecimal(dist)
                .setScale(2, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), RoundingMode.HALF_UP)
                .multiply(bd2).add(bd3)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        costFunctions.put("vorp", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "vorp", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS,
                List.of("salary"), List.of(SigmaContractorFactory.INTEGER), operatorVorp, vorpUpdater
        )));
        costFunctions.put("salary", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "salary", SigmaContractorFactory.INTEGER,
                List.of("vorp"), List.of(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS), operatorSalary
        )));

        BigDecimal two = new BigDecimal("2");

        Function<List<BigDecimal>, BigDecimal> operatorDrbOrb = (l) -> l.get(1).multiply(two).subtract(l.get(0));
        costFunctions.put("drb", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "drb", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, List.of("orb", "trb"),
                List.of(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS), operatorDrbOrb
        )));
        costFunctions.put("orb", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "orb", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, List.of("drb", "trb"),
                List.of(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS), operatorDrbOrb
        )));

        Function<List<BigDecimal>, BigDecimal> operatorTrb = (l) -> l.get(0).add(l.get(1)).divide(two, RoundingMode.HALF_UP);
        costFunctions.put("trb", new IterableCostFunctionWrapper<>(new CalculationCostFunction<>(
                "trb", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, List.of("orb", "drb"),
                List.of(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS), operatorTrb
        )));

        this.costFunctions = new LinkedHashMap<>();
        this.costFunctions.putAll(costFunctions);

        return new NonConstantCostModel(TRuleset.unfold(costFunctions), Map.ofEntries(
                Map.entry("salary#curr", new PartitionedTemporalBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.INTEGER, "line_nr",
                        SigmaContractorFactory.INTEGER, "salary", 10
                )),
                Map.entry("salary#next", new PartitionedTemporalBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.INTEGER, "line_nr",
                        SigmaContractorFactory.INTEGER, "salary", 10
                )),
                Map.entry("minutes_played#curr", new PartitionedTemporalBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.INTEGER, "line_nr",
                        SigmaContractorFactory.INTEGER, "minutes_played", 10
                )),
                Map.entry("minutes_played#next", new PartitionedTemporalBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.INTEGER, "line_nr",
                        SigmaContractorFactory.INTEGER, "minutes_played", 10
                )),
                Map.entry("drb#curr", new PartitionedBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "drb", 1000
                )),
                Map.entry("drb#next", new PartitionedBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "drb", 1000
                )),
                Map.entry("orb#curr", new PartitionedBounding<>(
                    this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "orb", 1000
                )),
                Map.entry("orb#next", new PartitionedBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "orb", 1000
                )),
                Map.entry("trb#curr", new PartitionedBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "trb", 1000
                )),
                Map.entry("trb#next", new PartitionedBounding<>(
                        this.fullDataset, "player", SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, "trb", 1000
                )),
                Map.entry("position#curr", new SetBounding<>(Set.of("PG", "SG", "SF", "PF", "C"))),
                Map.entry("position#next", new SetBounding<>(Set.of("PG", "SG", "SF", "PF", "C")))
        ));
    }


    /**
     * This main function creates a new instance of the RepairCustomNbaPlayer class in each run,
     * and thus executes the preprocessing/initialization phase each run.
     */
    public static void main(String[] args) throws ChronosException, DataReadException, ParseException, IOException, RepairException {
        long startTotal = System.currentTimeMillis();

        String path = "data/nba";
        String datasetFilename = "dataset.csv";
        String rulesetFilename = "rules.rbx";
        String timeAttribute = "line_nr";
        String partitionAttribute = "player";

        boolean baseline = true;
        int numAnchors = 1;
        boolean earlyStop = true;

        System.out.println("Running ExperimentsNBA with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");

        int amount = 10;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double executionTime = 0.0;
        double executionTimePreprocessing = 0.0;
        Validator<Integer> validator = new Validator<>(path + "/error_locations.txt");
        for (int i = 0; i < amount; i++) {
            ExperimentsNBA rd = new ExperimentsNBA(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);

            rd.readDatasetFromPath(";");
            rd.readRulesFromPath();
            double durationPreprocessing = rd.initialize(baseline, Set.of("year"), NullBehavior.NO_REPAIR);
            double duration = rd.run(numAnchors, validator, Set.of("age", "yrs_experience"));

            int numCells = rd.fullDataset.getSize() * rd.fullDataset.getContract().getAttributes().size();
            validator.setPartitionedLocations(rd.convertRepairLocations(validator.getPartitionedLocations()));
            Map<String, List<Validator.Location<Integer>>> convertedLocations = rd.convertRepairLocations();
            List<Double> metrics = validator.validate(rd.validationPath, convertedLocations, numCells);
            precision += metrics.get(0);
            recall += metrics.get(1);
            f1 += metrics.get(2);
            executionTime += duration;
            executionTimePreprocessing += durationPreprocessing;
            System.out.println(durationPreprocessing + " + " + duration + ": " + metrics);
        }
        System.out.println("Avg precision: " + precision / amount);
        System.out.println("Avg recall: " + recall / amount);
        System.out.println("Avg f1: " + f1 / amount);
        System.out.println("Avg execution time preprocessing: " + executionTimePreprocessing / amount);
        System.out.println("Avg execution time: " + executionTime / amount);

        long endTotal = System.currentTimeMillis();
        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0/60.0 + " minutes");
    }

    /**
     * This main function takes a copy of the RepairCustomGAIT class in each run,
     * so only executes the preprocessing/initialization once.
     */
//    public static void main(String[] args) throws ChronosException, DataReadException, ParseException, IOException, RepairException {
//        long startTotal = System.currentTimeMillis();
//
//        String path = "data/nba";
//        String datasetFilename = "dataset.csv";
//        String rulesetFilename = "rules.rbx";
//        String timeAttribute = "line_nr";
//        String partitionAttribute = "player";
//        boolean earlyStop = true;
//        boolean baseline = true;
//        int numAnchors = 1;
//
//        ExperimentsNBA rd = new ExperimentsNBA(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
//
//        System.out.println("Running ExperimentsNBA with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
//
//        // read dataset and rules
//        long start = System.currentTimeMillis();
//        rd.readDatasetFromPath(";");
//        rd.readRulesFromPath();
//        long stop = System.currentTimeMillis();
//        System.out.println("Time for reading dataset and rules: " + (stop-start)/1000.0 + " seconds");
//
//        double dur = rd.initialize(baseline, Set.of("year"), NullBehavior.NO_REPAIR);
//        System.out.println("time for initialization: " + dur + " minutes");
//
//        int numCells = rd.fullDataset.getSize() * rd.fullDataset.getContract().getAttributes().size();
//
//        // execute repair
//        int amount = 10;
//        double precision = 0.0;
//        double recall = 0.0;
//        double f1 = 0.0;
//        double execution_time = 0.0;
//        for (int i = 0; i < amount; i++) {
//            System.out.println("Run " + i);
//            ExperimentsNBA copy = new ExperimentsNBA(rd);
//            Validator<Integer> validator = new Validator<>(copy.groundTruthPath);
//            double duration = copy.run(numAnchors, validator, Set.of("age", "yrs_experience"));
//
//            validator.setPartitionedLocations(copy.convertRepairLocations(validator.getPartitionedLocations()));
//            Map<String, List<Validator.Location<Integer>>> convertedLocations = copy.convertRepairLocations();
//            List<Double> metrics = validator.validate(copy.validationPath, convertedLocations, numCells);
//            precision += metrics.get(0);
//            recall += metrics.get(1);
//            f1 += metrics.get(2);
//            execution_time += duration;
//            System.out.println(duration + " ; " + metrics);
//        }
//        System.out.println("Avg precision: " + precision / amount);
//        System.out.println("Avg recall: " + recall / amount);
//        System.out.println("Avg f1: " + f1 / amount);
//        System.out.println("Avg execution_time: " + execution_time / amount);
//
//        long endTotal = System.currentTimeMillis();
//        System.out.println("Total runtime: " + (endTotal-startTotal)/1000.0/60.0 + " minutes");
//    }
}
