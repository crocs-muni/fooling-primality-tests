from sage.groups.generic import discrete_log_rho
p = (0x96c5871fc4eb345f5ce4db9d5411befdd421d26b << (174+42) * 4) | \
	0x782567ad50eb6dbbfe065f015efe2432450af3ad45
k = GF(p)
q = 488740366582603 * 35678046760529947 * 49362777024842803
g = k(int("961f7bc907fd1f03fc1bc37a09098989d0a6c697797791dd59d031c8\
b6f3439cf9cadeafbf9c251bb525c64045984e9bce3fe70cd339b9365\
378adf86d3735e89cff53e76d01edbcff42522d2e26b8147faa0e50bc\
d5bc231a80ee476b24f5207e55fce53e950924f288e22d9d76d319d68\
b57507e7fd811c2e9de5c2aaaebd7", 16))

privkey = randrange(0, q)
pubkey = g^privkey

factors = factor(q)
dlogs = []
mods = []
for factor, power in factors:
	mul = Integer(q/factor)
	dlog = discrete_log_rho(pubkey^mul, g^mul, factor)
	dlogs.append(dlog)
	mods.append(factor)

result = CRT_list(dlogs, mods)
print(result == privkey)
