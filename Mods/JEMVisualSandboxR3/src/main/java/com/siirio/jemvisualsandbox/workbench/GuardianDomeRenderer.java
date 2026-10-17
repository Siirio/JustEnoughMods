package com.siirio.jemvisualsandbox.workbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** One connected native Blockbench shell, shared positions and smooth normals. */
final class GuardianDomeRenderer {
    private static final ResourceLocation MODEL=new ResourceLocation("jem_visual_sandbox","guardian_dome/model.json");
    private static final ResourceLocation TEXTURE=new ResourceLocation("jem_visual_sandbox","guardian_dome/gold-wave-one-shot.png");
    private static final int TEXTURE_FRAMES=36,TEXTURE_FPS=24,FRAME_HEIGHT=64;
    private record Face(int layer,int[] indices,float[][] uv,float[][] normals) {}
    private record Dome(float[][] vertices,Face[] faces) {}
    private static Dome dome;
    private static boolean failed;
    private GuardianDomeRenderer() {}
    static void clear(){dome=null;failed=false;}

    private static float[] vector(JsonArray input,int length) throws IOException {
        if(input.size()!=length)throw new IOException("Invalid guardian vector");
        float[] result=new float[length];
        for(int i=0;i<length;i++){
            result[i]=input.get(i).getAsFloat();
            if(!Float.isFinite(result[i]))throw new IOException("Non-finite guardian vertex/normal");
        }
        return result;
    }
    private static Dome load() throws IOException {
        var manager=Minecraft.getInstance().getResourceManager();
        manager.getResourceOrThrow(TEXTURE);
        try(var reader=new InputStreamReader(manager.getResourceOrThrow(MODEL).open(),StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            var points=root.getAsJsonArray("vertices");var surfaces=root.getAsJsonArray("faces");
            if(root.get("format").getAsInt()!=1 || points.isEmpty() || points.size()>512 || surfaces.isEmpty() || surfaces.size()>512)
                throw new IOException("Invalid connected guardian model dimensions");
            float[][] vertices=new float[points.size()][];
            for(int i=0;i<vertices.length;i++)vertices[i]=vector(points.get(i).getAsJsonArray(),3);
            Face[] faces=new Face[surfaces.size()];
            for(int i=0;i<faces.length;i++){
                var f=surfaces.get(i).getAsJsonObject();var ids=f.getAsJsonArray("indices");
                if(ids.size()!=3 && ids.size()!=4)throw new IOException("Guardian face must be triangle or quad");
                var uv=f.getAsJsonArray("uv");var normals=f.getAsJsonArray("normals");
                if(uv.size()!=ids.size() || normals.size()!=ids.size())throw new IOException("Guardian face attributes mismatch");
                int layer=switch(f.get("layer").getAsString()){case "outer"->0;case "inner"->1;case "rim"->2;default->throw new IOException("Unknown guardian surface");};
                int[] indices=new int[ids.size()];float[][] texels=new float[ids.size()][],directions=new float[ids.size()][];
                for(int j=0;j<indices.length;j++){
                    indices[j]=ids.get(j).getAsInt();
                    if(indices[j]<0 || indices[j]>=vertices.length)throw new IOException("Guardian index outside shared vertex table");
                    texels[j]=vector(uv.get(j).getAsJsonArray(),2);directions[j]=vector(normals.get(j).getAsJsonArray(),3);
                }
                faces[i]=new Face(layer,indices,texels,directions);
            }
            return new Dome(vertices,faces);
        }
    }
    static void render(RenderLevelStageEvent event,Vec3 origin,float age){ render(VisualRenderContext.world(event),origin,age); }
    static void render(VisualRenderContext event,Vec3 origin,float age){
        if(age<0 || age>=30 || failed)return;
        if(dome==null)try{dome=load();}catch(IOException | RuntimeException ex){
            failed=true;com.mojang.logging.LogUtils.getLogger().error("Guardian dome requires the connected native model",ex);return;
        }
        float alpha=Math.min(1,age/4)*Math.min(1,(30-age)/8);
        // Incoming age includes the partial tick. Each activation owns its
        // one-shot clock; hold the final cel instead of looping a texture ticker.
        int frame=Math.min((int)Math.floor(age/20F*TEXTURE_FPS),TEXTURE_FRAMES-1);
        float scale=age<4?.92F+.08F*age/4:age<22?1:1+.06F*(age-22)/8;
        Vec3 camera=event.getCamera().getPosition();
        boolean inside=camera.y>=origin.y+.1 && camera.distanceToSqr(origin.add(0,.1,0))<2.2*2.2*scale*scale;
        var poses=event.getPoseStack();poses.pushPose();
        poses.translate(origin.x-camera.x,origin.y+.1-camera.y,origin.z-camera.z);
        poses.scale(scale,scale,scale);poses.translate(0,-.1,0);
        var pose=poses.last();var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var type=Material.TYPE;var out=buffers.getBuffer(type);
        for(Face face:dome.faces){
            // Render the appropriate side of the thick shell, avoiding a second
            // superimposed wave pattern shining through from its other surface.
            if(face.layer!=2 && face.layer!=(inside?1:0))continue;
            int[] order=face.indices.length==3?TRIANGLE:QUAD;
            for(int corner:order){
                float[] p=dome.vertices[face.indices[corner]],uv=face.uv[corner],n=face.normals[corner];
                out.vertex(pose.pose(),p[0],p[1],p[2]).color(255,255,255,(int)(255*alpha))
                        .uv(uv[0],(frame+Math.max(.5F/FRAME_HEIGHT,Math.min(1-.5F/FRAME_HEIGHT,uv[1])))/TEXTURE_FRAMES)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
                        .normal(pose.normal(),n[0],n[1],n[2]).endVertex();
            }
        }
        buffers.endBatch(type);poses.popPose();
    }
    private static final int[] TRIANGLE={0,1,2},QUAD={0,1,2,0,2,3};
    private static final class Material extends RenderType {
        private static final RenderType TYPE=create("jem_guardian_connected_dome",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,MEDIUM_BUFFER_SIZE,false,false,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(TEXTURE,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private Material(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorted,Runnable setup,Runnable clear){
            super(name,format,mode,size,crumbling,sorted,setup,clear);
        }
    }
}
