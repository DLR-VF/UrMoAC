sp = {}
with open("sources.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        sp[vals[0]] = float(vals[1])

dp = {}
with open("destinations.csv") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        dp[vals[0]] = float(vals[1])

with open("od_output.urmoac") as fd:
    for l in fd:
        if l[0]=='#':
            continue
        vals = l.strip().split(";")
        dist = abs(sp[vals[0]] - dp[vals[1]])
        if dist!=float(vals[2]):
            print ("Mismatching distances between %s and %s: is %s, wanted: %s" % (vals[0], vals[1], float(vals[2]), dist))
        

