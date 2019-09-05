import pathlib
from argparse import ArgumentParser
import os
import pandas as pd
import re


def make_stats(res_dir, out_dir, t):
    # t = 'kernel'

    begin_lines = [
        '\\documentclass{article}\n',
        '\\usepackage{booktabs}\n',
        '\\usepackage{pgfplots}\n',
        '\\pgfplotsset{compat=1.14}\n',
        '\n',
        '\\newcommand{\distplot}[4]{\n',
        '\\begin{tikzpicture}\n',
        '\\begin{axis}[\n',
        'height = 70,\n',
        'width = 0.8\\textwidth,\n',
        'xbar stacked,\n',
        'axis y line = none,\n',
        'axis x line = none,\n',
        'xmin = 0,\n',
        'nodes near coords,\n',
        'every node near coord/.append style={yshift=10pt},\n',
        ']\n',
        '\\addplot coordinates {(#1,0)};\n',
        '\\addplot coordinates {(#2,0)};\n',
        '\\addplot coordinates {(#3,0)};\n',
        '\\addplot coordinates {(#4,0)};\n',
        '\\end{axis}\n',
        '\\end{tikzpicture}\n',
        '}\n',
        '\n',
        '\\newcommand{\distplotlegend}[4]{\n',
        '\\begin{tikzpicture}\n',
        '\\begin{axis}[\n',
        'height = 70,\n',
        'width = 0.8\\textwidth,\n',
        'xbar stacked,\n',
        'axis y line = none,\n',
        'axis x line = none,\n',
        'xmin = 0,\n',
        'nodes near coords,\n',
        'every node near coord/.append style={yshift=10pt},\n',
        'legend style={at={(0.5,-0.1)},anchor=north,draw=none,column sep=1ex,},\n',
        'legend columns=-1\n',
        ']\n',
        '\\addplot coordinates {(#1,0)};\n',
        '\\addplot coordinates {(#2,0)};\n',
        '\\addplot coordinates {(#3,0)};\n',
        '\\addplot coordinates {(#4,0)};\n',
        '\\addlegendentry{Safe};\n',
        '\\addlegendentry{Borderline};\n',
        '\\addlegendentry{Rare};\n',
        '\\addlegendentry{Outlier};\n',
        '\end{axis}\n',
        '\end{tikzpicture}\n',
        '}\n',
        '\\begin{document}\n'
    ]
    class_lines = begin_lines.copy()
    union_lines = begin_lines.copy()
    for dataset_name in os.listdir(res_dir):
        dataset_name_tex = re.sub('_', '\\_', dataset_name)
        class_lines.append('{}\n\n'.format(dataset_name_tex))
        path = os.path.join(res_dir, dataset_name)
        df = get_as_df(os.path.join(path, 'class_vs_union_{}.csv'.format(t)), 'class')
        # write_dataframe(df, os.path.join(out_dir, 'class_{}'.format(t), 'csv', dataset_name + '.csv'))
        class_lines.append(df_to_tex(df, 'class'))
        union_lines.append('{}\n\n'.format(dataset_name_tex))
        df = get_as_df(os.path.join(path, 'union_vs_union_{}.csv'.format(t)), 'union')
        # write_dataframe(df, os.path.join(out_dir, 'union_{}'.format(t), 'csv', dataset_name + '.csv'))
        union_lines.append(df_to_tex(df, 'union'))
    class_lines.append('\\end{document}\n')
    union_lines.append('\\end{document}\n')
    write_file(os.path.join(out_dir, 'class_{}'.format(t), 'class_{}.tex'.format(t)), class_lines)
    write_file(os.path.join(out_dir, 'union_{}'.format(t), 'union_{}.tex'.format(t)), union_lines)


def get_tabular(filepath):
    df = pd.read_csv(filepath, delimiter=';', header=0)
    count = df.groupby(['minority_decision', 'majority_decision', 'type']).agg('count')
    return '{}\n'.format(count.to_latex())


def get_as_df(filepath, u):
    cols = []
    if u == 'class':
        cols = ['class', 'union', 'safe', 'borderline', 'rare', 'outlier', 'total']
    else:
        cols = ['min_union', 'maj_union', 'safe', 'borderline', 'rare', 'outlier', 'total']
    df = pd.read_csv(filepath, delimiter=';', header=0)
    count_types_grouped = df.groupby(['minority_decision', 'majority_decision', 'type'])
    count_types = count_types_grouped.agg('count')
    count_divisions_grouped = count_types.groupby(['minority_decision', 'majority_decision'])
    df = pd.DataFrame(columns=cols)
    i = -1
    previous_outer_group = ()
    for name, group in count_types_grouped:
        current_outer_group = (name[0], name[1])
        if current_outer_group != previous_outer_group:
            i += 1
            df.loc[i] = [name[0], name[1], 0, 0, 0, 0, 0]
            previous_outer_group = current_outer_group
        no_in_group = group['index'].agg('count')
        total = count_divisions_grouped.get_group(current_outer_group)['index'].agg('sum')
        df.at[i, name[2].lower()] = no_in_group
        df.at[i, 'total'] = total
    return df


def df_to_tex(df, u):
    header = ''
    if u == 'class':
        header = 'Class & Union &  & Total \\\\ \n'
    else:
        header = 'Union (min) & Union (maj) &  & Total \\\\ \n'
    lines = [
        '\\begin{tabular}{lllr}\n',
        '\\toprule\n',
        header,
        '\\midrule\n'
    ]
    for i in range(len(df)):
        line = df.loc[i]
        if i == len(df) - 1:
            line = '{} & {} & \\distplotlegend{{{}}}{{{}}}{{{}}}{{{}}} & {} \\\\ \n'.format(*line)
        else:
            line = '{} & {} & \\distplot{{{}}}{{{}}}{{{}}}{{{}}} & {} \\\\ \n'.format(*line)
        line = re.sub('_', '\\_', line)
        lines += line
    lines += [
        '\\bottomrule\n',
        '\\end{tabular}\n'
    ]
    if len(lines) > 6:  # control lines + at least 1 line of data
        return ''.join(lines)
    else:
        return ''


def write_dataframe(df, filepath):
    df.to_csv(filepath, sep=' ', index=False)


def write_file(filepath, lines):
    path = pathlib.Path(filepath)
    path.parents[0].mkdir(parents=True, exist_ok=True)
    with open(filepath, mode='w') as file:
        file.writelines(lines)
        file.close()


def main():
    parser = ArgumentParser()
    parser.add_argument("-r", "--resultspath", dest="results",
                        help="path to analysis results directory")
    parser.add_argument("-o", "--outputpath", dest="out",
                        help="path to output directory")
    args = parser.parse_args()
    make_stats(args.results, args.out, 'knn')
    make_stats(args.results, args.out, 'kernel')
    make_stats(args.results, args.out, 'knn_monotonic')
    make_stats(args.results, args.out, 'kernel_monotonic')


if __name__ == "__main__":
    main()
