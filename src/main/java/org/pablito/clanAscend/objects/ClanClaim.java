package org.pablito.clanAscend.objects;

import java.sql.Timestamp;

public class ClanClaim {

    private final String id;
    private final String clanId;
    private final String world;
    private final int chunkX;
    private final int chunkZ;

    private Timestamp claimedAt;

    public ClanClaim(String id, String clanId, String world, int chunkX, int chunkZ) {
        this.id = id;
        this.clanId = clanId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimedAt = new Timestamp(System.currentTimeMillis());
    }

    public String getId() { return id; }
    public String getClanId() { return clanId; }
    public String getWorld() { return world; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public Timestamp getClaimedAt() { return claimedAt; }

    public void setClaimedAt(Timestamp claimedAt) { this.claimedAt = claimedAt; }

    public String getChunkKey() {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}