package com.justenoughmods.achievementguide.integration.petutilities;
import com.justenoughmods.achievementguide.mixin.PetUtilityQuestMerchantAccess;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.module.RibbitProfessionModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
public final class PetUtilityQuestRibbitTrade {
 public static final class Run {public final ServerPlayer actor;public final MerchantMenu menu;public final Merchant merchant;public final MerchantOffer offer;public final ItemStack result;public final int held;public int paid;public Run(ServerPlayer a,MerchantMenu m,Merchant t,MerchantOffer o){actor=a;menu=m;merchant=t;offer=o;result=o.getResult().copy();held=held(a,m,result);}}
 public record Debit(Run run,ItemStack a,ItemStack b,int uses){}
 private static final ThreadLocal<Run> RUN=new ThreadLocal<>();
 private PetUtilityQuestRibbitTrade(){}
 public static int held(ServerPlayer p,AbstractContainerMenu m,ItemStack s){return PetUtilityQuestProof.count(p.getInventory(),s)+(ItemStack.isSameItemSameTags(m.getCarried(),s)?m.getCarried().getCount():0);}
 public static void begin(AbstractContainerMenu menu,Player p){RUN.remove();var actor=PetUtilityQuestProof.actor(p);if(actor==null || p.containerMenu!=menu || !menu.stillValid(p) || !(menu instanceof MerchantMenu merchantMenu))return;var access=(PetUtilityQuestMerchantAccess)menu;var merchant=access.jem$petMerchant();var offer=access.jem$petTrades().getActiveOffer();if(!(merchant instanceof RibbitEntity ribbit) || !ribbit.isAlive() || merchant.getTradingPlayer()!=actor || offer==null || offer.getResult().isEmpty())return;var profession=ribbit.getRibbitData().getProfession();if(profession==RibbitProfessionModule.MERCHANT || profession==RibbitProfessionModule.FISHERMAN)RUN.set(new Run(actor,merchantMenu,merchant,offer));}
 public static Debit debit(Merchant merchant,MerchantContainer input){var r=RUN.get();return r!=null && r.merchant==merchant && r.offer==input.getActiveOffer()?new Debit(r,input.getItem(0).copy(),input.getItem(1).copy(),r.offer.getUses()):null;}
 public static void taken(Debit d,MerchantContainer input,ItemStack output){if(d==null)return;var r=d.run();if(r!=RUN.get() || r.offer.getUses()!=d.uses()+1)return;var ca=r.offer.getCostA();var cb=r.offer.getCostB();boolean normal=r.offer.satisfiedBy(d.a(),d.b()) && d.a().getCount()-input.getItem(0).getCount()==ca.getCount() && d.b().getCount()-input.getItem(1).getCount()==cb.getCount();boolean reversed=r.offer.satisfiedBy(d.b(),d.a()) && d.b().getCount()-input.getItem(1).getCount()==ca.getCount() && d.a().getCount()-input.getItem(0).getCount()==cb.getCount();if((normal || reversed) && ca.getCount()+cb.getCount()>0)r.paid+=r.result.getCount();}
 public static void finish(AbstractContainerMenu menu,Player p){var r=RUN.get();RUN.remove();if(r!=null && r.menu==menu && r.actor==p && PetUtilityQuestProof.actor(p)==p && menu.stillValid(p) && r.merchant.getTradingPlayer()==p && r.paid>0 && held(r.actor,menu,r.result)-r.held==r.paid)PetUtilityQuestProof.emit(r.actor,"v3_pet_swamp_market");}
}
