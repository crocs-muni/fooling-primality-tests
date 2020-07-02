#!/usr/bin/env sage

def generate_composite_with_given_number_of_factors(r, bits):
	"""
	Generate a composite with specified bitsize and r prime factors (not necessarily distinct).
	"""
	factors = []
	product = 1
	remainingBits = bits

	while r > 0:
		factorBits = remainingBits/r
		if r == 1:
			factor = random_prime(ceil(2^bits / product), proof = None, lbound = ceil(2^(bits-1) / product))
		else:
			factor = random_prime(floor(2^(factorBits)), proof = None, lbound = floor(2^(factorBits-1)))
		factors.append(factor)
		product *= factor
		remainingBits = bits - product.nbits()
		r -= 1

	assert product.nbits() == bits
	return (product, factors)


def generate_smooth_composite(smoothnessBound, bits, twoAllowed = True):
	"""
	Generate a composite with specified bitsize and smoothness.
	"""
	factors = []
	product = 1
	remainingBits = bits

	while remainingBits > 0:
		if twoAllowed:
			lowerBound = 2
		else:
			lowerBound = 3

		factor = random_prime(smoothnessBound + 1, proof = None, lbound = lowerBound)
		# Make sure the last factor has the correct bitsize
		if remainingBits <= smoothnessBound.nbits() and (product*factor).nbits() != bits:
			product /= factors.pop()
			product = ZZ(product)
			remainingBits = bits - product.nbits()
			continue

		factors.append(factor)
		product *= factor
		remainingBits = bits - product.nbits()

	assert product.nbits() == bits
	return (product, factors)


def contains_prime_factor_of_correct_size(number, factorBits, timeout):
	"""
	Test whether the number contains a factor of given bitsize.

	:return: A two-tuple (whether it contains, and factors as a list if it does)
	"""
	try:
		alarm(timeout)
		factorization = factor(number)
		cancel_alarm()
	except AlarmInterrupt:
		return (False, None)
	print("Factored:", factorization, file=sys.stderr)
	factors = sum(map(lambda x: [x[0]] * x[1], factorization), [])
	factorSizes = list(map(lambda x: x[0].nbits(), factorization))
	if factorBits in factorSizes:
		return (True, factors)
	return (False, None)


def generate_composite_with_given_number_of_factors_with_prime_subgroup_of_correct_size(r, bits, factorBits, timeout=30):
	"""
	:param r: Number of factors
	:param bits: Bit size of composite
	:param factorBits: Bit size of prime factor
	"""
	while True:
		composite, factors = generate_composite_with_given_number_of_factors(r, bits)
		subgroup_found, m1factors = contains_prime_factor_of_correct_size(composite - 1, factorBits, timeout)
		if subgroup_found:
			break
	return (composite, factors, m1factors)


def generate_smooth_composite_with_prime_subgroup_of_correct_size(smoothnessBound, bits, factorBits, twoAllowed=True, timeout=30):
	"""
	:param smoothnessBound: Smoothness bound
	:param bits: Bit size of composite
	:param factorBits: Bit size of prime factor
	:param twoAllowed: Whether 2 is an ok factor
	"""
	while True:
		composite, factors = generate_smooth_composite(smoothnessBound, bits, twoAllowed)
		subgroup_found, m1factors = contains_prime_factor_of_correct_size(composite - 1, factorBits, timeout)
		if subgroup_found:
			break
	return (composite, factors, m1factors)


if __name__ == "__main__":
	import argparse
	import re
	from datetime import timedelta
	regex = re.compile(r'((?P<hours>\d+?)hr)?((?P<minutes>\d+?)m)?((?P<seconds>\d+?)s)?')
	
	def parse_time(time_str):
	    parts = regex.match(time_str)
	    if not parts:
	        return
	    parts = parts.groupdict()
	    time_params = {}
	    for (name, param) in parts.iteritems():
	        if param:
	            time_params[name] = int(param)
	    return int(timedelta(**time_params).total_seconds())
	parser = argparse.ArgumentParser()
	parser.add_argument("bits", type=Integer, help="Bitsize of the composite.")
	parser.add_argument("amount", type=Integer, help="Amount of composites to generate.")
	group = parser.add_mutually_exclusive_group(required=True)
	group.add_argument("--factors", type=Integer, metavar="num", help="Number of factors of the composite of somewhat equal size.")
	group.add_argument("--smooth", type=Integer, metavar="bound", help="Smoothness bound for the composite, inclusive.")
	parser.add_argument("--subgroup", type=Integer, metavar="bits", help="Require composite-1 to have a prime order subgroup of given bitsize. (SLOW, factors composite-1)")
	parser.add_argument("--two", action="store_true", default=False, help="Allow 2 as a factor.")
	parser.add_argument("--timeout", type=parse_time, metavar="time", default=30, help="When a specific prime order subgroup of composite-1 is requested, "
																						"it needs to be factored. This can take a long time, so this provides "
																						"a timeout after which the generation is aborted and new composite is constructed.")
	args = parser.parse_args()

	for i in range(args.amount):
		if args.subgroup:
			if args.factors:
				data = generate_composite_with_given_number_of_factors_with_prime_subgroup_of_correct_size(args.factors, args.bits, args.subgroup, args.timeout)
			if args.smooth:
				data = generate_smooth_composite_with_prime_subgroup_of_correct_size(args.smooth, args.bits, args.subgroup, args.two, args.timeout)
		else:
			if args.factors:
				data = generate_composite_with_given_number_of_factors(args.factors, args.bits)
			if args.smooth:
				data = generate_smooth_composite(args.smooth, args.bits, args.two)
		line = [str(i)]
		for elem in data:
			if isinstance(elem, list):
				line.append(" ".join(map(str, elem)))
			else:
				line.append(str(elem))
		print(",".join(line))
		