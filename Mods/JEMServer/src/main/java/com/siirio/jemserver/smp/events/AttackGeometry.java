package com.siirio.jemserver.smp.events;

import net.minecraft.util.Mth;

public record AttackGeometry(Type type,int minX,int maxX,int minZ,int maxZ,double originX,double originZ,double secondX,double secondZ,
                             double targetX,double targetZ,double directionX,double directionZ,double radius,double width,double length,
                             double startAngle,double endAngle,long startTick,long windupDuration,long activeDuration,double progress,int variant,boolean reverse) {
    public enum Type {
        TWIN_CATACLYSM,CROSS_BLAST,COLLISION_COURSE,EARTH_SPLIT,CRIMSON_DIVIDE,IMPALING_FORMATION,ARROW_HEAVEN,TRINITY_ASSAULT,
        DISASSEMBLE,MAGNETIC_GRID,BLINKING_GRID,CROSSING_SHADOWS,SEISMIC_LINES,WORLD_EATER,SCULK_PULSE,TENTACLE_CORRIDOR,
        SCULK_BURIAL,ANCIENT_ROAR,RADIATION_SWEEP,MELTDOWN_GRID,FINAL_COUNTDOWN,CRUSHER,FALLING_FORGE,MAGNETIC_SAW,THORN_ROWS,BLOOM,
        CLOSING_GARDEN,VOID_LINES,END_COLLAPSE,VOID_CROSS,BONE_SPEAR_CHOIR,GRAVEFALL,LAST_MASS,
        CONSTRICTING_HALO,TRIPLE_RIFT,HUNTER_LATTICE
    }

    public AttackGeometry {
        double magnitude=Math.hypot(directionX,directionZ);
        if(magnitude>.0001) {
            directionX/=magnitude;
            directionZ/=magnitude;
        }
    }

    public boolean contains(double x,double z) {
        double cx=(minX+maxX+1)/2.0;
        double cz=(minZ+maxZ+1)/2.0;
        double dx=x-cx,dz=z-cz;
        double radius=Math.min(maxX-minX,maxZ-minZ)/2.0;
        double distance=Math.hypot(dx,dz);
        int checker=Math.floorMod((int)Math.floor(dx/5)+(int)Math.floor(dz/5),2);
        return switch(type) {
            case TWIN_CATACLYSM -> circle(x,z,originX,originZ,radius*.88)||circle(x,z,secondX,secondZ,radius*.88);
            case CROSS_BLAST -> twin(dx,dz,distance,radius);
            case COLLISION_COURSE -> Math.abs(dz)>2&&Math.abs(dz)<radius*.86;
            case EARTH_SPLIT,SEISMIC_LINES,SCULK_PULSE,THORN_ROWS,VOID_LINES,BONE_SPEAR_CHOIR -> denseRows(dz,radius);
            case CRIMSON_DIVIDE -> line(x,z,originX,originZ,targetX,targetZ,radius*.62);
            case IMPALING_FORMATION -> denseRows(dz+5,radius);
            case ARROW_HEAVEN,MAGNETIC_GRID,MELTDOWN_GRID,BLOOM -> lattice(dx,dz,variant,5);
            case TRINITY_ASSAULT -> (Math.abs(dx)<4||Math.abs(dz)<4||checker==0)&&Math.abs(dx-dz)>3;
            case DISASSEMBLE -> distance>4&&distance<radius*.78;
            case BLINKING_GRID -> Math.abs(dx)<4||Math.abs(dz)<4||Math.abs(Math.abs(dx)-Math.abs(dz))<3||lattice(dx,dz,variant,5);
            case CROSSING_SHADOWS -> Math.abs(Math.abs(dx)-Math.abs(dz))<4||denseRows(dz,radius);
            case WORLD_EATER -> distance<radius*.68||Math.abs(distance-radius*.88)<3;
            case TENTACLE_CORRIDOR -> line(x,z,originX,originZ,targetX,targetZ,4.5);
            case SCULK_BURIAL -> circle(x,z,targetX,targetZ,8);
            case ANCIENT_ROAR -> variant%2==0?distance<radius*.62||lattice(dx,dz,variant,5):distance>radius*.38||lattice(dx,dz,variant,5);
            case RADIATION_SWEEP -> angleDistance(Math.atan2(dz,dx),0)<Math.PI*.62;
            case FINAL_COUNTDOWN -> distance>Math.max(4,radius*.25);
            case CRUSHER -> Math.abs(dx)>Math.max(3,radius*.12)&&Math.floorMod((int)Math.floor(dz/6),3)!=0;
            case FALLING_FORGE,GRAVEFALL -> lattice(dx,dz,variant,5);
            case MAGNETIC_SAW -> Math.abs(distance-radius*.55)<5||Math.abs(dx)<3||Math.abs(dz)<3;
            case CLOSING_GARDEN -> Math.floorMod((int)Math.floor(dx/6),3)!=0||Math.floorMod((int)Math.floor(dz/6),3)!=1;
            case END_COLLAPSE -> quadrant(dx,dz)!=Math.floorMod(variant,4)||lattice(dx,dz,variant,4);
            case VOID_CROSS -> Math.abs(Math.abs(dx)-Math.abs(dz))<5||lattice(dx,dz,variant,5);
            case LAST_MASS -> (Math.floorMod((int)Math.floor(dz/7),2)==0||checker==0)&&Math.abs(dx)<radius*.8;
            case CONSTRICTING_HALO -> distance<radius*.38||distance>radius*.58;
            case TRIPLE_RIFT -> parallelRifts(dx,dz,directionX,directionZ,radius);
            case HUNTER_LATTICE -> lattice(dx,dz,variant,4);
        };
    }

    public boolean front(double x,double z) {
        double radius=Math.min(maxX-minX,maxZ-minZ)/2.0;
        return switch(type) {
            case TWIN_CATACLYSM -> ring(x,z,originX,originZ,radius*.78*progress,1)||ring(x,z,secondX,secondZ,radius*.78*progress,1);
            case TENTACLE_CORRIDOR -> lineProgress(x,z,originX,originZ,targetX,targetZ,4.5,progress,.08);
            case SCULK_BURIAL -> ring(x,z,targetX,targetZ,8*progress,.8);
            default -> contains(x,z);
        };
    }

    public AttackGeometry atProgress(double value) {
        return new AttackGeometry(type,minX,maxX,minZ,maxZ,originX,originZ,secondX,secondZ,targetX,targetZ,directionX,directionZ,
                radius,width,length,startAngle,endAngle,startTick,windupDuration,activeDuration,Mth.clamp(value,0,1),variant,reverse);
    }

    public double activationProgress(double x,double z) {
        double arenaRadius=Math.max(1,Math.min(maxX-minX,maxZ-minZ)/2.0);
        return Mth.clamp(switch(type) {
            case TWIN_CATACLYSM -> Math.min(Math.hypot(x-originX,z-originZ),Math.hypot(x-secondX,z-secondZ))/arenaRadius;
            case CRIMSON_DIVIDE,TENTACLE_CORRIDOR,IMPALING_FORMATION,EARTH_SPLIT,SEISMIC_LINES,THORN_ROWS,VOID_LINES,BONE_SPEAR_CHOIR ->
                    projection(x,z,originX,originZ,targetX,targetZ);
            case TRIPLE_RIFT -> projection(x,z,originX,originZ,targetX,targetZ);
            case SCULK_BURIAL -> Math.hypot(x-targetX,z-targetZ)/Math.max(1,width);
            default -> {
                double centerX=(minX+maxX+1)/2.0;
                double centerZ=(minZ+maxZ+1)/2.0;
                yield Math.hypot(x-centerX,z-centerZ)/arenaRadius;
            }
        },0,1);
    }

    private boolean twin(double dx,double dz,double distance,double radius) {
        return switch(Math.floorMod(variant,8)) {
            case 0 -> Math.abs(dx)<5||Math.abs(dz)<5||lattice(dx,dz,variant,5);
            case 1 -> Math.abs(Math.abs(dx)-Math.abs(dz))<4||lattice(dx,dz,variant,5);
            case 2 -> Math.abs(distance-radius*.65)<5||distance<radius*.3;
            case 3 -> distance<radius*.62||lattice(dx,dz,variant,5);
            case 4 -> dx>-radius*.3;
            case 5 -> dz<radius*.3;
            case 6 -> Math.abs(dx)<7||Math.abs(dz)<7||Math.abs(Math.abs(dx)-Math.abs(dz))<4;
            default -> distance<radius*.88;
        };
    }

    private static boolean denseRows(double distance,double radius) {
        return Math.floorMod((int)Math.floor((distance+radius)/5),3)!=0;
    }

    private static boolean lattice(double dx,double dz,int variant,int cellSize) {
        int x=Math.floorDiv((int)Math.floor(dx)+variant,cellSize);
        int z=Math.floorDiv((int)Math.floor(dz)-variant,cellSize);
        return Math.floorMod(x+z,3)!=0;
    }

    private static boolean parallelRifts(double dx,double dz,double directionX,double directionZ,double radius) {
        double transverse=-dx*directionZ+dz*directionX;
        double width=radius*.22;
        return Math.abs(transverse)<width||Math.abs(transverse-radius*.55)<width||Math.abs(transverse+radius*.55)<width;
    }

    private static boolean line(double x,double z,double fromX,double fromZ,double toX,double toZ,double width) {
        double vx=toX-fromX,vz=toZ-fromZ,lengthSquared=vx*vx+vz*vz;
        if(lengthSquared<.01) return circle(x,z,fromX,fromZ,width);
        double projection=((x-fromX)*vx+(z-fromZ)*vz)/lengthSquared;
        if(projection<0||projection>1) return false;
        double px=fromX+vx*projection,pz=fromZ+vz*projection;
        return Math.hypot(x-px,z-pz)<=width;
    }

    private static double projection(double x,double z,double fromX,double fromZ,double toX,double toZ) {
        double vx=toX-fromX,vz=toZ-fromZ,lengthSquared=vx*vx+vz*vz;
        if(lengthSquared<.01) return 0;
        return ((x-fromX)*vx+(z-fromZ)*vz)/lengthSquared;
    }

    private static boolean lineProgress(double x,double z,double fromX,double fromZ,double toX,double toZ,double width,double progress,double band) {
        double vx=toX-fromX,vz=toZ-fromZ,lengthSquared=vx*vx+vz*vz;
        if(lengthSquared<.01) return false;
        double projection=((x-fromX)*vx+(z-fromZ)*vz)/lengthSquared;
        if(Math.abs(projection-progress)>band) return false;
        double px=fromX+vx*projection,pz=fromZ+vz*projection;
        return Math.hypot(x-px,z-pz)<=width;
    }

    private static boolean circle(double x,double z,double cx,double cz,double radius) {
        return Math.hypot(x-cx,z-cz)<=radius;
    }

    private static boolean ring(double x,double z,double cx,double cz,double radius,double width) {
        return Math.abs(Math.hypot(x-cx,z-cz)-radius)<=width;
    }

    private static int quadrant(double dx,double dz) {
        if(dx>=0&&dz>=0) return 0;
        if(dx<0&&dz>=0) return 1;
        if(dx<0) return 2;
        return 3;
    }

    private static double angleDistance(double first,double second) {
        return Math.abs(Mth.wrapDegrees(Math.toDegrees(first-second)))*Math.PI/180;
    }
}
