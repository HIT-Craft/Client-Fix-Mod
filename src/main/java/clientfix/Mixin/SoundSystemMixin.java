package clientfix.Mixin;

import com.google.common.collect.Multimap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Shadow private int ticks;

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    @Shadow public abstract void stop(SoundInstance soundInstance);

    @Shadow protected abstract float getAdjustedVolume(SoundInstance soundInstance);

    @Shadow protected abstract float getAdjustedPitch(SoundInstance soundInstance);

    @Shadow @Final private Map<SoundInstance, Channel.SourceManager> sources;

    @Shadow @Final private GameOptions settings;

    @Shadow @Final private Map<SoundInstance, Integer> soundEndTicks;

    @Shadow @Final private Map<SoundInstance, Integer> startTicks;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private static Marker MARKER;

    @Shadow @Final private Multimap<SoundCategory, SoundInstance> sounds;

    @Shadow public abstract void play(SoundInstance soundInstance);

    /**
     * @author ten_miles_away
     * @reason fix bugs
     */
    @Overwrite
    private void tick() {
        ++this.ticks;
        Iterator iterator = this.tickingSounds.iterator();

        while(iterator.hasNext()) {
            TickableSoundInstance tickableSoundInstance = (TickableSoundInstance)iterator.next();
            tickableSoundInstance.tick();
            if (tickableSoundInstance.isDone()) {
                this.stop(tickableSoundInstance);
            } else {
                float f = this.getAdjustedVolume(tickableSoundInstance);
                float g = this.getAdjustedPitch(tickableSoundInstance);
                Vec3d vec3d = new Vec3d((double)tickableSoundInstance.getX(), (double)tickableSoundInstance.getY(), (double)tickableSoundInstance.getZ());
                Channel.SourceManager sourceManager = (Channel.SourceManager)this.sources.get(tickableSoundInstance);
                if (sourceManager != null) {
                    sourceManager.run((source) -> {
                        source.setVolume(f);
                        source.setPitch(g);
                        source.setPosition(vec3d);
                    });
                }
            }
        }

        iterator = this.sources.entrySet().iterator();

        SoundInstance soundInstance;
        while(iterator.hasNext()) {

            Map.Entry<SoundInstance, Channel.SourceManager> entry = (Map.Entry)iterator.next();
            Channel.SourceManager sourceManager2 = (Channel.SourceManager)entry.getValue();

            soundInstance = (SoundInstance)entry.getKey();
            float h = this.settings.getSoundVolume(soundInstance.getCategory());

            if (h <= 0.0F) {
                sourceManager2.run(Source::stop);
                iterator.remove();
            } else if (sourceManager2.isStopped()) {

                System.out.println("Stopped");

                int i = (Integer)this.soundEndTicks.get(soundInstance);

                if (i <= this.ticks) {
                    int j = soundInstance.getRepeatDelay();
                    if (soundInstance.isRepeatable() && j > 0) {
                        this.startTicks.put(soundInstance, this.ticks + j);
                    }

                    iterator.remove();
                    LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", sourceManager2);
                    this.soundEndTicks.remove(soundInstance);

                    try {
                        this.sounds.remove(soundInstance.getCategory(), soundInstance);
                    } catch (RuntimeException var9) {
                    }

                    if (soundInstance instanceof TickableSoundInstance) {
                        this.tickingSounds.remove(soundInstance);
                    }
                }
            } else {
                int j = soundInstance.getRepeatDelay();
                if (soundInstance.isRepeatable() && j > 0) {
                    this.startTicks.put(soundInstance, this.ticks + j);
                }

                iterator.remove();
                LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", sourceManager2);
                this.soundEndTicks.remove(soundInstance);

                try {
                    this.sounds.remove(soundInstance.getCategory(), soundInstance);
                } catch (RuntimeException var9) {
                }

                if (soundInstance instanceof TickableSoundInstance) {
                    this.tickingSounds.remove(soundInstance);
                }
            }
        }

        Iterator iterator2 = this.startTicks.entrySet().iterator();

        while(iterator2.hasNext()) {
            Map.Entry<SoundInstance, Integer> entry2 = (Map.Entry)iterator2.next();
            if (this.ticks >= (Integer)entry2.getValue()) {
                soundInstance = (SoundInstance)entry2.getKey();
                if (soundInstance instanceof TickableSoundInstance) {
                    ((TickableSoundInstance)soundInstance).tick();
                }

                this.play(soundInstance);
                iterator2.remove();
            }
        }

    }
}
