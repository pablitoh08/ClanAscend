package org.pablito.clanAscend.api.model;

public class ClanRewardResult {

    private final boolean success;
    private final String clanId;
    private final String clanName;
    private final String message;

    public ClanRewardResult(boolean success, String clanId, String clanName, String message) {
        this.success = success;
        this.clanId = clanId;
        this.clanName = clanName;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getClanId() {
        return clanId;
    }

    public String getClanName() {
        return clanName;
    }

    public String getMessage() {
        return message;
    }

    public static ClanRewardResult success(String clanId, String clanName, String message) {
        return new ClanRewardResult(true, clanId, clanName, message);
    }

    public static ClanRewardResult failure(String message) {
        return new ClanRewardResult(false, null, null, message);
    }
}