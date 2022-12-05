// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vtop__Syms.h"


void Vtop___024root__trace_chg_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp);

void Vtop___024root__trace_chg_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_chg_top_0\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    if (VL_UNLIKELY(!vlSymsp->__Vm_activity)) return;
    // Body
    Vtop___024root__trace_chg_sub_0((&vlSymsp->TOP), bufp);
}

void Vtop___024root__trace_chg_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_chg_sub_0\n"); );
    // Init
    uint32_t* const oldp VL_ATTR_UNUSED = bufp->oldp(vlSymsp->__Vm_baseCode + 1);
    // Body
    bufp->chgIData(oldp+0,(vlSelf->cpudbgdata),24);
    bufp->chgCData(oldp+1,(vlSelf->HEX5),7);
    bufp->chgCData(oldp+2,(vlSelf->HEX4),7);
    bufp->chgCData(oldp+3,(vlSelf->HEX3),7);
    bufp->chgCData(oldp+4,(vlSelf->HEX2),7);
    bufp->chgCData(oldp+5,(vlSelf->HEX1),7);
    bufp->chgCData(oldp+6,(vlSelf->HEX0),7);
    bufp->chgCData(oldp+7,((0xfU & vlSelf->cpudbgdata)),4);
    bufp->chgCData(oldp+8,((0xfU & (vlSelf->cpudbgdata 
                                    >> 4U))),4);
    bufp->chgCData(oldp+9,((0xfU & (vlSelf->cpudbgdata 
                                    >> 8U))),4);
    bufp->chgCData(oldp+10,((0xfU & (vlSelf->cpudbgdata 
                                     >> 0xcU))),4);
    bufp->chgCData(oldp+11,((0xfU & (vlSelf->cpudbgdata 
                                     >> 0x10U))),4);
    bufp->chgCData(oldp+12,((0xfU & (vlSelf->cpudbgdata 
                                     >> 0x14U))),4);
}

void Vtop___024root__trace_cleanup(void* voidSelf, VerilatedVcd* /*unused*/) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_cleanup\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VlUnpacked<CData/*0:0*/, 1> __Vm_traceActivity;
    // Body
    vlSymsp->__Vm_activity = false;
    __Vm_traceActivity[0U] = 0U;
}
