package com.justenoughmods.achievementguide.integration.photo;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.lightroom.PrintingProcess;

public interface PhotoLightroomState {
    int jem$photoPrintTime();
    PrintingProcess jem$photoProcess(Frame frame);
}
