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
            fullReset(client);
            lastHealth = -1.0f;
            return;
        }

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            client.player.sendMessage(
                    Text.literal("§6[Auto Shield] §fStatus: " + (enabled ? "§aON" : "§cOFF")),
                    true
            );
            if (!enabled) {
                restoreWeaponNow(client);
            }
        }

        float currentHealth = client.player.getHealth();
        boolean tookDamage = lastHealth > 0.0f && currentHealth < lastHealth;
        lastHealth = currentHealth;

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (!enabled) {
            if (blockingActive || releasingActive) {
                restoreWeaponNow(client);
            }
            return;
        }

        boolean useKeyCurrentlyPressed = client.options.useKey.isPressed();

        if (!blockingActive && !releasingActive) {
            manualUseHeldBeforeBlock = useKeyCurrentlyPressed;
        }

        if (tookDamage && !blockingActive && !releasingActive && !manualUseHeldBeforeBlock && cooldownTicks == 0) {
            int currentSlot = client.player.getInventory().selectedSlot;
            int shieldSlot = findShieldInHotbar(client);

            if (shieldSlot != -1 && shieldSlot != currentSlot) {
                previousSlot = currentSlot;
                activeShieldSlot = shieldSlot;
                client.player.getInventory().selectedSlot = shieldSlot;
                client.options.useKey.setPressed(true);

                blockingActive = true;
                releasingActive = false;
                holdTicks = 3;      // صد سريع جداً وخاطف للـ 1v1
                releaseTicks = 1;   // تيك واحد فك قبل الرجوع للسلاح
                cooldownTicks = 2;  // كولداون خفيف جداً حتى يستجيب للهجمات المتلاحقة
            }
        }

        if (blockingActive) {
            if (!isHoldingShieldSlot(client)) {
                restoreWeaponNow(client);
                return;
            }

            if (holdTicks > 0) {
                client.options.useKey.setPressed(true);
                holdTicks--;
            } else {
                client.options.useKey.setPressed(false);
                blockingActive = false;
                releasingActive = true;
            }
        } else if (releasingActive) {
            client.options.useKey.setPressed(false);
            if (releaseTicks > 0) {
                releaseTicks--;
            } else {
                restoreWeaponNow(client);
            }
        }
    }

    private boolean isHoldingShieldSlot(MinecraftClient client) {
        if (client.player == null || activeShieldSlot < 0 || activeShieldSlot >= 9) {
            return false;
        }
        return client.player.getInventory().selectedSlot == activeShieldSlot
                && client.player.getInventory().getStack(activeShieldSlot).isOf(Items.SHIELD);
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

    private void restoreWeaponNow(MinecraftClient client) {
        if (client != null) {
            if (client.options != null && !manualUseHeldBeforeBlock) {
                client.options.useKey.setPressed(false);
            }
            if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
                if (client.player.getInventory().selectedSlot != previousSlot) {
                    client.player.getInventory().selectedSlot = previousSlot;
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

    private void fullReset(MinecraftClient client) {
        if (client != null && client.options != null) {
            client.options.useKey.setPressed(false);
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
