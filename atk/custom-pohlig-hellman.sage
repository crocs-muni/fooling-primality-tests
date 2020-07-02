from sage.groups.generic import discrete_log
p  = 28260319 * 30235481 * 39172037 * 39191063 * 41237249 * 47624921 \
	 * 51042223 * 71578097 * 77171399 * 107659879
a  = 0x84a477c83f88e833a49b562869f1553a4abbf7ffe29893ca272bf85b300cfe43
b  = 0x9567df38696eec2e80b4f43d056621c639938361b58260e12df91ac528c1ee2c
gx = 0x8e6da816bc1bd86cc7b9d393c08bcb9cdb44a016f44890419542ae43f34f9041
gy = 0x68e8c1d9e8ad5d256cfcf161c41090b5a7bbd3c7ca83f3cc185e289d8ce6ca0e
n  = 0xacc602e17e38aa923887566d83b95ec21b72368cc6a8565bd907f71d4824e67d

e = EllipticCurve(Integers(p), [a, b])
g = e(gx, gy)

privkey = randrange(0, n); pubkey = privkey * g
pub_x, pub_y = pubkey.xy()
pub_x, pub_y = lift(pub_x), lift(pub_y)

dlogs = []; mods = []
for factor, power in factor(p):
	kf = GF(factor)
	ef = EllipticCurve([kf(a), kf(b)])
	gf = ef(kf(gx), kf(gy))
	pf = ef(kf(pub_x), kf(pub_y))
	dlog = discrete_log(pf, gf, n, operation="+")
	dlogs.append(dlog); mods.append(gf.order())

result = CRT_list(dlogs, mods) % n
print(result == privkey)