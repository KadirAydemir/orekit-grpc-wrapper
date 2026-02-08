# Orekit gRPC Service API Documentation

This document provides a comprehensive guide to the gRPC services exposed by the Orekit gRPC Wrapper. It includes service definitions, data models, example JSON payloads, and **full Protobuf definitions** for client generation.

## Overview

The API consists of four main services:
1.  **OrbitalService**: Propagates satellite orbits (SGP4/SDP4, Numerical).
2.  **CoordinateTransformService**: Transforms coordinates between reference frames.
3.  **EclipseService**: Calculates satellite eclipse intervals.
4.  **VisibilityService**: Calculates access intervals between satellites and ground stations.
5.  **ManeuverService**: High-fidelity orbit analysis with maneuver detection.

---

## 1. `OrbitalService`

Defined in `orbital_service.proto`.

### Methods

| Method | Description |
| :--- | :--- |
| `Propagate` | Propagates a single state vector to a target duration. |
| `PropagateTLE` | Propagates a single TLE over a time range (Streaming). |
| `BatchPropagateTLE` | Bulk propagation of multiple TLEs (Streaming). |

### Example: `PropagateTLE`

**Request (`TLEPropagateRequest`)**
```json
{
  "model": "SGP4",
  "tle_line1": "1 25544U 98067A   24001.12345678  .00012345  00000-0  12345-3 0  9993",
  "tle_line2": "2 25544  51.6400  20.2000 0005000 100.0000  50.0000 15.50000000123456",
  "start_date": "2024-01-01T00:00:00Z",
  "end_date": "2024-01-01T02:00:00Z",
  "position_count": 60,
  "output_frame": "TEME"
}
```

**Response (`TLEPropagateResponse` Stream)**
```json
{
  "positions": [
    { "x": 6000.1, "y": 1000.2, "z": 500.5, "timestamp": "2024-01-01T00:00:00Z" }
  ],
  "frame": "TEME"
}
```

---

## 2. `CoordinateTransformService`

Defined in `coordinate_transform_service.proto`.

### Methods

| Method | Description |
| :--- | :--- |
| `Transform` | Transforms position and velocity vectors from a source frame to a target frame. |
| `BatchTransform` | Transforms multiple position and velocity vectors (Streaming). |

### Example: `Transform`

**Request (`TransformRequest`)**
```json
{
  "source_frame": "TEME",
  "target_frame": "ITRF",
  "epoch_iso": "2024-01-01T12:00:00Z",
  "x": 7000000.0,
  "y": 0.0,
  "z": 0.0,
  "vx": 0.0,
  "vy": 7500.0,
  "vz": 0.0
}
```

**Response (`TransformResponse`)**
```json
{
  "source_frame": "TEME",
  "target_frame": "ITRF",
  "epoch_iso": "2024-01-01T12:00:00Z",
  "x": 6000123.45,
  "y": 3000567.89,
  "z": 1000.00,
  "vx": -450.0,
  "vy": 7200.0,
  "vz": 10.0
}
```

---

## 3. `EclipseService`

Defined in `eclipse_service.proto`.

### Methods

| Method | Description |
| :--- | :--- |
| `CalculateEclipses` | Calculates eclipse intervals for a satellite within a given date range. |
| `BatchCalculateEclipses` | Calculates eclipse intervals for multiple satellites (Streaming). |

### Example: `CalculateEclipses`

**Request (`EclipseRequest`)**
```json
{
  "tle_line1": "1 25544U ...",
  "tle_line2": "2 25544 ...",
  "start_date_iso": "2024-01-01T00:00:00Z",
  "end_date_iso": "2024-01-02T00:00:00Z"
}
```

**Response (`EclipseResponse`)**
```json
{
  "norad_id": 25544,
  "intervals": [
    {
      "start_iso": "2024-01-01T00:30:00Z",
      "end_iso": "2024-01-01T01:00:00Z",
      "duration_seconds": 1800.0
    },
    {
      "start_iso": "2024-01-01T02:00:00Z",
      "end_iso": "2024-01-01T02:30:00Z",
      "duration_seconds": 1800.0
    }
  ]
}
```


## 4. `VisibilityService`

Defined in `visibility_service.proto`.

### Methods

| Method | Description |
| :--- | :--- |
| `GetAccessIntervals` | Computes visibility (access) intervals between a TLE-defined satellite and a ground station. |
| `BatchGetAccessIntervals` | Computes visibility intervals for multiple satellites (Streaming). |

### Example: `GetAccessIntervals`

**Request (`AccessIntervalsRequest`)**
```json
{
  "tle_line1": "1 25544U ...",
  "tle_line2": "2 25544 ...",
  "ground_station": {
    "name": "Ankara Ground Station",
    "latitude_degrees": 39.9334,
    "longitude_degrees": 32.8597,
    "altitude_meters": 850.0
  },
  "start_date_iso": "2024-01-01T00:00:00Z",
  "end_date_iso": "2024-01-02T00:00:00Z",
  "min_elevation_degrees": 10.0
}
```

**Response (`AccessIntervalsResponse`)**
```json
{
  "satellite_name": "ISS (ZARYA)",
  "station_name": "Ankara Ground Station",
  "intervals": [
    {
      "start_iso": "2024-01-01T04:15:00Z",
      "end_iso": "2024-01-01T04:25:00Z",
      "duration_seconds": 600.0
    }
  ]
}
```

---

## 5. `ManeuverService`

Defined in `maneuver_service.proto`.

### Methods

| Method | Description |
| :--- | :--- |
| `DetectManeuvers` | Performs high-fidelity numerical propagation to detect maneuvers by comparing with observed TLEs. |

### Example: `DetectManeuvers`

**Request (`ManeuverDetectionRequest`)**
```json
{
  "initial_tle": {
    "tle_line1": "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991",
    "tle_line2": "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005"
  },
  "observed_tles": [
    {
      "tle_line1": "1 25544U 98067A   24001.50000000  .00016717  00000-0  10270-3 0  9991",
      "tle_line2": "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005"
    }
  ],
  "config": {
    "gravity_degree": 10,
    "gravity_order": 10
  },
  "maneuver_threshold_km": 2.0,
  "output_frame": "TEME"
}
```

**Response (`ManeuverDetectionResponse`)**
```json
{
  "maneuvers": [
    {
      "timestamp": "2024-01-01T12:00:00Z",
      "residual_km": 2.5,
      "delta_v_estimate_m_s": 0.5,
      "description": "Maneuver detected at 2024-01-01T12:00:00Z: position residual = 2.50 km, estimated delta-V = 0.50 m/s"
    }
  ],
  "frame": "TEME"
}
```

---

# Appendix: Protobuf Definitions

**Dependencies:**
- `coordinate_transform_service.proto`, `eclipse_service.proto`, and `visibility_service.proto` import or rely on definitions in common (though currently mainly `coordinate_transform_service` and `visibility_service` import `orbital_service.proto`).
- Ensure all files are in the same directory (or configure your proto path accordingly) when generating code.
- Package name: `orbital`
- Java Package: `tr.com.kadiraydemir.orekit.grpc`

### `orbital_service.proto`

```protobuf
syntax = "proto3";

package orbital;

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc";
option java_outer_classname = "OrbitalServiceProto";

// Propagation model selection
enum PropagationModel {
  AUTO = 0;      // Auto-select SGP4/SDP4 based on TLE period
  SGP4 = 1;      // Force SGP4 (near-Earth objects)
  SDP4 = 2;      // Force SDP4 (deep-space objects)
  NUMERICAL = 3; // High-fidelity numerical propagator
}

// Integrator type for numerical propagation
enum IntegratorType {
  DORMAND_PRINCE_853 = 0;  // Default - 8th order with embedded 5th & 3rd order
  DORMAND_PRINCE_54 = 1;   // 5th order with embedded 4th order
  CLASSICAL_RUNGE_KUTTA = 2; // Classical 4th order Runge-Kutta
  ADAMS_BASHFORTH = 3;     // Adams-Bashforth multi-step
  ADAMS_MOULTON = 4;       // Adams-Moulton multi-step (implicit)
  GRAGG_BULIRSCH_STOER = 5; // Gragg-Bulirsch-Stoer extrapolation
}

// Reference frame for output coordinates
enum ReferenceFrame {
  TEME = 0;   // True Equator Mean Equinox (default, native SGP4/SDP4 frame)
  GCRF = 1;   // Geocentric Celestial Reference Frame
  EME2000 = 2; // Earth Mean Equator and Equinox of J2000
  ITRF = 3;   // International Terrestrial Reference Frame (Earth-fixed)
}

service OrbitalService {
  rpc Propagate (PropagateRequest) returns (PropagateResponse) {}
  rpc PropagateTLE (TLEPropagateRequest) returns (stream TLEPropagateResponse) {}
  // Batch Input -> Stream Output
  rpc BatchPropagateTLE (BatchTLEPropagateRequest) returns (stream BatchTLEPropagateResponse) {}
}

message PropagateRequest {
  string satellite_name = 1;
  double semimajor_axis = 2; // meters
  double eccentricity = 3;
  double inclination = 4; // radians
  double perigee_argument = 5; // radians
  double right_ascension_of_ascending_node = 6; // radians
  double mean_anomaly = 7; // radians
  string epoch_iso = 8; // ISO-8601 string, e.g. "2024-01-01T12:00:00Z"
  double duration = 9; // seconds to propagate
}

message PropagateResponse {
  string satellite_name = 1;
  double pos_x = 2;
  double pos_y = 3;
  double pos_z = 4;
  double vel_x = 5;
  double vel_y = 6;
  double vel_z = 7;
  string final_date_iso = 8;
  string frame_name = 9;
}

message TLEPropagateRequest {
  PropagationModel model = 1;
  string tle_line1 = 2;
  string tle_line2 = 3;
  string start_date = 4; // ISO-8601
  string end_date = 5; // ISO-8601
  int32 position_count = 6;
  ReferenceFrame output_frame = 7; // Output reference frame (default: TEME)
  IntegratorType integrator = 8;  // Integrator type for numerical model (ignored for SGP4/SDP4)
}

message PositionPoint {
  double x = 1;
  double y = 2;
  double z = 3;
  string timestamp = 4;
}

message TLEPropagateResponse {
  repeated PositionPoint positions = 1;
  string frame = 2; // Reference frame name (applies to all positions)
}

message TLELines {
  string tle_line1 = 1;
  string tle_line2 = 2;
}

message BatchTLEPropagateResponse {
  int32 satellite_id = 1;
  repeated PositionPoint positions = 2;
  string frame = 3;
  string error = 4;
}

message BatchTLEPropagateRequest {
  PropagationModel model = 1;
  string start_date = 2; // ISO-8601
  string end_date = 3; // ISO-8601
  int32 position_count = 4;
  ReferenceFrame output_frame = 5;
  IntegratorType integrator = 6;
  repeated TLELines tles = 7;
}
```

### `coordinate_transform_service.proto`

```protobuf
syntax = "proto3";

package orbital;

import "orbital_service.proto";

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc";
option java_outer_classname = "CoordinateTransformServiceProto";

service CoordinateTransformService {
    // Transform coordinates from one frame to another
    rpc Transform (TransformRequest) returns (TransformResponse) {}

    // Transform multiple coordinates (streaming response)
    rpc BatchTransform (BatchTransformRequest) returns (stream TransformResponse) {}
}

message TransformRequest {
    ReferenceFrame source_frame = 1;
    ReferenceFrame target_frame = 2;
    string epoch_iso = 3; // ISO-8601 string, e.g. "2024-01-01T12:00:00Z"
    
    // Position and Velocity in source frame
    double x = 4;
    double y = 5;
    double z = 6;
    double vx = 7;
    double vy = 8;
    double vz = 9;
}

message BatchTransformRequest {
    ReferenceFrame source_frame = 1;
    ReferenceFrame target_frame = 2;
    // Multiple state vectors to transform (all at the same epoch)
    repeated StateVector state_vectors = 3;
    string epoch_iso = 4;
}

message StateVector {
    // Position in source frame
    double x = 1;
    double y = 2;
    double z = 3;
    // Velocity in source frame
    double vx = 4;
    double vy = 5;
    double vz = 6;
}

message TransformResponse {
    ReferenceFrame source_frame = 1;
    ReferenceFrame target_frame = 2;
    string epoch_iso = 3;

    // Transformed Position and Velocity in target frame
    double x = 4;
    double y = 5;
    double z = 6;
    double vx = 7;
    double vy = 8;
    double vz = 9;
    // Error message for partial failures - empty if successful
    string error = 10;
}
```

### `eclipse_service.proto`

```protobuf
syntax = "proto3";

package orbital;

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc";
option java_outer_classname = "EclipseServiceProto";

service EclipseService {
  // Calculate eclipse intervals for a satellite
  rpc CalculateEclipses (EclipseRequest) returns (EclipseResponse) {}

  // Calculate eclipse intervals for multiple satellites (streaming response)
  rpc BatchCalculateEclipses (BatchEclipseRequest) returns (stream EclipseResponse) {}
}

message EclipseRequest {
  string tle_line1 = 1;
  string tle_line2 = 2;
  string start_date_iso = 3;
  string end_date_iso = 4;
}

message BatchEclipseRequest {
  // Multiple TLEs to process
  repeated TLEPair tles = 1;
  // Common date range for all satellites
  string start_date_iso = 2;
  string end_date_iso = 3;
}

message TLEPair {
  string line1 = 1;
  string line2 = 2;
}

message EclipseResponse {
    int32 norad_id = 1;
    repeated EclipseInterval intervals = 2;
    // Error message for partial failures - empty if successful
    string error = 3;
}

message EclipseInterval {
    string start_iso = 1;
    string end_iso = 2;
    double duration_seconds = 3;
}
```

### `visibility_service.proto`

```protobuf
syntax = "proto3";

package orbital;

import "orbital_service.proto";

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc";
option java_outer_classname = "VisibilityServiceProto";

service VisibilityService {
    // Calculate access intervals between a satellite and a ground station
    rpc GetAccessIntervals (AccessIntervalsRequest) returns (AccessIntervalsResponse) {}

    // Calculate access intervals for multiple satellites (streaming response)
    rpc BatchGetAccessIntervals (BatchAccessIntervalsRequest) returns (stream AccessIntervalsResponse) {}
}

message AccessIntervalsRequest {
    string tle_line1 = 1;
    string tle_line2 = 2;
    GroundStation ground_station = 3;
    string start_date_iso = 4;
    string end_date_iso = 5;
    double min_elevation_degrees = 6; // Minimum elevation angle in degrees (default 0)
}

message GroundStation {
    double latitude_degrees = 1;
    double longitude_degrees = 2;
    double altitude_meters = 3;
    string name = 4;
}

message BatchAccessIntervalsRequest {
    // Multiple TLEs to process
    repeated TLELines tles = 1;
    // Common ground station for all satellites
    GroundStation ground_station = 2;
    // Common date range for all satellites
    string start_date_iso = 3;
    string end_date_iso = 4;
    double min_elevation_degrees = 5;
}

message AccessIntervalsResponse {
    string satellite_name = 1;
    string station_name = 2;
    repeated AccessInterval intervals = 3;
    // Error message for partial failures - empty if successful
    string error = 4;
}

message AccessInterval {
    string start_iso = 1;
    string end_iso = 2;
    double duration_seconds = 3;
}
```
### `maneuver_service.proto`

```protobuf
syntax = "proto3";

package maneuver;

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc.maneuver";
option java_outer_classname = "ManeuverServiceProto";

// Reference frame for output coordinates
enum ReferenceFrame {
  TEME = 0;   // True Equator Mean Equinox (default, native SGP4/SDP4 frame)
  GCRF = 1;   // Geocentric Celestial Reference Frame
  EME2000 = 2; // Earth Mean Equator and Equinox of J2000
  ITRF = 3;   // International Terrestrial Reference Frame (Earth-fixed)
}

service ManeuverService {
  // High-fidelity orbit analysis with maneuver detection
  rpc DetectManeuvers (ManeuverDetectionRequest) returns (ManeuverDetectionResponse) {}
}

message TLELines {
  string tle_line1 = 1;
  string tle_line2 = 2;
}

// Force model configuration for numerical propagation
message ForceModelConfig {
  int32 gravity_degree = 1;
  int32 gravity_order = 2;
  bool solar_radiation_pressure = 3;
  bool atmospheric_drag = 4;
}

// Request for maneuver detection
message ManeuverDetectionRequest {
  TLELines initial_tle = 1;
  repeated TLELines observed_tles = 2;
  ForceModelConfig config = 3;
  double maneuver_threshold_km = 4;
  ReferenceFrame output_frame = 6;
}

// Report for detected maneuvers
message ManeuverReport {
  string timestamp = 1;
  double residual_km = 2;
  double delta_v_estimate_m_s = 3;
  string description = 4;
}

// Response for maneuver detection
message ManeuverDetectionResponse {
  repeated ManeuverReport maneuvers = 1;
  string frame = 3;
}
```
