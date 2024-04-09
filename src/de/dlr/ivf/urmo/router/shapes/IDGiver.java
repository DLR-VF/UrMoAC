/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.shapes;

/**
 * @class IDGiver
 * @brief Something that supplies a running id
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public interface IDGiver {
	/** @brief Returns the next running id
	 * @return Next free id
	 */
	public long getNextRunningID();

	
	/** @brief Informs the id giver about a new id
	 * @param id An extern id to regard
	 */
	public void hadExternID(long id);

}
