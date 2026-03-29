package org.pablito.clanAscend.objects;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Objects;

public class ChunkLocation {
    private final String worldName;
    private final int x;
    private final int z;

    public ChunkLocation(String worldName, int x, int z) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }

    @SuppressWarnings("unused")
    public ChunkLocation(Chunk chunk) {
        this.worldName = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    public ChunkLocation(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX() >> 4; // Divide por 16
        this.z = location.getBlockZ() >> 4;
    }

    @SuppressWarnings("unused")
    public String getWorldName() { return worldName; }

    @SuppressWarnings("unused")
    public int getX() { return x; }

    @SuppressWarnings("unused")
    public int getZ() { return z; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkLocation that = (ChunkLocation) o;
        return x == that.x && z == that.z && Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, z);
    }

    @Override
    public String toString() {
        return worldName + ":" + x + ":" + z;
    }

    public static ChunkLocation fromString(String str) {
        String[] parts = str.split(":");
        if (parts.length != 3) return null;
        try {
            return new ChunkLocation(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}