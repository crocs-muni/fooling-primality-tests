# Attacks

This directory contains attack scripts which were used to demonstrate the use of the Pohlig-Hellman decomposition
to recover the key.

`dsa-pohlig-hellman.sage` uses the standard Pohlig-Hellman decomposition to solve the discrete logarithm problem
in a composite order subgroup of size 160 bits over F_p parameters with 1024 bits.

`simple-pohlig-hellman.sage` uses the standard Pohlig-Hellman decomposition to solve the discrete logarithm problem
on a composite order elliptic curve of size 256 bits.

`custom-pohlig-hellman.sage` uses a decomposition similar to that of Pohlig-Hellman to solve the discrete logarithm problem
on a an elliptic curve over Z_p, with p composite of size 256 bits.

The fourth attack that we performed used [CADO-NFS](http://cado-nfs.gforge.inria.fr/) to solve the discrete logarithm problem
in a prime order subgroup of size 160 bits over composite p of size 1024 bits (that split and allowed to compute only a roughly
300 bit discrete log).
