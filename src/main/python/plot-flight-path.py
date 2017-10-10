import matplotlib.pyplot as plt
from matplotlib.path import Path
from mpl_toolkits.mplot3d import Axes3D
import sys
import numpy as np
import matplotlib.collections as mcoll


# source: https://stackoverflow.com/a/25941474
def colorline(
        x, y, z=None, cmap=plt.get_cmap('copper'), norm=plt.Normalize(0.0, 1.0),
        linewidth=3, alpha=1.0):
    """
    http://nbviewer.ipython.org/github/dpsanders/matplotlib-examples/blob/master/colorline.ipynb
    http://matplotlib.org/examples/pylab_examples/multicolored_line.html
    Plot a colored line with coordinates x and y
    Optionally specify colors in the array z
    Optionally specify a colormap, a norm function and a line width
    """

    # Default colors equally spaced on [0,1]:
    if z is None:
        z = np.linspace(0.0, 1.0, len(x))

    # Special case if a single number:
    if not hasattr(z, "__iter__"):  # to check for numerical input -- this is a hack
        z = np.array([z])

    z = np.asarray(z)

    segments = make_segments(x, y)
    lc = mcoll.LineCollection(segments, array=z, cmap=cmap, norm=norm,
                              linewidth=linewidth, alpha=alpha)

    ax = plt.gca()
    ax.add_collection(lc)

    return lc


def make_segments(x, y):
    """
    Create list of line segments from x and y coordinates, in the correct format
    for LineCollection: an array of the form numlines x (points per line) x 2 (x
    and y) array
    """

    points = np.array([x, y]).T.reshape(-1, 1, 2)
    segments = np.concatenate([points[:-1], points[1:]], axis=1)
    return segments


with open(sys.argv[1]) as input_file:
    content = input_file.readlines()
    content = [x.strip().split(',') for x in content]
    content = sorted(content, key=lambda x: x[2])

    longitudes = [float(row[3]) for row in content]
    latitudes = [float(row[4]) for row in content]
    vertices = [(float(row[3]), float(row[4])) for row in content]
    altitudes = [float(row[5]) for row in content]
    timestamps = [float(row[2]) for row in content]

plt.figure()
plt.xlabel('Longitude')
plt.ylabel('Latitude')
plt.plot(longitudes, latitudes, 'o', markersize=0.5)

# fig = plt.figure()
# ax = fig.gca(projection='3d')
# ax.plot(longitudes, latitudes, altitudes)
# ax.set_xlabel('Longitude')
# ax.set_ylabel('Latitude')
# ax.set_zlabel('Altitude')

# fig = plt.figure()
# ax = fig.gca(projection='3d')
# N = len(longitudes)
# for i in range(N - 1):
#     ax.plot(longitudes[i:i + 2], latitudes[i:i + 2], altitudes[i:i + 2], color=plt.cm.jet(int(255 * i / N)))
# ax.set_xlabel('Longitude')
# ax.set_ylabel('Latitude')
# ax.set_zlabel('Altitude')

codes = [Path.MOVETO] + [Path.LINETO] * (len(vertices) - 1)
path = Path(vertices, codes)
verts = path.interpolated(steps=3).vertices
x, y = verts[:, 0], verts[:, 1]
fig, ax = plt.subplots()
ax.set_xlim(min(longitudes) - 1, max(longitudes) + 1)
ax.set_ylim(min(latitudes) - 1, max(latitudes) + 1)
z = np.linspace(0, 1, len(x))
colorline(x, y, z, cmap=plt.get_cmap('jet'), linewidth=2)

plt.figure()
plt.xlabel('Timestamp')
plt.ylabel('Altitude')
plt.plot(timestamps, altitudes, '.', markersize=0.5)

plt.show()
