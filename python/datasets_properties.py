from argparse import ArgumentParser
import os
import pandas as pd
import json


def get_dataframes(csv_dir):
    for csv_filename in os.listdir(csv_dir):
        csv_filepath = os.path.join(csv_dir, csv_filename)
        df = pd.read_csv(csv_filepath, delimiter=',', header=None)
        df.filename = csv_filename
        yield df


def get_attr_names(json_dir, filename):
    json_filepath = os.path.join(json_dir, filename.replace('.csv', '.json'))
    names = []
    dec_attr_name = ''
    with open(json_filepath, mode='r') as json_file:
        attributes = json.load(json_file)
        for attr in attributes:
            names.append(attr['name'])
            if attr['type'] == 'decision':
                dec_attr_name = attr['name']
    return names, dec_attr_name


def count(df, decision_attr_name):
    examples_no = df.size
    decision_count = df.groupby([decision_attr_name]).size()
    decision_count = decision_count.to_dict()
    return examples_no, decision_count


def main():
    parser = ArgumentParser()
    parser.add_argument("-c", "--csvpath", dest="csv",
                        help="path to datasets csv directory")
    parser.add_argument("-j", "--jsonpath", dest="json",
                        help="path to datasets json directory")
    parser.add_argument("-o", "--outputpath", dest="out",
                        help="path to output directory")
    args = parser.parse_args()
    result = dict()
    result['filenames'] = []
    result['totals'] = []
    result['distributed'] = []
    for df in get_dataframes(args.csv):
        names, decision_attr_name = get_attr_names(args.json, df.filename)
        df.columns = names
        total, distributed = count(df, decision_attr_name)
        result['filenames'].append(df.filename)
        result['totals'].append(total)
        result['distributed'].append(distributed)
    res_df = pd.DataFrame.from_dict(result)
    res_df.to_csv(os.path.join(args.out, 'count.csv'), sep=';', index=0)


if __name__ == "__main__":
    main()
