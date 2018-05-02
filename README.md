# Flight Visualization

Big Data project for the [Large Scale Engineering](https://event.cwi.nl/lsde/2017/index.shtml) course at Vrije Universiteit Amsterdam.

## The task

Commercial airplanes periodically send out radio messages containing their position details (plane identifier, flight number, latitude, longitude, height, speed, ...) . These [ADS-B messages](https://en.wikipedia.org/wiki/Automatic_dependent_surveillance_%E2%80%93_broadcast) are picked up by enthusiasts and collected in systems such as the [OpenSky network](https://opensky-network.org/) or [Flightradar24](http://www.flightradar24.com/). We have obtained ~200 GB of compressed ADS-B messages from September 2015 in [a compressed format](https://github.com/openskynetwork/osky-sample).

Generate an interactive flight path animation (GIF?) of all flights based on their accurate location data. Speed up time. Reduce amount of flights if necessary through stratified sampling of diverse flight routes. [Non-interactive Example](https://www.youtube.com/watch?v=Q6XpgV0DZd4).

## Paper

The documenting paper is available [here](https://event.cwi.nl/lsde/2017/results/group06.pdf).

## Build

Build the project using gradle.

```./gradlew build```

## Working with the cluster

The project contains a bash script to provide a convenient way to submit spark jobs to a yarn cluster. Keep in mind, that this script will automatically provide the first input parameter which are the folders containing the raw messages.

```./submit-job.sh [jar file] [main class] [parameters]```

Where the jar file is the built project and the main class is the spark job class to run on the cluster. The class ```in.dragonbra.CsvToJson``` must be run locally as it requires network connection and produces files in the current working directory.

## Visualization

The visualization is a static html/javascript web page found in the src/main/resources/web/ directory. The visualization is currently avaiable at https://dragonbra.in/flight-visualization/.
