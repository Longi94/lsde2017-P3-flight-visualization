'use strict';

const OFFSET_TS = 1474156800;
const START_TS = 0;
const END_TS = 604800;
const CHUNK_INTERVAL = 14400;
const FPS = 25;

$('document').ready(function () {
    copyright("#copyright-div", 2017);
});

// window dimensions
var currentWidth = $('#map').width();
var width = 938;
var height = 620;

// is the timer running?
var timerRunning = false;

// whether the slider is being dragged or not
var sliding = false;

// the current and the next chunk of flights
var flightChunks = [];

// flights waiting to be animated
var flightBuffer = [];

// flights currently animated
var animBuffer = [];

// airline data
var airlines = {};

// how fast the animation goes
var speedMultiplier = 200;

// flight filters
var icaoFilter = "";
var flightFilter = "";

// projection to convert lon-lat to
var projection = d3
    .geoMercator()
    .scale(800)
    .translate([200, 1100]);

// paths n stuff
var path = d3.geoPath()
    .pointRadius(2)
    .projection(projection);

// the whole thing
var svg = d3.select("#map")
    .append("svg")
    .attr("preserveAspectRatio", "xMidYMid")
    .attr("viewBox", "0 0 " + width + " " + height)
    .attr("width", currentWidth)
    .attr("height", currentWidth * height / width);

// load the countries json
$.get("json/countries.topo.json", function (countries) {
    // create svg from topojson
    /** @namespace countries.objects.countries */
    svg.append("g")
        .attr("class", "countries")
        .selectAll("path")
        .data(topojson.feature(countries, countries.objects.countries).features)
        .enter()
        .append("path")
        .attr("d", path);

    initFlights(START_TS);
}).fail(print);

// load the airlines json
$.get("json/airlines.json", function (airlinesData) {
    var $airlineSelect = $("#airline");
    $.each(airlinesData, function (i, airline) {
        /** @namespace airline.icao */
        /** @namespace airline.flightIdentities */
        airlines[airline.icao] = airline.flightIdentities;
        $airlineSelect.append("<option value='" + airline.icao + "'>" + airline.name + "</option>");
    });
}).fail(print);

// the slider object
var timeSlider = $('#time-slider').slider({
    formatter: function (timestamp) {
        return formatTimestamp(timestamp);
    }
});

// timer that starts the animations, lazy-loads flights, etc.
var timer;

// which timestamp did the animation start at
var animationStart;

// the next timestamp poll
var nextPoll;

var sliderDateFormat;

// keep the ratio of the map on screen resize
$(window).resize(function () {
    currentWidth = $("#map").width();
    svg.attr("width", currentWidth);
    svg.attr("height", currentWidth * height / width);
});

$("#speed").change(function () {
    speedMultiplier = $(this).val();
    restartAnim();
});

$("#airline").change(function () {
    icaoFilter = $(this).val();
    flightFilter = "";
    if (icaoFilter) {
        var $flightCodeSelect = $("#flight-code").empty().append("<option value=''>ALL</option>");
        $.each(airlines[icaoFilter], function (i, flights) {
            $flightCodeSelect.append("<option value='" + flights + "'>" + flights + "</option>")
        });

        $("#flight-code-div").show();
    } else {
        $("#flight-code").empty();
        $("#flight-code-div").hide();
    }
    if (!timerRunning) {
        drawPreview(timeSlider.slider('getValue'));
    }
    restartAnim();
});

$("#flight-code").change(function () {
    flightFilter = $(this).val();
    if (!timerRunning) {
        drawPreview(timeSlider.slider('getValue'));
    }
    restartAnim();
});

// called when the slider is clicked on
timeSlider.slider('on', 'slideStart', function () {
    sliding = true;
});

// called when the slider is released
timeSlider.slider('on', 'slideStop', function (value) {
    sliding = false;

    if (timerRunning) {
        timer.stop();
        d3.selectAll(".plane").attr("class", "plane-preview");
        initFlights(value - OFFSET_TS, true);
    } else {
        $('#time').text(formatTimestamp(value));
        initFlights(value - OFFSET_TS, false);
    }
});

// start the animation if not started
$('#start-button').click(function () {
    if (!timerRunning) {
        startAnim(timeSlider.slider('getValue') - OFFSET_TS);
    }
});

// stop the animation, remove the dots
$('#stop-button').click(function () {
    if (timerRunning) {
        timer.stop();
        d3.selectAll(".plane").attr("class", "plane-preview");
        timerRunning = false;
        enableButtons();
    }
});

function restartAnim() {
    if (timerRunning) {
        timer.stop();
        d3.selectAll(".plane").attr("class", "plane-preview");
        startAnim(timeSlider.slider('getValue') - OFFSET_TS);
    }
}

function flightsFbToFlights(result) {
    var data = new Uint8Array(result);
    var buf = new flatbuffers.ByteBuffer(data);
    var flightsFb = FlightsFb.getRootAsFlightsFb(buf);

    var flights = [];

    for (var i = 0; i < flightsFb.flightsLength(); i++) {
        var flightFb = flightsFb.flights(i);

        var coordinates = [];
        var altitudes = [];
        var timestamps = [];
        var identity = flightFb.identity();

        for (var j = 0; j < flightFb.timestampsLength(); j++) {
            timestamps.push(flightFb.timestamps(j));
            altitudes.push(flightFb.altitudes(j));
            coordinates.push(projection([flightFb.longitudes(j), flightFb.latitudes(j)]));
        }

        flights.push({
            coordinates: coordinates,
            timestamps: timestamps,
            altitudes: altitudes,
            identity: identity
        });
    }

    return flights;
}

/**
 * load the flights from the server
 * @param timestamp
 */
function loadFlights(timestamp) {
    timestamp = timestamp - timestamp % CHUNK_INTERVAL;
    downloadBinary('bin/flights/' + timestamp + '.bin', function (flights) {
        flightChunks[0] = flightChunks[1];

        flightChunks[1] = flightsFbToFlights(flights);

        flightBuffer = flightBuffer.concat(flightChunks[1]);

        $('#small-loading').hide();
    });
}

function initFlights(timestamp, start) {
    $('#loading-overlay').show();

    var jsonTs = timestamp - timestamp % CHUNK_INTERVAL;
    flightChunks = [];

    downloadBinary('bin/flights/' + jsonTs + '.bin', function (flights) {
        downloadBinary('bin/flights/' + (jsonTs + CHUNK_INTERVAL) + '.bin', function (nextFlights) {
            flightChunks.push(flightsFbToFlights(nextFlights));

            $('#loading-overlay').hide();

            if (start) {
                startAnim(timestamp);
            }
        });

        flightChunks.push(flightsFbToFlights(flights));

        nextPoll = timestamp + CHUNK_INTERVAL - (timestamp % CHUNK_INTERVAL);

        drawPreview(timestamp);
    });
}

function drawPreview(timestamp) {
    $('circle').remove();

    $.each(flightChunks[0], function (i, flight) {

        if (flight.timestamps.length === 1 ||
            timestamp < flight.timestamps[0] ||
            timestamp > flight.timestamps[flight.timestamps.length - 1]) return;

        if (filterFlight(flight)) return;

        var index = 0;
        while (timestamp > flight.timestamps[index + 1]) {
            index++;
        }

        var alt;
        var coords;
        if (index >= flight.timestamps.length - 1) {
            alt = flight.altitudes[flight.altitudes - 1];
            coords = flight.coordinates[flight.coordinates - 1];
        } else {
            var step = (timestamp - flight.timestamps[index]) / (flight.timestamps[index + 1] - flight.timestamps[index]);
            alt = flight.altitudes[index] + step * (flight.altitudes[index + 1] - flight.altitudes[index]);

            var lon = flight.coordinates[index][0] + step * (flight.coordinates[index + 1][0] - flight.coordinates[index][0]);
            var lat = flight.coordinates[index][1] + step * (flight.coordinates[index + 1][1] - flight.coordinates[index][1]);
            coords = [lon, lat];
        }

        // dot svg
        svg.append("circle")
            .attr("class", "plane-preview")
            .attr("r", "1")
            .attr("transform", "translate(" + coords[0] + "," + coords[1] + ") scale(" + alt / 8000.0 + ")");
    });
}

/**
 * Start the animation
 * @param timestamp
 */
function startAnim(timestamp) {
    animationStart = timestamp;
    timerRunning = true;
    disableButtons();

    flightBuffer = flightChunks[0].concat(flightChunks[1]);
    animBuffer = [];

    $('.plane').remove();
    previewsRemoved = false;
    nextTimerFrame = 0;
    timer = d3.timer(timerDelta);
}

/**
 * Add flights to the anim buffer that should start animating
 * @param timestamp
 */
function extendAnimationBuffer(timestamp) {
    while (flightBuffer.length > 0 && flightBuffer[0].timestamps[0] < timestamp) {
        if (!filterFlight(flightBuffer[0])) {
            flightBuffer[0].plane = svg.append("circle")
                .attr("class", "plane")
                .attr("r", "1");

            animBuffer.push(flightBuffer[0]);
        }
        flightBuffer.shift();
    }
}

/**
 * Remove flights from the anim buffer that have already ended
 * @param timestamp
 */
function pruneAnimationBuffer(timestamp) {
    animBuffer = animBuffer.filter(function (flight) {
        if (flight.timestamps.length === 0 || flight.timestamps[flight.timestamps.length - 1] < timestamp) {
            flight.plane.remove();
            return false;
        }
        return true;
    });
}

var previewsRemoved = false;

var nextTimerFrame = 0;

function timerDelta(elapsed) {
    if (nextTimerFrame >= elapsed) {
        return;
    }
    nextTimerFrame += 1000 / FPS;

    var currentTs = animationStart + (elapsed / 1000) * speedMultiplier;
    if (currentTs > END_TS) {
        timer.stop();
        timerRunning = false;
        enableButtons();
    }

    // load the next batch
    if (nextPoll < currentTs) {
        nextPoll += CHUNK_INTERVAL;
        $('#small-loading').show();
        loadFlights(nextPoll);
    }

    // do not update to allow sliding
    if (!sliding) {
        timeSlider.slider('setValue', currentTs + OFFSET_TS);
    }

    $('#time').text(formatTimestamp(currentTs + OFFSET_TS));

    // animate necessary flights
    extendAnimationBuffer(currentTs);
    pruneAnimationBuffer(currentTs);

    $.each(animBuffer, function (i, flight) {
        while (currentTs > flight.timestamps[1] && flight.timestamps.length > 1) {
            flight.timestamps.shift();
            flight.coordinates.shift();
            flight.altitudes.shift();
        }

        var alt;
        var coords;
        if (flight.timestamps.length === 1) {
            alt = flight.altitudes[0];
            coords = flight.coordinates[0];
        } else {
            var step = (currentTs - flight.timestamps[0]) / (flight.timestamps[1] - flight.timestamps[0]);
            alt = flight.altitudes[0] + step * (flight.altitudes[1] - flight.altitudes[0]);

            var lon = flight.coordinates[0][0] + step * (flight.coordinates[1][0] - flight.coordinates[0][0]);
            var lat = flight.coordinates[0][1] + step * (flight.coordinates[1][1] - flight.coordinates[0][1]);
            coords = [lon, lat];
        }

        flight.plane.attr("transform", "translate(" + coords[0] + "," + coords[1] + ") scale(" + alt / 8000.0 + ")");
    });

    if (!previewsRemoved) {
        $(".plane-preview").remove();
        previewsRemoved = true;
    }
}

/**
 * True if the flight should be filtered out.
 * @param flight
 * @returns {boolean}
 */
function filterFlight(flight) {
    return (flightFilter && (!flight.identity || flightFilter !== flight.identity)) ||
        (icaoFilter && (!flight.identity || !flight.identity.startsWith(icaoFilter)));
}

function formatTimestamp(timestamp) {
    if (sliderDateFormat === undefined) {
        sliderDateFormat = new Date();
    }
    sliderDateFormat.setTime(timestamp * 1000);
    return sliderDateFormat.toUTCString();
}

function enableButtons() {
    $("#stop-button").attr("disabled", "disabled");
    $("#start-button").removeAttr("disabled");
}

function disableButtons() {
    $("#start-button").attr("disabled", "disabled");
    $("#stop-button").removeAttr("disabled");
}

// convenience
function print(o) {
    console.log(o);
}

function downloadBinary(url, handleResult) {
    var xhttp = new XMLHttpRequest();
    xhttp.open("GET", url, true);
    xhttp.responseType = "arraybuffer";

    xhttp.onload = function (event) {
        handleResult(xhttp.response);
    };

    xhttp.onerror = function () {
        console.error('GET ' + url + xhttp.status);
    };

    xhttp.send();
}