// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vtop__Syms.h"


VL_ATTR_COLD void Vtop___024root__trace_init_sub__TOP__0(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_init_sub__TOP__0\n"); );
    // Init
    const int c = vlSymsp->__Vm_baseCode;
    // Body
    tracep->declBus(c+1,"cpudbgdata", false,-1, 23,0);
    tracep->declBus(c+2,"HEX5", false,-1, 6,0);
    tracep->declBus(c+3,"HEX4", false,-1, 6,0);
    tracep->declBus(c+4,"HEX3", false,-1, 6,0);
    tracep->declBus(c+5,"HEX2", false,-1, 6,0);
    tracep->declBus(c+6,"HEX1", false,-1, 6,0);
    tracep->declBus(c+7,"HEX0", false,-1, 6,0);
    tracep->pushNamePrefix("top ");
    tracep->declBus(c+1,"cpudbgdata", false,-1, 23,0);
    tracep->declBus(c+2,"HEX5", false,-1, 6,0);
    tracep->declBus(c+3,"HEX4", false,-1, 6,0);
    tracep->declBus(c+4,"HEX3", false,-1, 6,0);
    tracep->declBus(c+5,"HEX2", false,-1, 6,0);
    tracep->declBus(c+6,"HEX1", false,-1, 6,0);
    tracep->declBus(c+7,"HEX0", false,-1, 6,0);
    tracep->pushNamePrefix("seg0 ");
    tracep->declBus(c+8,"in", false,-1, 3,0);
    tracep->declBus(c+7,"out", false,-1, 6,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("seg1 ");
    tracep->declBus(c+9,"in", false,-1, 3,0);
    tracep->declBus(c+6,"out", false,-1, 6,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("seg2 ");
    tracep->declBus(c+10,"in", false,-1, 3,0);
    tracep->declBus(c+5,"out", false,-1, 6,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("seg3 ");
    tracep->declBus(c+11,"in", false,-1, 3,0);
    tracep->declBus(c+4,"out", false,-1, 6,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("seg4 ");
    tracep->declBus(c+12,"in", false,-1, 3,0);
    tracep->declBus(c+3,"out", false,-1, 6,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("seg5 ");
    tracep->declBus(c+13,"in", false,-1, 3,0);
    tracep->declBus(c+2,"out", false,-1, 6,0);
    tracep->popNamePrefix(2);
}

VL_ATTR_COLD void Vtop___024root__trace_init_top(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_init_top\n"); );
    // Body
    Vtop___024root__trace_init_sub__TOP__0(vlSelf, tracep);
}

VL_ATTR_COLD void Vtop___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vtop___024root__trace_chg_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vtop___024root__trace_cleanup(void* voidSelf, VerilatedVcd* /*unused*/);

VL_ATTR_COLD void Vtop___024root__trace_register(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_register\n"); );
    // Body
    tracep->addFullCb(&Vtop___024root__trace_full_top_0, vlSelf);
    tracep->addChgCb(&Vtop___024root__trace_chg_top_0, vlSelf);
    tracep->addCleanupCb(&Vtop___024root__trace_cleanup, vlSelf);
}

VL_ATTR_COLD void Vtop___024root__trace_full_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp);

VL_ATTR_COLD void Vtop___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_full_top_0\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    Vtop___024root__trace_full_sub_0((&vlSymsp->TOP), bufp);
}

VL_ATTR_COLD void Vtop___024root__trace_full_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_full_sub_0\n"); );
    // Init
    uint32_t* const oldp VL_ATTR_UNUSED = bufp->oldp(vlSymsp->__Vm_baseCode);
    // Body
    bufp->fullIData(oldp+1,(vlSelf->cpudbgdata),24);
    bufp->fullCData(oldp+2,(vlSelf->HEX5),7);
    bufp->fullCData(oldp+3,(vlSelf->HEX4),7);
    bufp->fullCData(oldp+4,(vlSelf->HEX3),7);
    bufp->fullCData(oldp+5,(vlSelf->HEX2),7);
    bufp->fullCData(oldp+6,(vlSelf->HEX1),7);
    bufp->fullCData(oldp+7,(vlSelf->HEX0),7);
    bufp->fullCData(oldp+8,((0xfU & vlSelf->cpudbgdata)),4);
    bufp->fullCData(oldp+9,((0xfU & (vlSelf->cpudbgdata 
                                     >> 4U))),4);
    bufp->fullCData(oldp+10,((0xfU & (vlSelf->cpudbgdata 
                                      >> 8U))),4);
    bufp->fullCData(oldp+11,((0xfU & (vlSelf->cpudbgdata 
                                      >> 0xcU))),4);
    bufp->fullCData(oldp+12,((0xfU & (vlSelf->cpudbgdata 
                                      >> 0x10U))),4);
    bufp->fullCData(oldp+13,((0xfU & (vlSelf->cpudbgdata 
                                      >> 0x14U))),4);
}
