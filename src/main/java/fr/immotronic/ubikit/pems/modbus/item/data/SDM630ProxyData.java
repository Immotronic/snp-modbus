/*
 * Copyright (c) Immotronic, 2013
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of snp-modbus, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.pems.modbus.item.data;

import java.util.HashMap;
import java.util.Map;

public final class SDM630ProxyData
{
	private final HashMap<String, Float> data;
	
	public SDM630ProxyData()
	{
		data = new HashMap<String, Float>(66);
	}
	
	public void fillData(short[] rawData, int startingAddress, int quantity)
	{
		for (int i = startingAddress; i < startingAddress + quantity; i+=2)
		{
			short data_h = rawData[i - startingAddress];
			short data_l = rawData[i - startingAddress + 1];
			
			float data_i = Float.intBitsToFloat((int)((data_h & 0xffff) << 16) + (data_l & 0xffff));
			if (SDM630Data.isRelevant(i/2+1))
			{
				data.put(Integer.toString(i/2 + 1), data_i);
			}
		}
	}
	
	public Map<String, Float> getValue()
	{
		return data;
	}
}
