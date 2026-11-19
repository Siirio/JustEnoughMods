package com.siirio.jemsettlements.family;

import java.util.UUID;

final class FamilyNames {
    private static final String[] MALE = {"Adrian","Alex","Anton","Dario","Elias","Emil","Felix","Hugo","Leon","Luca","Marco","Milan","Nico","Noah","Oscar","Pavel","Rafael","Riccardo","Theo","Victor"};
    private static final String[] FEMALE = {"Ada","Alina","Amelia","Clara","Diana","Elena","Eva","Iris","Julia","Lara","Lea","Mara","Mia","Nora","Olivia","Rosa","Selena","Sofia","Tessa","Vera"};
    private FamilyNames() { }
    static String name(UUID id, FamilyRecord.Sex sex) { String[] names = sex == FamilyRecord.Sex.MALE ? MALE : FEMALE; return names[Math.floorMod(id.hashCode(), names.length)]; }
}
