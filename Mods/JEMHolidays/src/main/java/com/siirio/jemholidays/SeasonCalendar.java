package com.siirio.jemholidays;

import java.time.LocalDate;
import java.time.Month;

final class SeasonCalendar {
    private SeasonCalendar() {
    }

    static boolean isHalloween(LocalDate date) {
        return date.getMonth() == Month.OCTOBER && date.getDayOfMonth() >= 30
                || date.getMonth() == Month.NOVEMBER && date.getDayOfMonth() <= 2;
    }

    static boolean isChristmas(LocalDate date) {
        return date.getMonth() == Month.NOVEMBER && date.getDayOfMonth() >= 28
                || date.getMonth() == Month.DECEMBER
                || date.getMonth() == Month.JANUARY && date.getDayOfMonth() <= 28;
    }
}
