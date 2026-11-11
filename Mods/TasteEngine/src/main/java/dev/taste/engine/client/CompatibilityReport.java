package dev.taste.engine.client;

import dev.taste.engine.render.CapabilityProbe;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.ModList;

public final class CompatibilityReport {
    public enum Severity { INFO, WARNING, BLOCKING }
    public record Finding(Severity severity,String component,String message) { }
    public List<Finding> collect(CapabilityProbe.Result capability) {
        List<Finding> findings=new ArrayList<>();
        if(capability==null)findings.add(new Finding(Severity.INFO,"OpenGL","Capability probe waits for the first world frame"));
        else if(!capability.baseline())findings.add(new Finding(Severity.BLOCKING,"OpenGL",capability.reason()));
        if(ModList.get().isLoaded("luxium"))findings.add(new Finding(Severity.BLOCKING,"Luxium","Disable Luxium while Taste owns graphics"));
        if(ModList.get().isLoaded("jem_luxium"))findings.add(new Finding(Severity.BLOCKING,"JEM Luxium","Disable JEM Luxium while Taste owns graphics"));
        if(ModList.get().isLoaded("oculus"))findings.add(new Finding(Severity.BLOCKING,"Oculus","Disable Oculus while Taste owns graphics"));
        if(ModList.get().isLoaded("immersivevfx"))findings.add(new Finding(Severity.BLOCKING,"ImmersiveVFX","Disable ImmersiveVFX while Taste owns graphics"));
        if(ClientState.embeddiumInstalled())findings.add(new Finding(Severity.INFO,"Embeddium","Taste accepts only the exact 0.3.31 adapter contract"));
        return List.copyOf(findings);
    }
}
