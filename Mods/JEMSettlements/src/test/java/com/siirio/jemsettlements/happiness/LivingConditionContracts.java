package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.ResidentMood;
import java.io.InputStream;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Explicit user-requested regressions; opens no game, world or server. */
public final class LivingConditionContracts {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        boolean expectedStandalone=Boolean.parseBoolean(System.getProperty("jem.test.standalone", "false"));
        check(com.siirio.jemsettlements.SettlementBuild.STANDALONE==expectedStandalone,
                "compiled artifact uses its explicit production/integrated sandbox authority profile");
        for(String locale:new String[]{"ru_ru","en_us"}) {
            try(InputStream in=LivingConditionContracts.class.getResourceAsStream("/assets/jem_settlements/lang/"+locale+".json")) {
                if(in==null)throw new AssertionError("missing locale "+locale);
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                check(json.has("jem.settlements.conditions.crowding") && json.has("jem.settlements.conditions.light")
                        && json.has("jem.settlements.conditions.fear") && json.has("jem.settlements.housing.personal_space_refusal"),
                        "complete condition and native refusal localization ships in "+locale);
            }
        }
        var defaults=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        SettlementMetricsConfig.SPEC.correct(defaults); SettlementMetricsConfig.SPEC.setConfig(defaults);
        ResidentMood mood = new ResidentMood(UUID.randomUUID(), UUID.randomUUID(), null, null);
        int base = mood.personalMood();
        mood.crowdingPenalty(30); mood.lightPenalty(10); mood.fearPenalty(20);
        check(mood.mood() == 0, "all three penalties project through the existing owner");
        for (int i=0;i<100;i++) { mood.crowdingPenalty(30); mood.lightPenalty(10); mood.fearPenalty(20); }
        check(mood.personalMood()==base, "repeated observations do not accumulate permanent damage");
        ResourceLocation bread = new ResourceLocation("minecraft", "bread");
        mood.acceptFood(bread, 2, 1, 100);
        check(mood.personalMood()==base+2 && mood.mood()==0, "food is earned even while visible mood is clamped zero");
        mood.acceptFood(bread, 2, 1, 140);
        check(mood.personalMood()==base+3, "safe repeat earns one");
        ResourceLocation meal = new ResourceLocation("minecraft", "baked_potato");
        mood.requestedDish(meal); mood.acceptFood(meal, 2, 1, 180);
        check(mood.personalMood()==base+9, "requested safe meal preserves base plus extra four");
        mood.crowdingPenalty(0); mood.lightPenalty(0); mood.fearPenalty(0);
        check(mood.mood()==base+9, "repair restores projection with all earned food intact");
        mood.environment(100,100,0); int enriched=mood.mood();
        mood.fearPenalty(20); mood.fearPenalty(0);
        check(mood.mood()==enriched, "condition recovery preserves other system environment bonuses");
        var loaded=ResidentMood.load(mood.save());
        check(loaded.personalMood()==mood.personalMood(), "base survives persistence independently of penalties");
        assertCancellation("mixin/VillagerSleepSpaceMixin", "Villager guard cancels native sleep before LAST_SLEPT");
        assertCancellation("mixin/RecruitSleepSpaceMixin", "inherited native recruit sleep is cancelled");
        assertCancellation("mixin/RecruitBedSpaceGoalMixin", "native recruit goal cannot write sleepingPos after rejection");
        var recruit=read("compat/recruits/RecruitNeedsService");
        var feed=recruit.methods.stream().filter(m->m.name.equals("feedOne")).findFirst().orElseThrow();
        long personal=recruit.methods.stream().filter(m -> m.name.equals("feedOne") || m.name.startsWith("lambda$feedOne$")).flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false))
            .filter(i->i instanceof MethodInsnNode call && call.owner.endsWith("ResidentMood") && call.name.equals("personalMood")).count();
        check(personal>=2, "native recruit food delta reads both unpenalized personal values");
        System.out.println("LivingConditionContracts: PASS ("+assertions+" assertions; state and compiled sleep/food integration contracts)");
    }
    private static ClassNode read(String relative) throws Exception {
        try(InputStream in=LivingConditionContracts.class.getResourceAsStream("/com/siirio/jemsettlements/"+relative+".class")) {
            if(in==null)throw new AssertionError("missing production class "+relative);
            ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
    private static void assertCancellation(String name,String label)throws Exception {
        var node=read(name); boolean bound=false;
        for(var method:node.methods) {
            boolean callsRefusal=false,cancels=false,conditional=false;
            for(var instruction:method.instructions) {
                if(instruction instanceof MethodInsnNode call) {
                    callsRefusal |= call.owner.endsWith("BedPersonalSpace") && call.name.equals("refuse") && call.desc.endsWith(")Z");
                    cancels |= call.owner.endsWith("CallbackInfo") && call.name.equals("cancel");
                }
                conditional |= instruction instanceof JumpInsnNode;
            }
            if(!callsRefusal||!cancels||!conditional)continue;
            if(method.visibleAnnotations!=null)for(var annotation:method.visibleAnnotations)
                if(annotation.desc.endsWith("/Inject;"))bound|=isCancellableHead(annotation);
            if(method.invisibleAnnotations!=null)for(var annotation:method.invisibleAnnotations)
                if(annotation.desc.endsWith("/Inject;"))bound|=isCancellableHead(annotation);
        }
        check(bound,label);
    }
    private static boolean isCancellableHead(AnnotationNode annotation) {
        boolean cancellable=false,head=false;
        for(int i=0;i<annotation.values.size();i+=2) {
            Object key=annotation.values.get(i),value=annotation.values.get(i+1);
            if(key.equals("cancellable"))cancellable=Boolean.TRUE.equals(value);
            if(key.equals("at") && value instanceof java.util.List<?> list)
                for(Object at:list)if(at instanceof AnnotationNode node)head|=node.values.contains("HEAD");
        }
        return cancellable && head;
    }
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
}
