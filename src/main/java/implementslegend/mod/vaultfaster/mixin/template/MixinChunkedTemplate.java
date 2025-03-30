package implementslegend.mod.vaultfaster.mixin.template;

import implementslegend.mod.vaultfaster.SectionedTemplate;
import implementslegend.mod.vaultfaster.interfaces.SectionedTemplateContainer;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(ChunkedTemplate.class)
public abstract class MixinChunkedTemplate extends ConfiguredTemplate implements SectionedTemplateContainer {

    private SectionedTemplate st = new SectionedTemplate(this);

    public MixinChunkedTemplate(Template parent, PlacementSettings settings) {
        super(parent, settings);
    }

    @NotNull
    @Override
    public SectionedTemplate getSectionedTemplate() {
        return st;
    }

    @Overwrite(remap = false)
    public void place(ServerLevelAccessor world, ChunkPos pos) {
        st.place(world, pos);
    }

}
