package de.dlr.ivf.urmo.router.shapes;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;

public class DBODRelationExt extends DBODRelation {
	public DBODRelationExt(long o, long d, double w) {
		super(o, d, w);
	}
	public DBEdge fromEdge;
	public MapResult fromMR;
	public DBEdge toEdge;
	public MapResult toMR;

}
