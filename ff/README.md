# Primality testing in DH/DSA domain parameter validation

To test primality testing in finite field (DH/DSA) domain parameter
validation on JavaCards, we used a custom JavaCard applet.

## Data

The `data` directory contains the input data, organized for DH and DSA separately and for parameter sizes.
The `inputs` subdirectory contains the individual inputs (i.e. pseudomprimes, composites, ...)
and the `params` subdirectory contains the resulting parameters, embedding said inputs.

## Generating

The inputs were generated using the `generate_composites.sage` and `generate_pseudoprimes.sage` scripts.
These were then fed to the `generate_dsa.sage` script to generate the final parameters. See the help
of those scripts in the `../gen` directory for more information.

## Running

The cards were tested using the applet in the `applet` directory and the reader app in the `reader` directory.
Both the applet and the app can be built using `ant` and the `build.xml` antfile.

 1. Build the applet and reader app via `ant build`.
 2. Install the applet (`build/applet_dsa.cap` or `build/applet_full.cap`) on the card.
 For example using [GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro), so
 doing `gp --install build/applet_dsa.cap`. The full version of the applet supports testing
 DH domain parameter validation, which is only available on newer JavaCards, so the DSA-only
 version needs to be used on cards without DH support (the applet cannot be installed otherwise).
 3. Run `java -jar build/reader.jar <data_file>`
