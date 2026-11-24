package com.autoshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public class AutoShieldClient implements ClientModInitializer {
    private int previousSlot = -1;
    private int activeShieldSlot = -1;
    private int holdTicks = 0;
    private int releaseTicks = 0;
    private int cooldownTicks = 0;
    private float lastHealth = -1.0f;

    private boolean blockingActive = false;
    private boolean releasingActive = false;
    private boolean manualUseHeldBeforeBlock = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null) {
            fullReset(client);
            lastHealth = -1.0f;
            return;
        }

        float currentHealth = client.player.getHealth();
        boolean tookDamage = lastHealth > 0.0f && currentHealth < lastHealth;
        lastHealth = currentHealth;

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        boolean useKeyCurrentlyPressed = client.options.keyUse.isDown();

        if (!blockingActive && !releasingActive) {
            manualUseHeldBeforeBlock = useKeyCurrentlyPressed;
        }

        if (tookDamage && !blockingActive && !releasingActive && !manualUseHeldBeforeBlock && cooldownTicks == 0) {
            int currentSlot = client.player.getInventory().getSelectedSlot();
            int shieldSlot = findShieldInHotbar(client);

            if (shieldSlot != -1 && shieldSlot != currentSlot) {
                previousSlot = currentSlot;
                activeShieldSlot = shieldSlot;
                client.player.getInventory().setSelectedSlot(shieldSlot);
                client.options.keyUse.setDown(true);

                blockingActive = true;
                releasingActive = false;
                holdTicks = 3;
                releaseTicks = 1;
                cooldownTicks = 2;
            }
        }

        if (blockingActive) {
            if (!isHoldingShieldSlot(client)) {
                restoreWeaponNow(client);
                return;
            }

            if (holdTicks > 0) {
                client.options.keyUse.setDown(true);
                holdTicks--;
            } else {
                client.options.keyUse.setDown(false);
                blockingActive = false;
                releasingActive = true;
            }
        } else if (releasingActive) {
            client.options.keyUse.setDown(false);
            if (releaseTicks > 0) {
                releaseTicks--;
            } else {
                restoreWeaponNow(client);
            }
        }
    }

    private boolean isHoldingShieldSlot(Minecraft client) {
        if (client.player == null || activeShieldSlot < 0 || activeShieldSlot >= 9) {
            return false;
        }
        return client.player.getInventory().getSelectedSlot() == activeShieldSlot
                && client.player.getInventory().getItem(activeShieldSlot).is(Items.SHIELD);
    }

    private int findShieldInHotbar(Minecraft client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).is(Items.SHIELD)) {
                return i;
            }
        }
        return -1;
    }

    private void restoreWeaponNow(Minecraft client) {
        if (client != null) {
            if (client.options != null && !manualUseHeldBeforeBlock) {
                client.options.keyUse.setDown(false);
            }
            if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
                if (client.player.getInventory().getSelectedSlot() != previousSlot) {
                    client.player.getInventory().setSelectedSlot(previousSlot);
                }
            }
        }
        previousSlot = -1;
        activeShieldSlot = -1;
        holdTicks = 0;
        releaseTicks = 0;
        blockingActive = false;
        releasingActive = false;
        manualUseHeldBeforeBlock = false;
    }

    private void fullReset(Minecraft client) {
        if (client != null && client.options != null) {
            client.options.keyUse.setDown(false);
        }
        previousSlot = -1;
        activeShieldSlot = -1;
        holdTicks = 0;
        releaseTicks = 0;
        cooldownTicks = 0;
        blockingActive = false;
        releasingActive = false;
        manualUseHeldBeforeBlock = false;
    }
}
