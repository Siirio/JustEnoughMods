package com.justenoughmods.achievementguide.integration.wildlife;
import java.util.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
public interface WildlifeQuestDeathProof { void jem$wildlifeApproved(LivingDeathEvent event, Set<UUID> participants); }
