package joshxviii.plantz.mixin.client;

import joshxviii.plantz.PazItems;
import joshxviii.plantz.gui.LetterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "openItemGui", at = @At("HEAD"), cancellable = true)
    private void plantz$openLetterScreen(ItemStack itemStack, InteractionHand hand, CallbackInfo ci) {
        if (itemStack.is(PazItems.LETTER)) {
            Minecraft.getInstance().setScreen(new LetterScreen(itemStack, hand));
            ci.cancel();
        }
    }
}
