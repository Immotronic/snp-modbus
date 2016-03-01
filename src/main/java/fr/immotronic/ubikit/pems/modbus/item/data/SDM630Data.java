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

public enum SDM630Data
{
	PHASE1_VOLT(1, "V"),
	PHASE2_VOLT(2, "V"),
	PHASE3_VOLT(3, "V"),
	PHASE1_CURRENT(4, "A"),
	PHASE2_CURRENT(5, "A"),
	PHASE3_CURRENT(6, "A"),
	PHASE1_POWER(7, "W"),
	PHASE2_POWER(8, "W"),
	PHASE3_POWER(9, "W"),
	PHASE1_VA(10, "VA"),
	PHASE2_VA(11, "VA"),
	PHASE3_VA(12, "VA"),
	PHASE1_VA_REACTIVE(13, "VAr"),
	PHASE2_VA_REACTIVE(14, "VAr"),
	PHASE3_VA_REACTIVE(15, "VAr"),
	PHASE1_POWER_FACTOR(16, ""),
	PHASE2_POWER_FACTOR(17, ""),
	PHASE3_POWER_FACTOR(18, ""),
	PHASE1_ANGLE(19, "&deg;"),
	PHASE2_ANGLE(20, "&deg;"),
	PHASE3_ANGLE(21, "&deg;"),
	AVERAGE_VOLT(22, "V"),
	AVERAGE_CURRENT(24, "A"),
	SUM_CURRENT(25, "A"),
	TOTAL_POWER(27, "W"),
	TOTAL_VA(29, "VA"),
	TOTAL_VA_REACTIVE(31, "VAr"),
	TOTAL_POWER_FACTOR(32, ""),
	TOTAL_PHASE_ANGLE(34, "&deg;"),
	SUPPLY_VOLTAGE_FREQUENCY(36, "Hz"),
	IMPORT_Wh(37, "kWh"),
	EXPORT_Wh(38, "kWh"),
	IMPORT_VARh(39, "kVArh"),
	EXPORT_VARh(40, "kVArh"),
	VAH(41, "kVAh"),
	AH(42, "Ah"),
	POWER_DEMAND(43, "W"),
	MAX_POWER_DEMAND(44, "W"),
	VA_DEMAND(51, "VA"),
	MAX_VA_DEMAND(52, "VA"),
	CURRENT_DEMAND(53, "A"),
	MAX_CURRENT_DEMAND(54, "A"),
	LINE1_TO_LINE2_VOLT(101, "V"),
	LINE2_TO_LINE3_VOLT(102, "V"),
	LINE3_TO_LINE1_VOLT(103, "V"),
	AVERAGE_LINE_TO_LINE_VOLT(104, "V"),
	NEUTRAL_CURRENT(113, "A"),
	PHASE1_LN_VOLTS_THD(118, "%"),
	PHASE2_LN_VOLTS_THD(119, "%"),
	PHASE3_LN_VOLTS_THD(120, "%"),
	PHASE1_CURRENT_THD(121, "%"),
	PHASE2_CURRENT_THD(122, "%"),
	PHASE3_CURRENT_THD(123, "%"),
	AVERAGE_LINE_TO_NEUTRAL_VOLT_THD(125, "%"),
	AVERAGE_LINE_CURRENT_THD(126, "%"),
	POWER_FACTOR_INV(128, "&deg;"),
	PHASE1_CURRENT_DEMAND(130, "A"),
	PHASE2_CURRENT_DEMAND(131, "A"),
	PHASE3_CURRENT_DEMAND(132, "A"),
	MAX_PHASE1_CURRENT_DEMAND(133, "A"),
	MAX_PHASE2_CURRENT_DEMAND(134, "A"),
	MAX_PHASE3_CURRENT_DEMAND(135, "A"),
	LINE1_TO_LINE2_VOLT_THD(168, "%"),
	LINE2_TO_LINE3_VOLT_THD(169, "%"),
	LINE3_TO_LINE1_VOLT_THD(170, "%"),
	AVERAGE_LINE_TO_LINE_VOLT_THD(171, "%");
	
	final int index;
	final String unit;
	
	private SDM630Data(int index, String unit)
	{
		this.index = index;
		this.unit = unit;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getUnit() {
		return unit;
	}
	
	static boolean isRelevant(int index)
	{
		for (SDM630Data d : SDM630Data.values()) {
			if (d.index == index) {
				return true;
			}
		}
		return false;
	}
	
	public static SDM630Data getValue(int index)
	{
		for (SDM630Data d : SDM630Data.values()) {
			if (d.index == index) {
				return d;
			}
		}
		return null;
	}
}
