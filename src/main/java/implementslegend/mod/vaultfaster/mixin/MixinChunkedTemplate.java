package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.SectionedTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.*;

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
