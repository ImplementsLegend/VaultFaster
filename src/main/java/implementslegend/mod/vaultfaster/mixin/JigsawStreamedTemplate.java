package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.template.JigsawTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(JigsawTemplate.class)
public class JigsawStreamedTemplate implements StreamedTemplate {


    @Shadow private Consumer<PlacementSettings> configurator;

    @Shadow private List<JigsawTemplate> children;

    @Shadow private Template root;

    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {
        PlacementSettings copy = settings.copy();
        this.configurator.accept(copy);

        var templateList = new ArrayList<Template>(this.children);
        templateList.add(0,this.root);
        return templateList.parallelStream().flatMap(child -> ((StreamedTemplate)child).getTileStream(filter, copy));
    }
}
