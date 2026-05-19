import pandas as pd

if __name__ == '__main__':
    df = pd.read_csv('dataset_full.csv', delimiter=';', dtype=str)
    for n in [1000, 5000, 10000, 50000, 100000, 200000, 400000]:
        df_subset = df.head(n)
        df_subset.to_csv(f'dataset_{n}.csv', sep=';', index=False)