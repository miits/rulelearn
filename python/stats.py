import os
import pandas as pd
import csv
import pathlib
from argparse import ArgumentParser


def count(path_to_file, dataset_name):
    df = pd.read_csv(path_to_file, sep=';')
    total = df.shape[0]
    count_df = df.groupby('type')['index'].count().reset_index(name='count')
    results = {'name': dataset_name}
    for index, row in count_df.iterrows():
        results[row['type'].lower()] = row['count'] / total * 100
    return results


def save_csv(data, fieldnames, csv_file_path):
    path = pathlib.Path(csv_file_path)
    path.parents[0].mkdir(parents=True, exist_ok=True)
    with open(csv_file_path, 'w', newline='') as file:
        writer = csv.DictWriter(file, delimiter=';', fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)


def analyze(path):
    results = {
        'union_vs_union_knn': None,
        'union_vs_union_kernel': None,
        'class_vs_union_knn': None,
        'class_vs_union_kernel': None,
        'union_vs_union_knn_monotonic': None,
        'union_vs_union_kernel_monotonic': None,
        'class_vs_union_knn_monotonic': None,
        'class_vs_union_kernel_monotonic': None
    }
    for filename in os.listdir(path):
        path_to_file = os.path.join(path, filename)
        dataset_name = os.path.basename(os.path.normpath(path))
        result = count(path_to_file, dataset_name)
        key = filename.replace('.csv', '')
        results[key] = result
    return results


def make_stats(csv_path, results_path):
    results = {
        'union_vs_union_knn': [],
        'union_vs_union_kernel': [],
        'class_vs_union_knn': [],
        'class_vs_union_kernel': [],
        'union_vs_union_knn_monotonic': [],
        'union_vs_union_kernel_monotonic': [],
        'class_vs_union_knn_monotonic': [],
        'class_vs_union_kernel_monotonic': []
    }
    for entry in os.scandir(csv_path):
        if entry.is_dir:
            path = os.path.join(csv_path, entry.name)
            res = analyze(path)
            for key, value in res.items():
                results[key].append(value)
    fieldnames = ['name', 'safe', 'borderline', 'rare', 'outlier']
    for key, value in results.items():
        save_csv(value, fieldnames, os.path.join(results_path, key + '.csv'))


def main():
    parser = ArgumentParser()
    parser.add_argument("-c", "--csvpath", dest="csv",
                        help="path to csv containing examples labelling")
    parser.add_argument("-r", "--resultsdir", dest="results",
                        help="path to output directory")
    args = parser.parse_args()
    make_stats(args.csv, args.results)


if __name__ == "__main__":
    main()
