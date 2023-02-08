sp = {}
spa = {}
with open("../sources.csv") as fd:
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
with open("../destinations.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        dp[vals[0]] = float(vals[1])
        dpa[vals[0]] = "-1"
        


dists = {}
for s in sp:
    for d in dp:
        dist = abs(sp[s] - dp[d])
        a = spa[s]
        if a not in dists:
            dists[a] = {}
        d = "-1"
        if d not in dists[a]:
            dists[a][d] = [0, 0]
        dists[a][d][0] += dist
        dists[a][d][1] += 1

for a in dists:
    dists[a]["-1"][0] = dists[a]["-1"][0] / float(dists[a]["-1"][1])

with open("od_output.urmoac") as fd:
    for l in fd:
        vals = l.strip().split(";")
        if l[0]=='#':
            continue
        if abs(dists[vals[0]][vals[1]][0]-float(vals[2]))>.01:
            print ("Mismatching distances between %s and %s: is %s, wanted: %s" % (vals[0], vals[1], float(vals[2]), dists[vals[0]][vals[1]][0]))
        

