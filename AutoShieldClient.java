package com.autoshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoShieldClient implements ClientModInitializer {
    private static boolean enabled = true;
    private static KeyBinding toggleKey;

    private int previousSlot = -1;
    private int holdTicks = 0;
    private int returnDelayTicks = 0;
    private float lastHealth = -1.0f;
    private boolean blockingActive = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auto-shield.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.auto-shield"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            resetState();
            return;
        }

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            client.player.sendMessage(
                    Text.literal("§6[Auto Shield] §fStatus: " + (enabled ? "§aON" : "§cOFF")),
                    true
            );
            if (!enabled) {
                restoreWeapon(client);
                resetState();
            }
        }

        float currentHealth = client.player.getHealth();
        boolean tookDamage = lastHealth > 0.0f && currentHealth < lastHealth;
        lastHealth = currentHealth;

        if (!enabled) {
            return;
        }

        if (tookDamage && !blockingActive) {
            int shieldSlot = findShieldInHotbar(client);
            if (shieldSlot != -1) {
                previousSlot = client.player.getInventory().selectedSlot;
                client.player.getInventory().selectedSlot = shieldSlot;
                client.options.useKey.setPressed(true);
                blockingActive = true;
                holdTicks = 4;
                returnDelayTicks = 1;
            }
        }

        if (blockingActive) {
            if (holdTicks > 0) {
                holdTicks--;
                client.options.useKey.setPressed(true);
            } else {
                client.options.useKey.setPressed(false);
                if (returnDelayTicks > 0) {
                    returnDelayTicks--;
                } else {
                    restoreWeapon(client);
                    resetState();
                }
            }
        }
    }

    private int findShieldInHotbar(MinecraftClient client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.SHIELD)) {
                return i;
            }
        }
        return -1;
    }

    private void restoreWeapon(MinecraftClient client) {
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            client.player.getInventory().selectedSlot = previousSlot;
        }
    }

    private void resetState() {
        previousSlot = -1;
        holdTicks = 0;
        returnDelayTicks = 0;
        blockingActive = false;
    }
}
