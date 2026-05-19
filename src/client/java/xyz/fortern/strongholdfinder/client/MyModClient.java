package xyz.fortern.strongholdfinder.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fortern.strongholdfinder.math.Geometry;
import xyz.fortern.strongholdfinder.math.Vector2;

import java.util.Locale;

public class MyModClient implements ClientModInitializer {
    public static final String MOD_ID = "fsf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("minecraft", "dimension")),
            Identifier.fromNamespaceAndPath("minecraft", "overworld")
    );

    private static Tracker tracker;

    @Override
    public void onInitializeClient() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> tracker = null);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide() || !level.dimension().equals(OVERWORLD)) {
                return InteractionResult.PASS;
            }
            ItemStack itemStack;
            if (hand.asEquipmentSlot() == EquipmentSlot.MAINHAND) {
                itemStack = player.getMainHandItem();
            } else {
                itemStack = player.getOffhandItem();
            }
            if (!(itemStack.getItem() instanceof EnderEyeItem)) {
                return InteractionResult.PASS;
            }
            long gameTime = level.getGameTime();
            if (tracker == null) {
                tracker = new Tracker(gameTime);
            } else {
                if (gameTime - tracker.throwTime > 2400) {
                    // timeout
                    tracker = new Tracker(gameTime);
                }
            }
            return InteractionResult.PASS;
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (tracker == null) {
                return;
            }
            if (!level.isClientSide() || !level.dimension().equals(OVERWORLD) || !(entity instanceof EyeOfEnder enderEye)) {
                return;
            }
            Vec3 position = enderEye.position();
            LocalPlayer player = Minecraft.getInstance().player;
            assert player != null;
            if (player.position().x - position.x > 16 || player.position().z - position.z > 16) {
                return;
            }

            int throwNumber;
            if (tracker.firstEyeStep == 0) {
                throwNumber = 1;
                tracker.firstEyeLoadPos = new Vector2(position.x, position.z);
                tracker.firstEyeStep = 1;
            } else {
                throwNumber = 2;
                tracker.secondEyeLoadPos = new Vector2(position.x, position.z);
                tracker.secondEyeStep = 1;
            }
            player.sendSystemMessage(
                    Component.translatable(
                            "message.forternstrongholdfinder.eye_detected",
                            throwNumber,
                            coordinateComponent(position)
                    )
            );
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (tracker == null) {
                return;
            }
            if (tracker.firstEyeStep != 1 && tracker.secondEyeStep != 1) {
                return;
            }
            if (!level.isClientSide() || !level.dimension().equals(OVERWORLD) || !(entity instanceof EyeOfEnder enderEye)) {
                return;
            }
            Vec3 position = enderEye.position();
            LocalPlayer player = Minecraft.getInstance().player;
            assert player != null;
            Vector2 eyeUnloadPos = new Vector2(position.x, position.z);

            if (tracker.firstEyeStep == 1) {
                tracker.firstEyeUnloadPos = eyeUnloadPos;
                tracker.firstEyeStep = 2;
                player.sendSystemMessage(
                        Component.translatable(
                                "message.forternstrongholdfinder.eye_last_seen",
                                1,
                                coordinateComponent(position)
                        )
                );
            } else if (tracker.secondEyeStep == 1) {
                player.sendSystemMessage(
                        Component.translatable(
                                "message.forternstrongholdfinder.eye_last_seen",
                                2,
                                coordinateComponent(position)
                        )
                );
                tracker.secondEyeStep = 2;
            }
            if (tracker.secondEyeStep == 2) {
                Vector2 target = Geometry.intersectRays(tracker.firstEyeLoadPos, tracker.firstEyeUnloadPos, tracker.secondEyeLoadPos, eyeUnloadPos);
                tracker = null;
                if (Double.isNaN(target.x()) || Double.isNaN(target.z())) {
                    player.sendSystemMessage(Component.translatable("message.forternstrongholdfinder.calculation_failed"));
                } else {
                    String displayText = String.format(Locale.ROOT, "(%.2f, y, %.2f)", target.x(), target.z());
                    String teleportCommand = String.format(Locale.ROOT, "/teleport @s %.2f ~ %.2f", target.x(), target.z());
                    player.sendSystemMessage(
                            Component.translatable("message.forternstrongholdfinder.stronghold_estimated",
                                    Component.literal(displayText).withStyle(
                                            Style.EMPTY.withColor(ChatFormatting.GREEN)
                                                    .withClickEvent(new ClickEvent.SuggestCommand(teleportCommand))
                                                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable(
                                                            "message.forternstrongholdfinder.suggest_teleport"
                                                    )))
                                    )
                            )
                    );
                }
            }
        });
    }

    private static Component coordinateComponent(Vec3 position) {
        return Component.literal(String.format(
                Locale.ROOT,
                "(%.2f, %.2f, %.2f)",
                position.x,
                position.y,
                position.z
        )).withStyle(ChatFormatting.GREEN);
    }

    private static class Tracker {
        public int firstEyeStep = 0;
        public int secondEyeStep = 0;
        public Vector2 firstEyeLoadPos;
        public Vector2 firstEyeUnloadPos;
        public Vector2 secondEyeLoadPos;
        public long throwTime;

        public Tracker(long throwTime) {
            this.throwTime = throwTime;
        }
    }
}

