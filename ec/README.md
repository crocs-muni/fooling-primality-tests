# Primality testing in EC domain parameter validation

To test primality testing in elliptic curve domain parameter
validation on JavaCards, we used our [ECTester](https://github.com/crocs-muni/ECTester) tool.

## Data

The `data` directory contains the input data, organized by curve size, for sizes from
160 bits to 521 bits. The `inputs` subdirectory contains the individual inputs (i.e. pseudomprimes, composites, ...)
and the `params` subdirectory contains the resulting curves, embedding said input parameters.
The files in the `params` subdirectory are directly usable with ECTester (the same csv-based format is used).

## Generating

The inputs were generated using the `generate_composites.sage` and `generate_pseudoprimes.sage` scripts.
These were then fed to the `generate_ec.sage` script to generate the final parameters. The [ecgen](https://github.com/J08nY/ecgen) tool
was also used to generate elliptic curve domain parameters with composite/chosen order.  See the help
of those scripts in the `../gen` directory for more information.

## Running

The cards were tested using ECTester, with commands similar to this one (which does 100 ECDSA signatures
with a fixed keypair using the 256-bit parameters with pseudoprime `n`.):
```
java -jar ECTesterReader.jar --ecdsa 100 --fp -b 256 --curve data/256/params/n/pseudo.csv --fixed
```

The ECTester applet needs to be installed on the card for the above command to work. See the [ECTester](https://github.com/crocs-muni/ECTester)
repository for instructions.
