import os
import pandas as pd
import pathlib
from argparse import ArgumentParser


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
                if len(value.items()) > 0:
                    results[key].append(value)
    fieldnames = ['name', 'safe', 'borderline', 'rare', 'outlier']
    for key, value in results.items():
        if len(value) > 0:
            save_csv(value, fieldnames, os.path.join(results_path, key + '.csv'))


def analyze(path):
    results = {}
    for filename in os.listdir(path):
        path_to_file = os.path.join(path, filename)
        dataset_name = os.path.basename(os.path.normpath(path))
        result = count(path_to_file, dataset_name)
        if len(result.items()) > 1:
            key = filename.replace('.csv', '')
            results[key] = result
    return results


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
    df = pd.DataFrame(data)
    df = df.sort_values('safe', ascending=False)
    df.to_csv(csv_file_path, index=False, sep=';')


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
