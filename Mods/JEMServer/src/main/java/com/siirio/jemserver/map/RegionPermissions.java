package com.siirio.jemserver.map;

import java.util.UUID;

public final class RegionPermissions {
    private RegionPermissions() { }
    public static boolean mayEditMetadata(ProjectRegion project, UUID accountId) {
        return project.owner().equals(accountId) || project.collaborators().contains(accountId);
    }
}
