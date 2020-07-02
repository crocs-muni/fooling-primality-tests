from sage.groups.generic import discrete_log_rho
p  = 0x8b7dada7aa2173f4a3ed9139570386fd2b65eb9ed2232e749385df5532e8349d
k  = GF(p)
a  = k(0x1b27b49f431ab73930736bea17cee09d455a91997a986029807e399713a25ffd)
b  = k(0x6a5d9b63f85d937c868241fb54b5a4671556d46fd92aca1e20b312970b4e759f)
gx = 0x39494395fa2fa85ef2e6d441493e70b1adedaaf74360b9a9cc038c9897fbb42e
gy = 0x4b065332f5369883087e3943518b2da10cf9aa5e28a08f74968206bc2cc9b33e
n  = 27424609 * 33419179 * 37898257 * 39440263 * 49818481 * 52559371 \
     * 53216161 * 59617639 * 61332769 * 90393689

e = EllipticCurve([a, b]); e.set_order(n)
g = e(gx, gy)

privkey = randrange(0, n); pubkey = privkey * g

dlogs = []; mods = []
for factor, power in factor(n):
	mul = Integer(n/factor)
	dlog = discrete_log_rho(mul * pubkey, mul * g, factor, operation="+")
	dlogs.append(dlog); mods.append(factor)

result = CRT_list(dlogs, mods)
print(result == privkey)