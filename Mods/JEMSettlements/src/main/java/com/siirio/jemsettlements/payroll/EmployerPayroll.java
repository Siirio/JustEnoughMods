package com.siirio.jemsettlements.payroll;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Personal payroll is not a settlement and never creates territory, members or a treasury. */
public final class EmployerPayroll implements PayrollBook {
    private final List<PayrollPeriod> periods = new ArrayList<>();
    private long nextIndex;
    public long nextPayrollIndex() { return nextIndex; }
    public List<PayrollPeriod> payroll() { return List.copyOf(periods); }
    public void schedule(PayrollPeriod period) {
        periods.add(period); nextIndex = Math.addExact(period.index(), 1L);
        while (periods.size() > 32 && periods.get(0).paid()) periods.remove(0);
    }
    public void payrollMissed() { }
    public void payrollRecovered() { }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putLong("NextIndex", nextIndex);
        ListTag rows = new ListTag(); periods.forEach(value -> rows.add(value.save())); tag.put("Periods", rows); return tag;
    }
    public static EmployerPayroll load(CompoundTag tag) {
        EmployerPayroll book = new EmployerPayroll(); book.nextIndex = tag.getLong("NextIndex");
        ListTag rows = tag.getList("Periods", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) book.periods.add(PayrollPeriod.load(rows.getCompound(i)));
        return book;
    }
}
