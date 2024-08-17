package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.ConcurrentGridCache;
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
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static iskallia.vault.core.world.generator.GridGenerator.CELL_X;
import static iskallia.vault.core.world.generator.GridGenerator.CELL_Z;

@Mixin(GridGenerator.class)
public class ConcurrentGridGenerator {

    @Shadow @Final public static FieldKey<iskallia.vault.core.world.generator.layout.GridLayout> LAYOUT;
    private ConcurrentGridCache concurrentCache = new ConcurrentGridCache();

    @Overwrite(remap = false)
    public void generate(Vault vault, ServerLevelAccessor world, ChunkPos chunkPos) {
        BlockPos min = new BlockPos(chunkPos.x * 16, Integer.MIN_VALUE, chunkPos.z * 16);
        BlockPos max = new BlockPos(chunkPos.x * 16 + 15, Integer.MAX_VALUE, chunkPos.z * 16 + 15);

        var cellX = ((VaultGenerator)(Object)this).get(CELL_X).intValue();
        var cellZ = ((VaultGenerator)(Object)this).get(CELL_Z).intValue();
        var versionFlag = vault.get(Vault.VERSION).isOlderThan(Version.v1_7);
        var layout = ((VaultGenerator)(Object)this).get(LAYOUT);
        var seed = vault.get(Vault.SEED).longValue();
        for (var x = min.getX();x <= max.getX();x += cellX - Math.floorMod(x, cellX)) {
            for (int z = min.getZ(); z <= max.getZ(); z += cellZ - Math.floorMod(z, cellZ)) {
                RegionPos region = RegionPos.ofBlockPos(new BlockPos(x, 0, z), cellX, cellZ);
                ChunkRandom random = ChunkRandom.any();
                if (versionFlag) {
                    random.setCarverSeed(seed, region.getX(), region.getZ());
                } else {
                    random.setRegionSeed(seed, region.getX(), region.getZ(), 1234567890L);
                }

                ConfiguredTemplate template = concurrentCache.getAt(region,layout,vault,random);

                if (template != null) {
                    template = CommonEvents.TEMPLATE_GENERATION.invoke(world, template, region, chunkPos, random, TemplateGenerationEvent.Phase.PRE).getTemplate();
                    template.place(world, chunkPos);
                    CommonEvents.TEMPLATE_GENERATION.invoke(world, template, region, chunkPos, random, TemplateGenerationEvent.Phase.POST);
                }
            }


        }
    }
}
