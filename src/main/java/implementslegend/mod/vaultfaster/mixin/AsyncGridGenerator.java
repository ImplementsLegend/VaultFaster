package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.VaultGenerationExecutorKt;
import iskallia.vault.core.Version;
import iskallia.vault.core.data.key.FieldKey;
import iskallia.vault.core.event.CommonEvents;
import iskallia.vault.core.event.common.TemplateGenerationEvent;
import iskallia.vault.core.random.ChunkRandom;
import iskallia.vault.core.util.ObjectCache;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.core.world.generator.VaultGenerator;
import iskallia.vault.core.world.generator.layout.GridLayout;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.template.EmptyTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static iskallia.vault.core.vault.Vault.SEED;
import static iskallia.vault.core.world.generator.GridGenerator.CELL_X;
import static iskallia.vault.core.world.generator.GridGenerator.CELL_Z;

@Mixin(GridGenerator.class)
public abstract class AsyncGridGenerator extends VaultGenerator {


    @Shadow protected ObjectCache<RegionPos, ConfiguredTemplate> cache;

    @Shadow @Final public static FieldKey<GridLayout> LAYOUT;

    @Inject(method = "generate(Liskallia/vault/core/vault/Vault;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/ChunkPos;)V",at = @At("HEAD"),remap = false,cancellable = true)
    private void generateAsync(Vault vault, ServerLevelAccessor world, ChunkPos chunkPos, CallbackInfo ci){
        VaultGenerationExecutorKt.getVAULT_GENERATION_EXECUTOR().execute(()->{
            var cellX = this.get(CELL_X);
            var cellZ = this.get(CELL_Z);
            var seed = vault.get(SEED);
            BlockPos pos1 = new BlockPos(chunkPos.x * 16, Integer.MIN_VALUE, chunkPos.z * 16);
            BlockPos pos2 = new BlockPos(chunkPos.x * 16 + 15, Integer.MAX_VALUE, chunkPos.z * 16 + 15);
            int offsetX = Math.floorMod(pos1.getX(), cellX);
            int offsetZ = Math.floorMod(pos1.getZ(), cellZ);

            for(int x = pos1.getX(); x <= pos2.getX(); x +=  - offsetX) {
                for(int z = pos1.getZ(); z <= pos2.getZ(); z += this.get(CELL_Z) - offsetZ) {
                    RegionPos region = RegionPos.ofBlockPos(new BlockPos(x, 0, z), cellX, cellZ);
                    ChunkRandom random = ChunkRandom.any();
                    if ((vault.get(Vault.VERSION)).isOlderThan(Version.v1_7)) {
                        random.setCarverSeed(seed, region.getX(), region.getZ());
                    } else {
                        random.setRegionSeed(seed, region.getX(), region.getZ(), 1234567890L);
                    }

                    if (this.cache == null) {
                        return;
                    }

                    ConfiguredTemplate template;
                    if (this.cache.has(region)) {
                        template = this.cache.get(region);
                    } else {
                        PlacementSettings settings = new PlacementSettings(new ProcessorContext(vault, random)).setFlags(3);
                        template = this.get(LAYOUT).getAt(vault, region, random, settings).configure(ChunkedTemplate::new, settings);
                        if (template.getParent() != EmptyTemplate.INSTANCE) {
                            this.cache.set(region, template);
                        }
                    }

                    if (template != null) {
                        template = CommonEvents.TEMPLATE_GENERATION.invoke(world, template, region, chunkPos, random, TemplateGenerationEvent.Phase.PRE).getTemplate();
                        template.place(world, chunkPos);
                        CommonEvents.TEMPLATE_GENERATION.invoke(world, template, region, chunkPos, random, TemplateGenerationEvent.Phase.POST);
                    }
                }
            }
        });
        ci.cancel();
    }
}
