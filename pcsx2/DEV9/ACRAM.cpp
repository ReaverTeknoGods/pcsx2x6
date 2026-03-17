#include "ACRAM.h"
#include "MemoryTypes.h"
#include "IopMem.h"

u8 ACRAM::Read8(u32 addr) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        return iopMem->ACRAM[T];
    }
    return 0;
}

u16 ACRAM::Read16(u32 addr) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        u16* A = (u16*)&iopMem->ACRAM[T];
        return *A;
    }
    return 0;
}

u32 ACRAM::Read32(u32 addr) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        u32* A = (u32*)&iopMem->ACRAM[T];
        return *A;
    }
    return 0;
}


void ACRAM::Write8(u32 addr, u8 val) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        iopMem->ACRAM[T] = val;
    }
}

void ACRAM::Write16(u32 addr, u16 val) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        u16* A = (u16*)&iopMem->ACRAM[T];
        *A = val;
    }
}

 void ACRAM::Write32(u32 addr, u32 val) {
    u32 T = addr - ACRAM_ADDR_BASE_IOP_POV;
    if (T < ACRAM_MAX_SIZE) {
        u32* A = (u32*)&iopMem->ACRAM[T];
        *A = val;
    }
}