#!/usr/bin/env sage

import sys
import csv
from sage.schemes.elliptic_curves.constructor import coefficients_from_j


proof.arithmetic(False)


def local_curves_to_global_with_info(localCurves, jInv):
	globalCard = 1
	primes = []
	crtCoefficientsForA = []
	crtCoefficientsForB = []
	crtCoefficientsForGx = []
	crtCoefficientsForGy = []
	for (curve, factors) in localCurves:
		prime = ZZ(curve.base_field().characteristic())
		localCard = factors.prod()
		if curve.change_ring(GF(prime)).cardinality() != localCard:
			print >> sys.stderr, "Bad local card"
			return None

		globalCard *= localCard

		a = curve.a_invariants()[3]
		b = curve.a_invariants()[4]
		crtCoefficientsForA.append(ZZ(a))
		crtCoefficientsForB.append(ZZ(b))
		primes.append(prime)

		#generate a generator point on each curve
		G = curve.gen(0)
		crtCoefficientsForGx.append(ZZ(G[0]))
		crtCoefficientsForGy.append(ZZ(G[1]))

	#combine the curves into one
	pseudoprime = prod(primes)
	a = CRT_list(crtCoefficientsForA, primes)
	b = CRT_list(crtCoefficientsForB, primes)
	E = EllipticCurve(Integers(pseudoprime), [a, b])
	Gx = CRT_list(crtCoefficientsForGx, primes)
	Gy = CRT_list(crtCoefficientsForGy, primes)
	
	# check if cardinalities agree
	# expCard = 1
	#for prime in primes:
	 	#expCard *= E.change_ring(GF(prime)).cardinality()
		#print "exp E:", factor(E.change_ring(GF(prime)).cardinality())
		#print "exp twist:", factor(E.change_ring(GF(prime)).quadratic_twist().cardinality())
	
	#check if the cardinality annihilates the generator point
	G = E(Gx,Gy)
	if globalCard*G != E(0):
		print("G does not annhilate.", file=sys.stderr)
		return None

	return (E, Gx, Gy, globalCard, jInv, primes)


def find_global_curve(pseudoprimeFactors, jInvRange, smallestFactorBound = 50, bottomUp = True):
	if (not bottomUp):
		jInv = jInvRange[0]
		while jInv < jInvRange[1] or jInvRange[1] == 0:
			localCurves = []
			for prime in pseudoprimeFactors:
				R = Integers(prime)
				j = R(jInv)
				E = EllipticCurve_from_j(j)
				twist = E.quadratic_twist()
				fact = factor(E.cardinality())
				factTwist = factor(twist.cardinality())
		
				#choose the twist with the largest smallest factors
				if ((fact[0][0] < factTwist[0][0]) or (fact[0][0] == factTwist[0][0] and fact[0][1] > factTwist[0][1])):
					E, fact = twist, factTwist
				if fact[0][0] < smallestFactorBound:
					break
				localCurves.append((E, fact))
			else:
				print("Got local curves:", jInv, file=sys.stderr)
				globalCurveWithInfo = local_curves_to_global_with_info(localCurves, jInv)
				if globalCurveWithInfo is None:
					jInv += 1
					continue			
				return globalCurveWithInfo
			jInv += 1
	else:
		localCurves = []
		for prime in pseudoprimeFactors:
			R = Integers(prime)
			while True:
				a = ZZ.random_element()
				b = ZZ.random_element()
				try:
					E = EllipticCurve(R, [a,b])
					if is_prime(E.cardinality()):
						localCurves.append((E, factor(E.cardinality())))
						break
				except ArithmeticError:
					continue
		globalCurveWithInfo = local_curves_to_global_with_info(localCurves, 1)
		return globalCurveWithInfo	


def get_parameters_from_global_curve(globalCurveWithInfo):
	#parse the parameters into the format expected by the smartcard together with pseudoprime factors
	(E, Gx, Gy, globalCard, jInv, primes) = globalCurveWithInfo
	pp = E.base_ring().characteristic()
	a = ZZ(E.a_invariants()[3])
	b = ZZ(E.a_invariants()[4])
	gx = Gx
	gy = Gy
	n = globalCard
	h = 1
	return [pp,a,b,gx,gy,n,h]


def write_global_curves_to_file(globalCurve, outFile):
	writer = csv.writer(outFile)
	pp = globalCurve[0]
	fieldsHex = []
	for elem in globalCurve[:-1]:
		#pad with zeros
		fieldsHex.append('{0:0{1}x}'.format(elem, ceil(pp.nbits() / 8) * 2))
	fieldsHex.append('{0:04x}'.format(globalCurve[-1]))
	writer.writerow(fieldsHex)


def generate_all_params_for_given_pseudoprime(pseudoprimeFactors, outFile, jInvRange, smallestFactorBound):
	#see if this contains a prime power
	if len(set(pseudoprimeFactors)) != len(pseudoprimeFactors):
		print("Prime power.", file=sys.stderr)
		return
	globalCurveWithInfo = find_global_curve(pseudoprimeFactors, jInvRange, smallestFactorBound)
	if globalCurveWithInfo is None:
		print("Reached jInvEnd.", file=sys.stderr)
		return
	globalCurveParams = get_parameters_from_global_curve(globalCurveWithInfo)
	write_global_curves_to_file(globalCurveParams, outFile)


if __name__ == "__main__":
	import argparse
	parser = argparse.ArgumentParser()
	parser.add_argument("--jinvStart", metavar="jinv", type=int, default=1)
	parser.add_argument("--jinvEnd", metavar="jinv", type=int, default=10000)
	parser.add_argument("--factorBound", metavar="bound", type=int, default=50)
	
	args = parser.parse_args()
	reader = csv.reader(sys.stdin)
	for i, composite, factors in reader:
		factors = list(map(int, factors.split(" ")))
		generate_all_params_for_given_pseudoprime(factors, sys.stdout, [args.jinvStart, args.jinvEnd], args.factorBound)
