package dev.taste.engine.effects.water;

import dev.taste.engine.material.MediumRegistry;
import dev.taste.engine.render.reflection.SsrTracer;

public record WaterRenderer(SurfaceWaveField waves, RippleSimulation ripples, FoamRenderer foam,
        CausticRenderer caustics, SsrTracer reflections, MediumRegistry.Medium medium) { }
