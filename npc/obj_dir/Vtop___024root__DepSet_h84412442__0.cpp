// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "verilated.h"

#include "Vtop__Syms.h"
#include "Vtop___024root.h"

#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__act(Vtop___024root* vlSelf);
#endif  // VL_DEBUG

void Vtop___024root___eval_triggers__act(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_triggers__act\n"); );
    // Body
    vlSelf->__VactTriggered.at(0U) = ((IData)(vlSelf->clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__clk)));
    vlSelf->__VactTriggered.at(1U) = ((IData)(vlSelf->top__DOT____Vcellinp__mycounter2__clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter2__clk)));
    vlSelf->__VactTriggered.at(2U) = ((IData)(vlSelf->top__DOT____Vcellinp__mycounter3__clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter3__clk)));
    vlSelf->__VactTriggered.at(3U) = ((IData)(vlSelf->top__DOT____Vcellinp__mycounter4__clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter4__clk)));
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter2__clk 
        = vlSelf->top__DOT____Vcellinp__mycounter2__clk;
    vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter3__clk 
        = vlSelf->top__DOT____Vcellinp__mycounter3__clk;
    vlSelf->__Vtrigrprev__TOP__top__DOT____Vcellinp__mycounter4__clk 
        = vlSelf->top__DOT____Vcellinp__mycounter4__clk;
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        Vtop___024root___dump_triggers__act(vlSelf);
    }
#endif
}
