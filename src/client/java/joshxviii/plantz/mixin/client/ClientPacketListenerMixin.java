package joshxviii.plantz.mixin.client;

import joshxviii.plantz.PazItems;
import joshxviii.plantz.gui.LetterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Josh
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(
        method = "handleOpenBook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void plantz$openLetterScreen(ClientboundOpenBookPacket packet, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack held = mc.player.getItemInHand(packet.getHand());
        if (held.is(PazItems.LETTER)) {
            if (!(mc.screen instanceof LetterScreen)) {
                mc.setScreen(new LetterScreen(held, packet.getHand()));
            }
            ci.cancel();
        }
    }
}
