package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.AtomicallyIndexedConcurrentArrayCollection;
import implementslegend.mod.vaultfaster.StreamedTemplate;
import iskallia.vault.core.world.data.entity.PartialEntity;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StaticTemplate;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

@Mixin(targets = "iskallia.vault.core.world.template.configured.ChunkedTemplate$Wrapping")
public class WrappingStreamedTemplate implements StreamedTemplate {

    @Shadow @Final ChunkedTemplate this$0;


    @Shadow @Final private Template parent;

    @Shadow @Final private TileProcessor tileBound;

    private static final Field cache;

    static {
        try {
            cache = ChunkedTemplate.class.getDeclaredField("cache");
            //cache.trySetAccessible();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {


        return ((StreamedTemplate)this.parent).getTileStream(filter, settings).map( tile -> {

            try {
                StaticTemplate child = ((Map<ChunkPos,StaticTemplate>)this.cache.get(this$0))
                        .computeIfAbsent(new ChunkPos(tile.getPos()), pos -> new StaticTemplate(new AtomicallyIndexedConcurrentArrayCollection<>(new PartialTile[131072]), new ArrayList<>(128)));
                ((AtomicallyIndexedConcurrentArrayCollection<PartialTile>)child.getTiles()).addActual(tile);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return this.tileBound.process(tile, settings.getProcessorContext());
        });
    }
}
