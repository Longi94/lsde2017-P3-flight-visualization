import matplotlib.pyplot as plt
import sys

with open(sys.argv[1]) as input_file:
    content = input_file.readlines()
    content = [x.strip().split(',') for x in content]

    longitudes = [float(row[3]) for row in content]
    latitudes = [float(row[4]) for row in content]
    altitudes = [float(row[5]) for row in content]
    timestamps = [float(row[2]) for row in content]

plt.figure(1)
plt.xlabel('Longitude')
plt.ylabel('Latitude')
plt.plot(longitudes, latitudes, 'o')

plt.figure(2)
plt.xlabel('Timestamp')
plt.ylabel('Altitude')
plt.plot(timestamps, altitudes, 'o')

plt.show()
