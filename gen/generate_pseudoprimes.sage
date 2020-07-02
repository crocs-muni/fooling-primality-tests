#!/usr/bin/env sage

import random
import sys


proof.arithmetic(False)


def millerrabinliar(n,a):
    r = 0
    m = n-1
    while m % 2 == 0:
        r += 1
        m = m/2
    if Mod(a,n)^m == Mod(1,n):
        return True
    for i in range(r):
        if Mod(a,n)^(m*2^i) == Mod(-1,n):
            return True
    return False


# returns three primes whose product is a pseudoprime
def arnault3(bases, k2, k3):
    try:
        (ks_ok, list_of_intersections) = check_ks(bases, k2, k3)
    except:
        return None
    if not ks_ok:
        return None
    while True:
        p1 = construct_p1(list_of_intersections, k2, k3)
        if not is_prime(p1):
            continue
        print(".", file=sys.stderr, end="", flush=True)
        p2 = k2*(p1-1)+1
        if not is_prime(p2):
            continue
        print(";", file=sys.stderr, end="", flush=True)
        p3 = k3*(p1-1)+1
        if is_prime(p3):
            print("#", file=sys.stderr, end="", flush=True)
            break
    # try to factor, p1-1, p2-1, p3-1, if any has a factor of correct size, then output
    n = p1*p2*p3
    assert ((n-1)%(p1-1) == 0 and (n-1)%(p2-1) == 0 and (n-1)%(p3-1) == 0 and is_prime(p1) and is_prime(p2) and is_prime(p3))
    return n, p1, p2, p3

 
def compute_Sa_intersection(a, k2, k3):
    Sa = []
    for x in range(4*a):
        if kronecker(a,x) == -1 and gcd(x,4*a) == 1 and x%4 == 3:
            Sa.append(x)
    k2i = k2.inverse_mod(4*a)
    Sa2 = [k2i*(s+k2-1)%(4*a) for s in Sa]
    k3i = k3.inverse_mod(4*a)
    Sa3 = [k3i*(s+k3-1)%(4*a) for s in Sa]
    return (Set(Sa).intersection(Set(Sa2))).intersection(Set(Sa3)).list()


def check_ks(bases, k2, k3):
    if k2 == k3:
    	return False, []
    list_of_intersections = []
    for a in bases:
        intersection = compute_Sa_intersection(a, k2, k3)
        if intersection == []:
            return False, []
        list_of_intersections.append((a, intersection))
    return True, list_of_intersections


def construct_p1(list_of_intersections, k2, k3):
    p1 = crt(-1* k3.inverse_mod(k2), -1 * k2.inverse_mod(k3), k2, k3)
    modulus = k2*k3
    p1 = crt(p1, 3, modulus, 8)
    modulus = lcm(4, modulus)
    for a, intersection in list_of_intersections:
        if a % 2 == 0:
            continue
        z = random.choice(intersection)
        p1 = crt(p1, z, modulus, 4 * a)
        modulus = modulus * a
    return p1


base_k2 = 10
base_k3 = 10
#heuristically try specific baseBound, k2, k3 based on target bitsize
def generate_pseudoprime_near_given_size(bitsize, optimize=False):
    global base_k2, base_k3
    assert bitsize in [160, 192, 224, 256, 384, 512, 521, 1024, 2048, 4096]
    if bitsize == 160:
    	baseBound, k2, k3 = 11, 73, 101
    elif bitsize == 192:
        baseBound, k2, k3 = 13, 61, 101
    elif bitsize == 224:
        baseBound, k2, k3 = 14, 197, 257
    elif bitsize == 256:
        baseBound, k2, k3 = 16, 233, 101
    elif bitsize == 384:
        baseBound, k2, k3 = 23, 137, 157
    elif bitsize == 512:
        baseBound, k2, k3 = 30, 137, 157
    elif bitsize == 521:
        baseBound, k2, k3 = 30, 137, 157
    elif bitsize == 1024:
        baseBound, k2, k3 = 52, 241, 281
    elif bitsize == 2048:
    	baseBound, k2, k3 = 92, 673, 733
    elif bitsize == 4096:
        baseBound, k2, k3 = 164, 977, 997

    if optimize:
        k2, k3 = base_k2, base_k3
        base_k3 += 1
        if base_k3 >= 500:
            base_k2 += 1
            base_k3 = base_k2

    print("Doing", baseBound, k2, k3, file=sys.stderr)
    bases = primes_first_n(baseBound)
    result = arnault3(bases, k2, k3)
    
    return result, baseBound, k2, k3


def generate_pseudoprime_of_exactly_given_size(bitsize, optimize=False):
    while True:
        arnault, baseBound, k2, k3 = generate_pseudoprime_near_given_size(bitsize, optimize)
        if arnault == None:
        	continue
        print(arnault[0].nbits(), arnault[1].nbits(), arnault[2].nbits(), arnault[3].nbits(), file=sys.stderr)
        if arnault[0].nbits() == bitsize:
            break
    return arnault, baseBound, k2, k3


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("bits", type=int)
    parser.add_argument("count", type=int)
    parser.add_argument("--optimize", action="store_true", default=False)
    args = parser.parse_args()
    
    #write pseudoprime data ppCount times
    for i in range(args.count):
        arnault, baseBound, k2, k3 = generate_pseudoprime_of_exactly_given_size(args.bits, args.optimize)
        pp, p1, p2, p3 = arnault
        print(",".join([str(i), str(pp), str(p1)]), p2, p3)
