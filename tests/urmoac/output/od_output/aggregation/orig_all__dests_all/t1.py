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
      


dists = [0, 0]
for s in sp:
    for d in dp:
        dist = abs(sp[s] - dp[d])
        dists[0] += dist
        dists[1] += 1

dists[0] = dists[0] / float(dists[1])

with open("od_output.urmoac") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        if abs(dists[0]-float(vals[2]))>.01:
            print ("Mismatching distances between %s and %s: is %s, wanted: %s" % (vals[0], vals[1], float(vals[2]), dists[0]))
        

