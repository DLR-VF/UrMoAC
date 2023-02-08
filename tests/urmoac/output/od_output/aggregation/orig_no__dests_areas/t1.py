sp = {}
spa = {}
with open("sources.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        sp[vals[0]] = float(vals[1])
        if float(vals[1])>0:
          if float(vals[2])>0:
            a = "100003"
          else:
            a = "100001"
        else:
          if float(vals[2])>0:
            a = "100002"
          else:
            a = "100000"
        spa[vals[0]] = a
        
dp = {}
dpa = {}
with open("destinations.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        dp[vals[0]] = float(vals[1])
        if float(vals[1])>0:
          if float(vals[2])>0:
            a = "200003"
          else:
            a = "200001"
        else:
          if float(vals[2])>0:
            a = "200002"
          else:
            a = "200000"
        dpa[vals[0]] = a
        


dists = {}
for s in sp:
    for d in dp:
        dist = abs(sp[s] - dp[d])
        a = dpa[d]
        if s not in dists:
            dists[s] = {}
        if a not in dists[s]:
            dists[s][a] = [0, 0]
        dists[s][a][0] += dist
        dists[s][a][1] += 1

for s in dists:
    for a in dists[s]:
        dists[s][a][0] = dists[s][a][0] / float(dists[s][a][1])

with open("od_output.urmoac") as fd:
    for l in fd:
        vals = l.strip().split(";")
        if l[0]=='#':
            continue
        if abs(dists[vals[0]][vals[1]][0]-float(vals[2]))>.01:
            print ("Mismatching distances between %s and %s: is %s, wanted: %s" % (vals[0], vals[1], float(vals[2]), dists[vals[0]][vals[1]][0]))
        

