# Remove latitude/longitude flight-path output from simulations and exports

## Rationale

OpenRocket's flight simulator computed the rocket's geographic position (latitude/longitude) over the course of a flight and exposed it as a first-class simulation output. Because that data is a time-ordered geographic trace of a vehicle's flight path, it can be fed directly into mapping software, and that raises ITAR (export-control) concerns for some users.

The concern is specifically about **producing usable geographic trajectory output** — a lat/lng trace that leaves the application via a plot, a data export, or a foreign-format file. It is *not* about the app internally knowing the launch-site coordinate, which is an ordinary physics input.

This change removes the ability to view or export the rocket's lat/lng flight path, while leaving the simulation physics fully intact.

## Proposal

- Stop exposing the rocket's per-timestep latitude/longitude as simulation output, so it can no longer be **plotted** or **exported to CSV**.
- Stop writing latitude/longitude into **foreign-format exports** (RockSim).
- **Keep** the launch-site latitude/longitude *input*: it drives the WGS84 gravity model and the Coriolis/geodetic computation, is a single fixed point the user enters themselves, and removing it would degrade the simulation. It stays in the Launch Conditions UI and in native `.ork` save/load.
- **Scrub on load**: legacy `.ork` files that already stored a lat/lng trace should have those columns dropped when opened, so the data is neither displayed nor written back out on the next save. (Files that already left the app in the past are out of scope — nothing can be done about data that already escaped.)

## What changed

### Removed the lat/lng simulation output type

- **`core/.../simulation/FlightDataType.java`** — deleted the `TYPE_LATITUDE` and `TYPE_LONGITUDE` flight-data type definitions and their entries in the master type list. Because the plot selectors and CSV exporters enumerate whatever types exist in a flight-data branch, removing the types automatically removes lat/lng from every plot dropdown and CSV column list — there was no lat/lng-specific plot or export code to change.
- **`core/.../simulation/SimulationStatus.java`** — removed the two `storeData()` writes that populated `TYPE_LATITUDE` / `TYPE_LONGITUDE` into each flight-data point.

### Stopped exporting launch-site coordinates to RockSim

- **`core/.../file/rocksim/export/SimulationResultsDTO.java`** — removed the `launchLatitude` / `launchLongitude` JAXB fields, their assignment in `copyLaunchConditions(...)`, and their getters, so exported `.rkt` files no longer contain launch coordinates.
- **`core/.../file/rocksim/RockSimCommonConstants.java`** — removed the now-unused `LAUNCH_LATITUDE` / `LAUNCH_LONGITUDE` XML element-name constants.

### Scrub lat/lng from previously-saved files on load

- **`core/.../file/openrocket/importt/FlightDataBranchHandler.java`** — when reading a stored `<databranch>`, any column whose saved identifier is a latitude/longitude type is now dropped. A `BLOCKED_TYPE_NAMES` set matches the language-independent save keys (`latitude`, `longitude`) and the legacy English display names. The per-column type array is kept full-width (with a `null` entry for each dropped column) so the datapoint comma-count validation still holds and the surviving columns keep their correct values. Dropped columns are parsed but never stored, and the branch is constructed without those types — so they are not shown, not exported, and not written back out on the next save.

### Tests

- **`core/.../file/rocksim/export/RockSimSimulationExportTest.java`** — dropped the two assertions that expected launch lat/lng in the RockSim export (the DTO no longer carries them). The test still sets the launch coordinates on the simulation options, exercising that the physics input is preserved.
- **`core/.../file/openrocket/importt/FlightDataBranchHandlerLatLonTest.java`** *(new)* — loads a legacy-style `.ork` whose stored flight data has `latitude`/`longitude` columns placed between and after other columns, then asserts no lat/lng type survives and that the surrounding columns (time, altitude) keep their correctly-aligned values across all data points.

## Explicitly not changed

- The launch-site latitude/longitude **input** (`SimulationOptions.getLaunchLatitude/Longitude`), its Launch Conditions UI, its native `.ork` persistence, and its use in the gravity/Coriolis models (`WorldCoordinate` launch site, `GeodeticComputationStrategy`, `WGSGravityModel`).
- All other simulation outputs, plots, and exports.

## Verification

- `:core:compileJava`, `:core:compileTestJava`, and `:swing:compileJava` compile cleanly.
- The loader (`file.openrocket.*`), RockSim export (`file.rocksim.*`), and simulation (`simulation.*`) test suites pass, including the existing simulation-data save/load round-trip and the new lat/lng scrub test.

## Known limitation

A `.ork` saved by a very old, non-English OpenRocket build (from before stable save keys existed) could have stored localized column names (e.g. "Breitengrad"). Those specific legacy files would load the column as an "Unknown" custom value rather than a clean lat/lng type; they are not matched by the English/save-key scrub. Any file written by a modern build is fully covered, since those always use the `latitude` / `longitude` save keys.
