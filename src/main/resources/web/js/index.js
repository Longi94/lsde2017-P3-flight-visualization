'use strict';

const START_TS = 1474156800;
const END_TS = 1474761600;
const CHUNK_INTERVAL = 14400;

$('document').ready(function () {
    copyright("#copyright-div", 2017);
});

// window dimensions
var currentWidth = $('#map').width();
var width = 938;
var height = 620;

// is the timre running?
var timerRunning = false;

// whether the slider is being dragged or not
var sliding = false;

// this aray cointains the flights
var flightBuffer = [];
var flightChunks = [];

var speedMultiplier = 200;

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
    svg.append("g")
        .attr("class", "countries")
        .selectAll("path")
        .data(topojson.feature(countries, countries.objects.countries).features)
        .enter()
        .append("path")
        .attr("d", path);

    initFlights(START_TS);
}).fail(print);

var airlines = {};

// load the airlines json
$.get("json/airlines.json", function (airlinesData) {
    var $airlineSelect = $("#airline");
    $.each(airlinesData, function (i, airline) {
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

// timer that starts the animations, lazyloads flights, etc.
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
        d3.selectAll(".plane").interrupt().attr("class", "plane-preview");
        initFlights(value, true);
    } else {
        $('#time').text(formatTimestamp(value));
        initFlights(value, false);
    }
});

// start the animation if not started
$('#start-button').click(function () {
    if (!timerRunning) {
        startAnim(timeSlider.slider('getValue'));
    }
});

// stop the animation, remove the dots
$('#stop-button').click(function () {
    if (timerRunning) {
        timer.stop();
        d3.selectAll(".plane").interrupt().attr("class", "plane-preview");
        timerRunning = false;
        enableButtons();
    }
});

function restartAnim() {
    if (timerRunning) {
        timer.stop();
        d3.selectAll(".plane").interrupt().attr("class", "plane-preview");
        startAnim(timeSlider.slider('getValue'));
    }
}

/**
 * load the flights from the server
 * @param timestamp
 */
function loadFlights(timestamp) {
    timestamp = timestamp - timestamp % CHUNK_INTERVAL;
    $.get(
        'json/flights/' + timestamp + '.json',
        function (flights) {
            flightChunks[0] = flightChunks[1];
            flightChunks[1] = flights.map(flightMapper);

            flightBuffer = flightBuffer.concat(flightChunks[1]);

            $('#small-loading').hide();
        }
    ).fail(print);
}

/**
 * map the flights to a useable thing
 * @param flight
 * @returns {{coordinates: (Array|*), timestamps: *, altitudes: string}}
 */
function flightMapper(flight) {
    return {
        coordinates: flight.x.map(function (lon, i) {
            return projection([lon, flight.y[i]]);
        }),
        timestamps: flight.t,
        altitudes: flight.z,
        identity: flight.c
    }
}

function initFlights(timestamp, start) {
    $('#loading-overlay').show();

    var jsonTs = timestamp - timestamp % CHUNK_INTERVAL;
    flightChunks = [];
    $.get('json/flights/' + jsonTs + '.json', function (flights) {
        $.get('json/flights/' + (jsonTs + CHUNK_INTERVAL) + '.json', function (nextFlights) {
            flightChunks.push(nextFlights.map(flightMapper));

            $('#loading-overlay').hide();

            if (start) {
                startAnim(timestamp);
            }
        }).fail(print);

        flightChunks.push(flights.map(flightMapper));

        nextPoll = timestamp + CHUNK_INTERVAL - (timestamp % CHUNK_INTERVAL);

        drawPreview(timestamp);
    }).fail(print);
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
        var plane = svg.append("circle")
            .attr("class", "plane-preview")
            .attr("r", "1")
            .attr("transform", "translate(" + coords[0] + "," + coords[1] + ") scale(" + alt / 8000.0 + ")");
    });
}

var previewsRemoved = false;

var nextTimerFrame = 0;

/**
 * Start the animation
 * @param timestamp
 */
function startAnim(timestamp) {
    animationStart = timestamp;
    timerRunning = true;
    disableButtons();

    flightBuffer = flightChunks[0].concat(flightChunks[1]);

    $('.plane').remove();
    previewsRemoved = false;
    nextTimerFrame = 0;
    timer = d3.timer(function (elapsed) {

        if (nextTimerFrame >= elapsed) {
            return;
        }
        nextTimerFrame += 100;

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
            timeSlider.slider('setValue', currentTs);
        }

        $('#time').text(formatTimestamp(currentTs));

        // animate necessary flights
        while (flightBuffer.length > 0 && flightBuffer[0].timestamps[0] < currentTs) {
            fly(flightBuffer[0], currentTs);
            flightBuffer.shift();
        }

        if (!previewsRemoved) {
            $(".plane-preview").remove();
            previewsRemoved = true;
        }
    });
}

function fly(flight, startTs) {
    if (filterFlight(flight)) return;

    if (startTs >= flight.timestamps[flight.timestamps.length - 1]) return;

    while (startTs > flight.timestamps[1]) {
        flight.timestamps.shift();
        flight.coordinates.shift();
        flight.altitudes.shift();
    }

    // dot svg
    var plane = svg.append("circle")
        .attr("class", "plane")
        .attr("r", "1");

    // start the transition
    var t = flight.timestamps[flight.timestamps.length - 1] - startTs;
    plane.transition()
        .duration(t * 1000.0 / speedMultiplier)
        .ease(d3.easeLinear)
        .attrTween("transform", delta(flight, startTs))
        .remove();
}

function filterFlight(flight) {
    if (flightFilter && (!flight.identity || flightFilter !== flight.identity)) {
        return true;
    }

    if (icaoFilter && (!flight.identity || !flight.identity.startsWith(icaoFilter))) {
        return true;
    }

    return false;
}

function delta(flight, startTs) {
    return function () {
        return function (t) {

            var ts = startTs + (flight.timestamps[flight.timestamps.length - 1] - startTs) * t;

            while (ts > flight.timestamps[1] && flight.timestamps.length > 1) {
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
                var step = (ts - flight.timestamps[0]) / (flight.timestamps[1] - flight.timestamps[0]);
                alt = flight.altitudes[0] + step * (flight.altitudes[1] - flight.altitudes[0]);

                var lon = flight.coordinates[0][0] + step * (flight.coordinates[1][0] - flight.coordinates[0][0]);
                var lat = flight.coordinates[0][1] + step * (flight.coordinates[1][1] - flight.coordinates[0][1]);
                coords = [lon, lat];
            }

            return "translate(" + coords[0] + "," + coords[1] + ") scale(" + alt / 8000.0 + ")";
        }
    }
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

function print(o) {
    console.log(o);
}