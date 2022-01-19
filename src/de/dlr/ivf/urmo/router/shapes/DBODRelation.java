package de.dlr.ivf.urmo.router.shapes;

/** @class DBODRelation
 * @brief A single origin/destination relation with a weight 
 */
public class DBODRelation {
	/** @brief Constructor
	 * @param o The origin
	 * @param d The destination
	 * @param w The weight
	 */
	public DBODRelation(long o, long d, double w) {
		origin = o;
		destination = d;
		weight = w;
	}
	
	/// @brief The origin
	public long origin;
	
	/// @brief The destination
	public long destination;
	
	/// @brief The weight
	public double weight;
	

}
