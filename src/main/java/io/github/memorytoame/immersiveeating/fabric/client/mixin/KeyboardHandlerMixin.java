package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void food$blockHotbarKeys(long handle, int action, KeyEvent event, CallbackInfo ci) {
      if (AnimationUtils.isInputLocked() && !food$isAllowedDuringAnimation(event)) {
         ci.cancel();
      }
   }

   @Unique
   private static boolean food$isAllowedDuringAnimation(KeyEvent event) {
      Options options = Minecraft.getInstance().options;
      return options.keyAttack.matches(event)
              || event.key() == InputConstants.KEY_ESCAPE
              || options.keySprint.matches(event)
              || options.keyScreenshot.matches(event)
              || options.keyQuickActions.matches(event)
              || options.keyAdvancements.matches(event)
              || options.keyTogglePerspective.matches(event)
              || options.keyUp.matches(event)
              || options.keyDown.matches(event)
              || options.keyLeft.matches(event)
              || options.keyRight.matches(event)
              || options.keyFullscreen.matches(event)
              || options.keyChat.matches(event)
              || options.keyInventory.matches(event)
              || options.keyToggleGui.matches(event)
              || options.keyJump.matches(event);
   }
}

