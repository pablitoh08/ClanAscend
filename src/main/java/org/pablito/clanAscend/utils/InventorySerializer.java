package org.pablito.clanAscend.utils;

import org.bukkit.inventory.ItemStack;
import org.pablito.clanAscend.ClanAscend;

import java.io.*;
import java.util.Base64;

public final class InventorySerializer {

    private InventorySerializer() {
        // Utility class
    }

    public static String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null) items = new ItemStack[0];

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                if (item == null) {
                    dataOutput.writeBoolean(false);
                } else {
                    dataOutput.writeBoolean(true);
                    byte[] itemData = item.serializeAsBytes();
                    dataOutput.writeInt(itemData.length);
                    dataOutput.write(itemData);
                }
            }

            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize ItemStack array", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return new ItemStack[0];
        }

        byte[] data = Base64.getDecoder().decode(base64);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {

            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                boolean hasItem = dataInput.readBoolean();
                if (hasItem) {
                    int itemDataLength = dataInput.readInt();
                    byte[] itemData = new byte[itemDataLength];
                    dataInput.readFully(itemData);
                    items[i] = ItemStack.deserializeBytes(itemData);
                } else {
                    items[i] = null;
                }
            }

            return items;

        } catch (IOException e) {
            throw new IllegalStateException("Could not deserialize ItemStack array", e);
        }
    }

    @SuppressWarnings("unused")
    public static String itemStackToBase64(ItemStack item, ClanAscend plugin) {
        if (item == null) return "";

        try {
            byte[] itemData = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(itemData);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize ItemStack", e);
        }
    }

    @SuppressWarnings("unused")
    public static ItemStack itemStackFromBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] itemData = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(itemData);
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize ItemStack", e);
        }
    }

    @SuppressWarnings("unused")
    public static String toBase64(ItemStack[] items) {
        return itemStackArrayToBase64(items);
    }

    @SuppressWarnings("unused")
    public static ItemStack[] fromBase64(String base64) {
        return itemStackArrayFromBase64(base64);
    }
}