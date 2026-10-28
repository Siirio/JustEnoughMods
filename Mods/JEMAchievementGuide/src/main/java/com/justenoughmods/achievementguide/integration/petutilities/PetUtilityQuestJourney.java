package com.justenoughmods.achievementguide.integration.petutilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
/** Consecutive native accepted displacement, discarded on pilot, vehicle, or continuity change. */
public final class PetUtilityQuestJourney {
 public final ServerPlayer actor;public final Vec3 start;public Vec3 last;public final int began;
 public PetUtilityQuestJourney(ServerPlayer p,Entity e,Vec3 from){actor=p;start=from;last=from;began=e.tickCount;}
 public boolean move(ServerPlayer p,Entity e,Vec3 from){if(actor!=p || PetUtilityQuestProof.actor(p)==null || p.getRootVehicle()!=e || e.getControllingPassenger()!=p || e.tickCount<began || e.tickCount-began>24000 || !TravelProof.finite(from) || !TravelProof.finite(e.position()) || last.distanceToSqr(from)>1E-8)return false;last=e.position();double x=last.x-start.x,z=last.z-start.z;return x*x+z*z>=4;}
}
