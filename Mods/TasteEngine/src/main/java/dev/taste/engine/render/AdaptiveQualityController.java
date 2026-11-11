package dev.taste.engine.render;

public final class AdaptiveQualityController {
    private float scale=1.0F;
    private float stableSeconds;
    public float update(double gpuMilliseconds,float targetMilliseconds,float deltaSeconds,float minimumScale,float recoverySeconds,boolean enabled){
        if(!enabled)return scale=1.0F;
        if(gpuMilliseconds>targetMilliseconds*1.08){scale=Math.max(minimumScale,scale-0.025F);stableSeconds=0;}
        else if(gpuMilliseconds<targetMilliseconds*0.88){stableSeconds+=deltaSeconds;if(stableSeconds>=recoverySeconds){scale=Math.min(1.0F,scale+0.0125F);stableSeconds=0;}}
        else stableSeconds=0;
        return scale;
    }
    public float scale(){return scale;}
}
