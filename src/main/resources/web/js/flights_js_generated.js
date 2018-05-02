// automatically generated by the FlatBuffers compiler, do not modify

/**
 * @constructor
 */
function FlightFb() {
  /**
   * @type {flatbuffers.ByteBuffer}
   */
  this.bb = null;

  /**
   * @type {number}
   */
  this.bb_pos = 0;
}

/**
 * @param {number} i
 * @param {flatbuffers.ByteBuffer} bb
 * @returns {FlightFb}
 */
FlightFb.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {FlightFb=} obj
 * @returns {FlightFb}
 */
FlightFb.getRootAsFlightFb = function(bb, obj) {
  return (obj || new FlightFb).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {number} index
 * @returns {number}
 */
FlightFb.prototype.timestamps = function(index) {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.readFloat64(this.bb.__vector(this.bb_pos + offset) + index * 8) : 0;
};

/**
 * @returns {number}
 */
FlightFb.prototype.timestampsLength = function() {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
};

/**
 * @returns {Float64Array}
 */
FlightFb.prototype.timestampsArray = function() {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? new Float64Array(this.bb.bytes().buffer, this.bb.bytes().byteOffset + this.bb.__vector(this.bb_pos + offset), this.bb.__vector_len(this.bb_pos + offset)) : null;
};

/**
 * @param {number} index
 * @returns {number}
 */
FlightFb.prototype.longitudes = function(index) {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? this.bb.readFloat32(this.bb.__vector(this.bb_pos + offset) + index * 4) : 0;
};

/**
 * @returns {number}
 */
FlightFb.prototype.longitudesLength = function() {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
};

/**
 * @returns {Float32Array}
 */
FlightFb.prototype.longitudesArray = function() {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? new Float32Array(this.bb.bytes().buffer, this.bb.bytes().byteOffset + this.bb.__vector(this.bb_pos + offset), this.bb.__vector_len(this.bb_pos + offset)) : null;
};

/**
 * @param {number} index
 * @returns {number}
 */
FlightFb.prototype.latitudes = function(index) {
  var offset = this.bb.__offset(this.bb_pos, 8);
  return offset ? this.bb.readFloat32(this.bb.__vector(this.bb_pos + offset) + index * 4) : 0;
};

/**
 * @returns {number}
 */
FlightFb.prototype.latitudesLength = function() {
  var offset = this.bb.__offset(this.bb_pos, 8);
  return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
};

/**
 * @returns {Float32Array}
 */
FlightFb.prototype.latitudesArray = function() {
  var offset = this.bb.__offset(this.bb_pos, 8);
  return offset ? new Float32Array(this.bb.bytes().buffer, this.bb.bytes().byteOffset + this.bb.__vector(this.bb_pos + offset), this.bb.__vector_len(this.bb_pos + offset)) : null;
};

/**
 * @param {number} index
 * @returns {number}
 */
FlightFb.prototype.altitudes = function(index) {
  var offset = this.bb.__offset(this.bb_pos, 10);
  return offset ? this.bb.readFloat32(this.bb.__vector(this.bb_pos + offset) + index * 4) : 0;
};

/**
 * @returns {number}
 */
FlightFb.prototype.altitudesLength = function() {
  var offset = this.bb.__offset(this.bb_pos, 10);
  return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
};

/**
 * @returns {Float32Array}
 */
FlightFb.prototype.altitudesArray = function() {
  var offset = this.bb.__offset(this.bb_pos, 10);
  return offset ? new Float32Array(this.bb.bytes().buffer, this.bb.bytes().byteOffset + this.bb.__vector(this.bb_pos + offset), this.bb.__vector_len(this.bb_pos + offset)) : null;
};

/**
 * @param {flatbuffers.Encoding=} optionalEncoding
 * @returns {string|Uint8Array|null}
 */
FlightFb.prototype.identity = function(optionalEncoding) {
  var offset = this.bb.__offset(this.bb_pos, 12);
  return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
};

/**
 * @param {flatbuffers.Builder} builder
 */
FlightFb.startFlightFb = function(builder) {
  builder.startObject(5);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} timestampsOffset
 */
FlightFb.addTimestamps = function(builder, timestampsOffset) {
  builder.addFieldOffset(0, timestampsOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {Array.<number>} data
 * @returns {flatbuffers.Offset}
 */
FlightFb.createTimestampsVector = function(builder, data) {
  builder.startVector(8, data.length, 8);
  for (var i = data.length - 1; i >= 0; i--) {
    builder.addFloat64(data[i]);
  }
  return builder.endVector();
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} numElems
 */
FlightFb.startTimestampsVector = function(builder, numElems) {
  builder.startVector(8, numElems, 8);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} longitudesOffset
 */
FlightFb.addLongitudes = function(builder, longitudesOffset) {
  builder.addFieldOffset(1, longitudesOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {Array.<number>} data
 * @returns {flatbuffers.Offset}
 */
FlightFb.createLongitudesVector = function(builder, data) {
  builder.startVector(4, data.length, 4);
  for (var i = data.length - 1; i >= 0; i--) {
    builder.addFloat32(data[i]);
  }
  return builder.endVector();
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} numElems
 */
FlightFb.startLongitudesVector = function(builder, numElems) {
  builder.startVector(4, numElems, 4);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} latitudesOffset
 */
FlightFb.addLatitudes = function(builder, latitudesOffset) {
  builder.addFieldOffset(2, latitudesOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {Array.<number>} data
 * @returns {flatbuffers.Offset}
 */
FlightFb.createLatitudesVector = function(builder, data) {
  builder.startVector(4, data.length, 4);
  for (var i = data.length - 1; i >= 0; i--) {
    builder.addFloat32(data[i]);
  }
  return builder.endVector();
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} numElems
 */
FlightFb.startLatitudesVector = function(builder, numElems) {
  builder.startVector(4, numElems, 4);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} altitudesOffset
 */
FlightFb.addAltitudes = function(builder, altitudesOffset) {
  builder.addFieldOffset(3, altitudesOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {Array.<number>} data
 * @returns {flatbuffers.Offset}
 */
FlightFb.createAltitudesVector = function(builder, data) {
  builder.startVector(4, data.length, 4);
  for (var i = data.length - 1; i >= 0; i--) {
    builder.addFloat32(data[i]);
  }
  return builder.endVector();
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} numElems
 */
FlightFb.startAltitudesVector = function(builder, numElems) {
  builder.startVector(4, numElems, 4);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} identityOffset
 */
FlightFb.addIdentity = function(builder, identityOffset) {
  builder.addFieldOffset(4, identityOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
FlightFb.endFlightFb = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @constructor
 */
function FlightsFb() {
  /**
   * @type {flatbuffers.ByteBuffer}
   */
  this.bb = null;

  /**
   * @type {number}
   */
  this.bb_pos = 0;
}

/**
 * @param {number} i
 * @param {flatbuffers.ByteBuffer} bb
 * @returns {FlightsFb}
 */
FlightsFb.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {FlightsFb=} obj
 * @returns {FlightsFb}
 */
FlightsFb.getRootAsFlightsFb = function(bb, obj) {
  return (obj || new FlightsFb).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {number} index
 * @param {FlightFb=} obj
 * @returns {FlightFb}
 */
FlightsFb.prototype.flights = function(index, obj) {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? (obj || new FlightFb).__init(this.bb.__indirect(this.bb.__vector(this.bb_pos + offset) + index * 4), this.bb) : null;
};

/**
 * @returns {number}
 */
FlightsFb.prototype.flightsLength = function() {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
};

/**
 * @param {flatbuffers.Builder} builder
 */
FlightsFb.startFlightsFb = function(builder) {
  builder.startObject(1);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} flightsOffset
 */
FlightsFb.addFlights = function(builder, flightsOffset) {
  builder.addFieldOffset(0, flightsOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {Array.<flatbuffers.Offset>} data
 * @returns {flatbuffers.Offset}
 */
FlightsFb.createFlightsVector = function(builder, data) {
  builder.startVector(4, data.length, 4);
  for (var i = data.length - 1; i >= 0; i--) {
    builder.addOffset(data[i]);
  }
  return builder.endVector();
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} numElems
 */
FlightsFb.startFlightsVector = function(builder, numElems) {
  builder.startVector(4, numElems, 4);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
FlightsFb.endFlightsFb = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} offset
 */
FlightsFb.finishFlightsFbBuffer = function(builder, offset) {
  builder.finish(offset);
};
