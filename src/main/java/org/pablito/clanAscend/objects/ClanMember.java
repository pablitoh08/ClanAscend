package org.pablito.clanAscend.objects;

import java.sql.Timestamp;

public class ClanMember {

    private final String playerUUID;
    private final String playerName;
    private final String clanId;
    private String role;

    private int kills;
    private int deaths;
    private Timestamp joinedAt;
    private Timestamp lastActive;

    public ClanMember(String playerUUID, String playerName, String clanId, String role) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.clanId = clanId;
        this.role = role;
        this.kills = 0;
        this.deaths = 0;
        this.joinedAt = new Timestamp(System.currentTimeMillis());
        this.lastActive = new Timestamp(System.currentTimeMillis());
    }

    @SuppressWarnings("unused")
    public String getPlayerUUID() { return playerUUID; }

    @SuppressWarnings("unused")
    public String getPlayerName() { return playerName; }

    @SuppressWarnings("unused")
    public String getClanId() { return clanId; }

    public String getRole() { return role; }

    @SuppressWarnings("unused")
    public int getKills() { return kills; }

    @SuppressWarnings("unused")
    public int getDeaths() { return deaths; }

    @SuppressWarnings("unused")
    public Timestamp getJoinedAt() { return joinedAt; }

    @SuppressWarnings("unused")
    public Timestamp getLastActive() { return lastActive; }

    public void setRole(String role) { this.role = role; }
    public void setKills(int kills) { this.kills = kills; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    @SuppressWarnings("unused")
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    @SuppressWarnings("unused")
    public void setLastActive(Timestamp lastActive) { this.lastActive = lastActive; }

    @SuppressWarnings("unused")
    public double getKDR() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public boolean isLeader() {
        return role.equalsIgnoreCase("LEADER");
    }

    public boolean isOfficer() {
        return role.equalsIgnoreCase("OFFICER");
    }
}