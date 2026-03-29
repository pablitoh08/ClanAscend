package org.pablito.clanAscend.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClanPowerAddEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String clanId;
    private final String clanName;
    private final int amount;
    private final String reason;

    public ClanPowerAddEvent(String clanId, String clanName, int amount, String reason) {
        this.clanId = clanId;
        this.clanName = clanName;
        this.amount = amount;
        this.reason = reason;
    }

    public String getClanId() {
        return clanId;
    }

    public String getClanName() {
        return clanName;
    }

    public int getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}