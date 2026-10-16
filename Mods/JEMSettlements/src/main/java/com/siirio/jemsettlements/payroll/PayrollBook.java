package com.siirio.jemsettlements.payroll;

import java.util.List;

/** Existing town and personal employers share one scheduling/settlement contract. */
public interface PayrollBook {
    long nextPayrollIndex();
    List<PayrollPeriod> payroll();
    void schedule(PayrollPeriod period);
    void payrollMissed();
    void payrollRecovered();
}
