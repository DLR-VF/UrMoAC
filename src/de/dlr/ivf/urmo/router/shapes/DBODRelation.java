package de.dlr.ivf.urmo.router.shapes;

public class DBODRelation {
	public DBODRelation(long o, long d, double w) {
		origin = o;
		destination = d;
		weight = w;
	}
	public long origin;
	public long destination;
	public double weight;

}
