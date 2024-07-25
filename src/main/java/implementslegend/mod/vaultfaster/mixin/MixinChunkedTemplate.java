package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.AtomicallyIndexedConcurrentArrayCollection;
import implementslegend.mod.vaultfaster.SectionedTemplate;
import implementslegend.mod.vaultfaster.VaultGenerationExecutorKt;
import iskallia.vault.core.world.data.entity.PartialEntity;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StaticTemplate;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import kotlin.Unit;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkedTemplate.class)
public abstract class MixinChunkedTemplate extends ConfiguredTemplate {

    private SectionedTemplate st = new SectionedTemplate(this);

    public MixinChunkedTemplate(Template parent, PlacementSettings settings) {
        super(parent, settings);
    }


    @Overwrite(remap = false)
    public void place(ServerLevelAccessor world, ChunkPos pos) {
        st.place(world, pos);
    }

}
