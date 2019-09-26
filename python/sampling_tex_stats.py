import pathlib
from argparse import ArgumentParser
import os
import pandas as pd
import re


def make_stats(res_dir, out_dir):
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
    for type in os.scandir(res_dir):
        if type.is_dir() and not type.name.startswith('.'):
            type_name = re.sub('_', '\\_', type.name)
            class_lines.append('\\section{{{}}}\n'.format(type_name))
            union_lines.append('\\section{{{}}}\n'.format(type_name))
            for results in os.scandir(os.path.join(res_dir, type.name)):
                if not results.is_dir():
                    results_name = results.name.replace('.csv', '')
                    results_name = re.sub('_', '\\_', results_name)
                    class_lines.append('\\subsection{{{}}}\n'.format(results_name))
                    union_lines.append('\\subsection{{{}}}\n'.format(results_name))
                    df = pd.read_csv(results, sep=';')
                    for _, dataset in df.groupby('dataset', as_index=False):
                        dataset = dataset.reset_index(drop=True)
                        dataset = dataset.fillna(0)
                        dataset_name = dataset.iloc[0]['dataset']
                        dataset_name_tex = re.sub('_', '\\_', dataset_name)
                        if results.name.startswith('class'):
                            class_lines.append('{}\n\n'.format(dataset_name_tex))
                            class_lines.append(df_to_tex(dataset))
                        else:
                            union_lines.append('{}\n\n'.format(dataset_name_tex))
                            union_lines.append(df_to_tex(dataset))
    class_lines.append('\\end{document}\n')
    union_lines.append('\\end{document}\n')
    write_file(os.path.join(out_dir, 'class.tex'), class_lines)
    write_file(os.path.join(out_dir,  'union.tex'), union_lines)


def get_tabular(filepath):
    df = pd.read_csv(filepath, delimiter=';', header=0)
    count = df.groupby(['minority_decision', 'majority_decision', 'type']).agg('count')
    return '{}\n'.format(count.to_latex())


def df_to_tex(df):
    header = 'Dataset & Sample & \\\\ \n'
    lines = [
        '\\begin{tabular}{lll}\n',
        '\\toprule\n',
        header,
        '\\midrule\n'
    ]
    for i in range(len(df)):
        line = df.loc[i]
        if i == len(df) - 1:
            line = '{} & {} & \\distplotlegend{{{}}}{{{}}}{{{}}}{{{}}} \\\\ \n'.format(*line)
        else:
            line = '{} & {} & \\distplot{{{}}}{{{}}}{{{}}}{{{}}} \\\\ \n'.format(*line)
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
    make_stats(args.results, args.out)


if __name__ == "__main__":
    main()
