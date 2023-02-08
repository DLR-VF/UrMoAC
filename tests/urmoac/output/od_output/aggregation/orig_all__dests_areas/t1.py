sp = {}
spa = {}
with open("sources.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        sp[vals[0]] = float(vals[1])
        spa[vals[0]] = "-1"
        
dp = {}
with open("destinations.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        dp[vals[0]] = float(vals[1])
      


dists = {}
for s in sp:
    for d in dp:
        dist = abs(sp[s] - dp[d])
        a = spa[s]
        if a not in dists:
            dists[a] = {}
        if d not in dists[a]:
            dists[a][d] = [0, 0]
        dists[a][d][0] += dist
        dists[a][d][1] += 1

for a in dists:
    for d in dists[a]:
        dists[a][d][0] = dists[a][d][0] / float(dists[a][d][1])

with open("od_output.urmoac") as fd:
    for l in fd:
        vals = l.strip().split(";")
        if l[0]=='#':
            continue
        if abs(dists[vals[0]][vals[1]][0]-float(vals[2]))>.01:
            print ("Mismatching distances between %s and %s: is %s, wanted: %s" % (vals[0], vals[1], float(vals[2]), dists[vals[0]][vals[1]][0]))
        

