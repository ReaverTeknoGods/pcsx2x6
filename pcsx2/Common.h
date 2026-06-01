// SPDX-FileCopyrightText: 2002-2026 PCSX2 Dev Team
// SPDX-License-Identifier: GPL-3.0+

#pragma once

#include "common/Pcsx2Defs.h"

static const u32 BIAS = 2;				// Bus is half of the actual ps2 speed
// EE bus clock: S246 runs at console speed (294.912 MHz), S256 runs 4/3 faster (393.216 MHz)
static const u32 PS2CLK_DEFAULT = 294912000;	//hz	/* 294.912 mhz — PS2 console / S246 */
static const u32 PS2CLK_S256    = 393216000;	//hz	/* 393.216 mhz — S256 (294.912 * 4/3) */
extern u32 PS2CLK;
extern u32 PSXCLK;	/* 36.864 Mhz (S246) or 49.152 Mhz (S256) */


#include "Memory.h"
#include "R5900.h"
#include "Hw.h"
#include "Dmac.h"

#include "SaveState.h"
#include "DebugTools/Debug.h"

#include <string>

extern std::string ShiftJIS_ConvertString( const char* src );
extern std::string ShiftJIS_ConvertString( const char* src, int maxlen );
