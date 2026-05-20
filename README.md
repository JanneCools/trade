# TRADE: Transition-Rule-based Anomaly Detection Engine

This repository contains the reproducibility artifacts for the TRADE framework,
a logic-based approach for detecting anomalies in sparse and irregular time series using transition rules.

## Papers
For each paper, a corresponding release provides the code used for those experiments for reproducibility.

| Release | Paper | Venue|
|---------|-------|------|
| v1.0    | TRADE: Time Series Anomaly Detection Using Transition Rules| EDBT/ICDT 2027|


## Repository Structure
### Implementation code
The implementation code of TRADE consists of three directories: `core`, `sigma`, and `chronos`. 
The `core` directory contains the central data model, basic binding to data sources, and common operators used in the other directories.
The `sigma` directory represents the selection rule framework, while `chronos` contains the extensions introduced by TRADE such as 
the concepts of time series data and the minimum-cost repair mechanism for anomaly detection.

### Datasets
The `data` directory contains a subdirectory for each dataset (`gait`, `nba`, `election`, and `settlement`).
For each dataset, three files are used: 
- The data is stored in `dataset.csv` which includes a header with the attribute names. 
As each dataset is a collection of independent time series, there is a specific attribute per dataset (the partition attribute)
that can be used to extract an individual time series.
- The ground truth is stored in `error_locations.txt`. Each line in this file represents an anomaly in the dataset and consists of five fields, separated by a semicolon `;`.
The first field holds the partition attribute to identify the time series, while the second field specifies in which line (1-based indexing) the error occurs.
The third field indicates the attribute that is anomalous, the fourth field holds the clean value, and the fifth field stores the dirty value.
- The set of transition rules is stored in `rules.rbx`. 

For the `settlement` dataset, an additional file `dataset_full.csv` stores the full dataset used for the scalability tests.
The subsets of various sizes can be extracted from this dataset using the python script `util.py`.

### Experiments
The code for the experiments can be found in the directory `experiments`. This directory contains an abstract class
`Experiments` that contains mutual variables and functions, and runs the preprocessing and detection phases.
For each dataset, a separate subclass overrides some functions based on the properties of the dataset.
In each subclass, the hyperparameters can be set, including the number of anchors, activation of the early-stop
mechanism, and the type of cost model. Each subclass can be run directly; it will iteratively execute the algorithm
and write the average precision, recall, F1-score, preprocessing time, and detection time to standard output.
For the scalability tests, there is an additional, commented main function in the `ExperimentsSettlement` class.
This main function runs the preprocessing phase only once, as its execution time does not depend on the subset size.

## Requirements
- Java 17 or higher
- Maven
- Python 3.9+ for extracting subsets of the `settlement` dataset for scalability tests

## How to Reproduce
Clone this repository and build the project using `mvn install`.

You can run the main function in any of the experiment classes. 
At the start of such a main function, several hyperparameters are set. 
The following parameters are already set to their correct value and do not need to be altered:
- `path`: The path to the dataset subdirectory.
- `datasetFilename`: The filename of the dataset.
- `rulesetFilename`: The filename of the rule set.
- `partitionAttribute`: The attribute that is used to partition the time series in the dataset.
- `timeAttribute`: The attribute that is used to sort the tuples within a time series.

The following parameters determine the variants used in the experiments and can be changed:
- `earlyStop`: A boolean value that indicates whether the early-stop mechanism is applied.
- `baseline`: A boolean value that indicates whether the baseline cost model is used. If set to false, the customized cost model is used.
- `numAnchors`: The number of anchors. In the experiments, this is set to 1, 5, or 10.

Additionally, for the scalability tests, the full or a subset of the `settlement` dataset is used to run experiments.
Therefore, a parameter `tuples` specifies the number of tuples of the subset, and is used to retrieve the correct filename for parameter `datasetFilename`.
The subsets can be extracted using the `utils.py` file.

For each experiment, a for loop runs the algorithm 10 times, but this number can be altered by changing the parameter `amount`.
While the average of every metric (i.e., precision, recall, F1-score, preprocessing time, and detection time) is printed
at the end of the program to standard output, each run also reports all metrics to standard output.
This allows the user to inspect the average value as well as the separate values per run.
For the scalability tests, only the preprocessing time and detection time are reported.





