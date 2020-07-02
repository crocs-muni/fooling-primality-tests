#!/usr/bin/env sage

import sys


proof.arithmetic(False)


def create_DSA_params(q, pbits):
    qbits = q.nbits()
    diffbits = pbits - qbits
    
    #find suitable p
    for k in range(0,1000000,2):
        p = q*(2^diffbits + k) +1
        if is_prime(p):
            break
    
    #find suitable g
    R = IntegerModRing(p)
    for r in R:
        g = r^((p-1)/q)
        if g != R(1) and g != 0:
            break
    return p, g


def create_DSA_generator(p, q):
    #find suitable g
    R = IntegerModRing(p)
    for r in R:
        g = r^((p-1)/q)
        if g != R(1) and g != 0:
            break
    return g


if __name__ == "__main__":
	import csv
	import argparse
	parser = argparse.ArgumentParser()
	parser.add_argument("pbits", type=int)
	parser.add_argument("qbits", type=int)

	args = parser.parse_args()
	
	reader = csv.reader(sys.stdin)
	for line in reader:
		if len(line) == 4:
			# id, composite(p), p factors, p - 1 factors
			p = Integer(line[1])
			for fact in line[4].split(" "):
				fact = Integer(fact)
				if fact.nbits() == args.qbits:
					q = fact
					break
			else:
				print("q could not be found.", file=sys.stderr)
				continue
			g = create_DSA_generator(p, q)
			
		elif len(line) == 3:
			# id, composite(q), q factors
			q = Integer(line[1])
			p, g = create_DSA_params(q, args.pbits)			
		else:
			print("Whoops.", file=sys.stderr)
			continue

		print(",".join((str(p), str(q), str(g))))