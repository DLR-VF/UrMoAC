package de.dlr.ivf.urmo.router.mivspeeds;

import de.dlr.ivf.urmo.router.shapes.DBEdge;

public class SpeedModel {
	public SpeedModel() {
	}

	
	public double compute(DBEdge e, double t) {
		double vmax = e.getVMax();
		if(vmax<30./3.6) {
			return vmax / 1.5;
		} else if(vmax<=60./3.6) {
			return vmax / 2.;
		} else if(vmax<=80./3.6) {
			return vmax / 1.5;
		} else if(vmax<=120./3.6) {
			return vmax / 1.2;
		} else {
			return 140./3.6;
		}
	}
	
}
