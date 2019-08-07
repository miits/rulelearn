import random
import numpy as np
from matplotlib import pyplot as plt
from sklearn.datasets.samples_generator import make_blobs


def gauss_2d(u, s):
    x = random.gauss(u, s)
    y = random.gauss(u, s)
    return (x, y)


def gauss_01_series_2d(length):
    points = [gauss_2d(0, 1) for i in range(length)]
    return points


# points = gauss_01_series_2d(50)
# x = [p[0] for p in points]
# y = [p[1] for p in points]
# plt.scatter(x, y)
# points = gauss_01_series_2d(50)
# x = [p[0] for p in points]
# y = [p[1] for p in points]
# plt.scatter(x, y)
# points = gauss_01_series_2d(50)
# x = [p[0] for p in points]
# y = [p[1] for p in points]
# plt.scatter(x, y)
# plt.show()

centers = [[0, 1], [-1, -1], [1, -1]]
colors = ['tab:blue', 'tab:orange', 'tab:green']
points, labels = make_blobs(n_samples=450, centers=centers, cluster_std=0.3)
distinct_labels = set(labels)
counters = [0] * len(distinct_labels)
size = int(len(points) / len(distinct_labels))
x = np.zeros((size, len(distinct_labels)))
y = np.zeros((size, len(distinct_labels)))
for p, l in zip(points, labels):
    i = counters[l]
    x[i,l] = p[0]
    y[i,l] = p[1]
    counters[l] += 1
for i in distinct_labels:
    plt.scatter(x[:,i], y[:,i])
plt.legend(['Class 1', 'Class 2', 'Class 3'])
plt.savefig('..\\balanced_data.png')