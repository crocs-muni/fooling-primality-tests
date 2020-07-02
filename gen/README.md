# Generation scripts

This directory contains [SageMath](https://sagemath.org) scripts for generation of pseudoprimes, specific composites,
DH, DSA and ECDH/ECDSA domain parameters.

`generate_pseudoprimes.sage <bits> <count>` generates `count` pseudoprimes (they might not be unique) of `bits` bit-length.

`generate_composites.sage <bits> <count>` generates `count` composites of `bits` bit-length. It is possible to request
that the composites have a certain form (are smooth, have a specific number of factors, or that n-1 has a prime divisor of a right size),
for more info see the help of the script (via `-h` switch) or consult the sources.

`generate_dsa.sage <pbits> <qbits>` generates DSA parameters where `p` has `pbits` and `q` divides `p-1` and has `qbits`. The script needs
as input the generated pseudoprimes or composites of the right sizes, for example `sage generate_composites.sage --factors 3 160 1 | sage generate_dsa.sage 1024 160`.

`generate_ec.sage` generates elliptic curve domain parameters given input composites, which are used as the `p` parameter for the curve.

Elliptic curve domain parameters with composite `n` were generated using the [ecgen](https://github.com/J08nY/ecgen) tool, which uses
the complex multiplication method to generate elliptic curves with a chosen order.

## Example pseudoprimes

Some of the used generated pseudoprimes are available in the `example` directory. Others are directly in the `data` directories of the
`ff` and `ec` parts of this repository.
