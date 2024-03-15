package implementslegend.mod.vaultfaster.mixin

import iskallia.vault.core.world.template.JigsawTemplate
import iskallia.vault.core.world.template.Template
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(JigsawTemplate::class)
interface JigsawRootAccessor {
    var root: Template @Accessor get @Accessor set
}