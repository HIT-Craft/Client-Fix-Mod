package clientfix.Mixin;

import net.minecraft.client.sound.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    /*
     * @author ten_miles_away, Fallen_Breath
     * @reason fix a bug that some instances will never get removed
     */
    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/Channel$SourceManager;isStopped()Z"
            ),
            allow = 1
    )
    boolean pleaseRemoveInstanceThatWillNeverBeAccessed(Channel.SourceManager sourceManager)
    {
        return true;
    }
}
