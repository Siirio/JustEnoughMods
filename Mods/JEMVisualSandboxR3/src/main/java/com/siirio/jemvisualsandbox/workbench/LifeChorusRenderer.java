package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Golden healing front, larger outward R7 shields and a continuous guardian sphere. */
final class LifeChorusRenderer {
    private record Scene(ClientLevel level,long start,int kind,int subject,Vec3 origin) {}
    private static final List<Scene> ACTIVE=new ArrayList<>();
    private static WorkbenchMesh shield;
    private static float centerX,centerY,centerZ,shieldScale;
    private static boolean shieldFailed;
    static void clear(){ACTIVE.clear();shield=null;shieldFailed=false;}
    static void receive(SandboxNetwork.Visual p){
        var c=Minecraft.getInstance();if(c.level==null)return;
        if(ACTIVE.size()>=96)ACTIVE.remove(0);
        ACTIVE.add(new Scene(c.level,c.level.getGameTime()+(p.action()==31?(int)p.yaw():0),p.action(),p.subject(),p.origin()));
    }
    static void render(RenderLevelStageEvent event){
        var c=Minecraft.getInstance();if(c.level==null || !c.hasSingleplayerServer()){clear();return;}
        ACTIVE.removeIf(s->s.level!=c.level || c.level.getGameTime()-s.start>=(s.kind==30?130:32));
        for(Scene s:ACTIVE){
            float age=c.level.getGameTime()-s.start+event.getPartialTick();if(age<0)continue;
            var target=c.level.getEntity(s.subject);
            if(target==null || !target.isAlive())continue;
            Vec3 pos=target.getPosition(event.getPartialTick());
            if(s.kind==30){
                if(age<14)goldSprite(event,pos.add(0,1.1,0),"gold_swirl",Math.min(15,(int)(age*16/14)),2.5F,1);
                if(age>=7 && age<19){
                    groundArt(event,s.origin,(float)(16*(age-6)/12),"gold_front",Math.min(10,4+(int)(age-7)/2),1);
                }
                if(age>=19 && age<31){
                    groundArt(event,s.origin,16,"gold_front",Math.min(15,10+(int)(age-19)/2),(31-age)/12);
                }
                if(age>=12 && age<27)groundArt(event,s.origin,(float)(16*(age-11)/15),"gold_front",10,.2F*(27-age)/15);
                if(age>=18 && age<118){
                    float alpha=Math.min(1,(age-18)/4)*Math.min(1,(118-age)/10);
                    for(int i=0;i<4;i++){
                        double a=Math.toRadians(i*90+(age-18)*1.8);
                        Vec3 place=pos.add(Math.cos(a)*1.45,1.05+Math.sin(a*2)*.05,Math.sin(a)*1.45);
                        // R7 front points inward at the former radial yaw.
                        // User correction: present the painted face to threats.
                        miniShield(event,place,(float)(-Math.toDegrees(a)+270),alpha);
                    }
                }
                if(age>=118)for(int i=0;i<4;i++){
                    double a=Math.toRadians(i*90+180);double drift=(age-118)*.025;
                    goldSprite(event,pos.add(Math.cos(a)*(1.45+drift),1.05+drift,Math.sin(a)*(1.45+drift)),"gold_motes",Math.min(15,11+(int)(age-118)/3),.85F,(130-age)/12);
                }
            }else if(s.kind==35){
                if(age<30)guardianShell(event,pos,age);
                if(age<LifeChorusAbility.WAVE_TICKS)groundArt(event,s.origin,(float)(LifeChorusAbility.WAVE_RADIUS*(age+1)/LifeChorusAbility.WAVE_TICKS),"gold_front",Math.min(10,4+(int)age/2),1);
                else if(age<20)groundArt(event,s.origin,(float)LifeChorusAbility.WAVE_RADIUS,"gold_front",Math.min(15,11+(int)(age-12)/2),(20-age)/8);
            }else if(s.kind==31 || s.kind==27){
                
                if(age<24){
                    float height=(float)target.getBbHeight(),size=Mth.clamp(height*.85F,.8F,2.6F);
                    goldSprite(event,pos.add(0,height*.58,0),"gold_motes",Math.min(15,(int)(age*16/24)),size,Math.min(1,(24-age)/5));
                }
            }else if(s.kind==32){
                if(age>=7 && age<19)band(event,s.origin,8*(age-6)/12,"sonic_band",(int)age-7,1);
                if(age>=10 && age<26)band(event,s.origin,8*(age-9)/16,"sonic_band",(int)age-4,.45F*(26-age)/16);
            }
        }
    }
    private static void goldSprite(RenderLevelStageEvent e,Vec3 origin,String file,int frame,float size,float alpha){
        if(frame<0 || frame>=16 || alpha<=0)return;
        var c=Minecraft.getInstance();var poses=e.getPoseStack();poses.pushPose();Vec3 cam=e.getCamera().getPosition();
        poses.translate(origin.x-cam.x,origin.y-cam.y,origin.z-cam.z);poses.mulPose(c.getEntityRenderDispatcher().cameraOrientation());
        var type=RenderType.entityTranslucent(new ResourceLocation("jem_visual_sandbox","life_chorus/"+file+".png"));var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);var p=poses.last();
        // Native384x256 cels, no resampling or disguised pixelation. Preserve
        // their3:2 aspect; the middle remains transparent around the subject.
        for(float[] v:new float[][]{{-.75F,-.5F,0,1},{.75F,-.5F,1,1},{.75F,.5F,1,0},{-.75F,.5F,0,0}})out.vertex(p.pose(),v[0]*size,v[1]*size,0).color(255,255,255,(int)(255*alpha)).uv(v[2],(frame+v[3])/16F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
        buffers.endBatch(type);poses.popPose();
    }
    private static void guardianShell(RenderLevelStageEvent e,Vec3 origin,float age){
        var c=Minecraft.getInstance();var p=e.getPoseStack().last();Vec3 cam=e.getCamera().getPosition();
        var type=RenderType.entityTranslucent(new ResourceLocation("jem_visual_sandbox","ancients_pearl/guardian_sphere.png"));
        var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        float envelope=Math.min(1,age/3)*Math.min(1,(30-age)/10);
        Vec3 center=origin.add(0,1.35,0);
        int frame=(int)(age/2)%8;
        // User rejected separated curling tails: the entire sphere is filled.
        // A soft body and brighter silhouette keep it visibly solid without
        // turning the local first-person camera into an opaque yellow screen.
        double radius=1.45*(.88+.12*Math.min(1,age/4));
        boolean inside=cam.distanceToSqr(center)<radius*radius;
        for(int y=0;y<16;y++)for(int x=0;x<32;x++){
            double[][] corners={{x/32D,y/16D},{(x+1)/32D,y/16D},{(x+1)/32D,(y+1)/16D},{x/32D,(y+1)/16D}};
            for(double[] q:corners){
                double longitude=q[0]*Math.PI*2+age*.018,latitude=(q[1]-.5)*Math.PI;
                double nx=Math.cos(longitude)*Math.cos(latitude),ny=Math.sin(latitude),nz=Math.sin(longitude)*Math.cos(latitude);
                Vec3 normal=new Vec3(nx,ny,nz),point=center.add(normal.scale(radius));
                double edge=1-Math.abs(normal.dot(cam.subtract(point).normalize()));
                int alpha=(int)(255*envelope*(inside?.07:.12+.58*edge*edge));
                out.vertex(p.pose(),(float)(point.x-cam.x),(float)(point.y-cam.y),(float)(point.z-cam.z)).color(255,255,255,alpha).uv((float)q[0],(float)((frame+1-q[1])/8)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),(float)nx,(float)ny,(float)nz).endVertex();
            }
        }
        buffers.endBatch(type);
    }
    private static void groundArt(RenderLevelStageEvent e,Vec3 origin,float radius,String file,int frame,float alpha){
        var c=Minecraft.getInstance();Vec3 cam=e.getCamera().getPosition();var p=e.getPoseStack().last();
        var type=RenderType.entityTranslucent(new ResourceLocation("jem_visual_sandbox","life_chorus/"+file+".png"));var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        // Drape the original ornate artwork over exposed collision tops rather
        // than replacing it with a calculated ring. Missing floors stay empty.
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            float u=x/16F,v=z/16F,du=1/16F;
            double y=surface(origin.add((u+du/2-.5)*radius*3.2,0,(v+du/2-.5)*radius*2.2));if(!Double.isFinite(y))continue;
            for(float[] q:new float[][]{{u,v},{u+du,v},{u+du,v+du},{u,v+du}})out.vertex(p.pose(),(float)(origin.x+(q[0]-.5)*radius*3.2-cam.x),(float)(y-cam.y),(float)(origin.z+(q[1]-.5)*radius*2.2-cam.z)).color(255,255,255,(int)(255*alpha)).uv(q[0],(frame+q[1])/16F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,1,0).endVertex();
        }buffers.endBatch(type);
    }
    private static double surface(Vec3 point){
        var level=Minecraft.getInstance().level;BlockPos base=BlockPos.containing(point);
        for(int dy=3;dy>=-4;dy--){var at=base.offset(0,dy,0);var shape=level.getBlockState(at).getCollisionShape(level,at);
            if(!shape.isEmpty())return at.getY()+shape.max(net.minecraft.core.Direction.Axis.Y)+.045;
        }return Double.NaN;
    }
    private static void band(RenderLevelStageEvent e,Vec3 origin,double radius,String file,int frame,float alpha){
        if(radius<=0 || alpha<=0)return;radius=Math.min(16,radius);frame=Math.min(21,Math.max(0,frame));
        var c=Minecraft.getInstance();var poses=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        var type=RenderType.entityTranslucent(new ResourceLocation("jem_visual_sandbox","life_chorus/"+file+".png"));var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);var p=poses.last();
        // Material tiles have painted unequal wave lips and leaf clusters. A
        // missing floor leaves a gap rather than burying the effect in a block.
        for(int i=0;i<64;i++){
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32,inner=Math.max(0,radius-1.4),outer=radius+.8;
            Vec3 mid=origin.add(Math.cos((a+b)/2)*radius,0,Math.sin((a+b)/2)*radius);double y=surface(mid);if(!Double.isFinite(y))continue;
            double u0=(i%2)*.5,u1=u0+.5;
            double[][] v={{Math.cos(a)*inner,Math.sin(a)*inner,u0,0},{Math.cos(b)*inner,Math.sin(b)*inner,u1,0},{Math.cos(b)*outer,Math.sin(b)*outer,u1,1},{Math.cos(a)*outer,Math.sin(a)*outer,u0,1}};
            for(double[] q:v)out.vertex(p.pose(),(float)(origin.x+q[0]-cam.x),(float)(y-cam.y),(float)(origin.z+q[1]-cam.z)).color(255,255,255,(int)(alpha*255)).uv((float)q[2],(float)((frame+q[3])/24)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,1,0).endVertex();
        }buffers.endBatch(type);
    }
    private static void miniShield(RenderLevelStageEvent e,Vec3 origin,float yaw,float alpha){
        if(shieldFailed || alpha<=0)return;
        if(shield==null)try{
            shield=WorkbenchMesh.load("shield",1);float[] frame=shield.poses[10];float[] min={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE},max={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
            for(int i=0;i<frame.length;i++){int axis=i%3;min[axis]=Math.min(min[axis],frame[i]);max[axis]=Math.max(max[axis],frame[i]);}
            centerX=(min[0]+max[0])/2;centerY=(min[1]+max[1])/2;centerZ=(min[2]+max[2])/2;shieldScale=.95F/(max[1]-min[1]);
        }catch(java.io.IOException ex){shieldFailed=true;com.mojang.logging.LogUtils.getLogger().error("Life Chorus requires authored R7 shield",ex);return;}
        var c=Minecraft.getInstance();var poses=e.getPoseStack();poses.pushPose();Vec3 cam=e.getCamera().getPosition();poses.translate(origin.x-cam.x,origin.y-cam.y,origin.z-cam.z);poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));poses.scale(shieldScale,shieldScale,shieldScale);poses.translate(-centerX,-centerY,-centerZ);var p=poses.last();float[] frame=shield.poses[10];
        var type=RenderType.entityTranslucent(shield.textures[0]);var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        for(int f=0;f<shield.materials.length;f++){
            int k=f*12;Vec3 a=new Vec3(frame[k+3]-frame[k],frame[k+4]-frame[k+1],frame[k+5]-frame[k+2]),b=new Vec3(frame[k+6]-frame[k],frame[k+7]-frame[k+1],frame[k+8]-frame[k+2]);Vec3 n=a.cross(b).normalize();
            for(int v=0;v<4;v++){int o=k+v*3;out.vertex(p.pose(),frame[o],frame[o+1],frame[o+2]).color(255,255,255,(int)(255*alpha)).uv(shield.uv[f][v*2],shield.uv[f][v*2+1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();}
        }buffers.endBatch(type);poses.popPose();
    }
}
