package implementslegend.mod.vaultfaster.mixin.modifier;

import implementslegend.mod.vaultfaster.FixatedBlockIDsKt;
import implementslegend.mod.vaultfaster.TileMapperKt;
import implementslegend.mod.vaultfaster.interfaces.CascadeApplicableIndices;
import iskallia.vault.core.vault.modifier.modifier.DecoratorCascadeModifier;
import iskallia.vault.core.world.data.tile.TilePredicate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;

@Mixin(DecoratorCascadeModifier.Properties.class)
public class MixinCascadeProperties implements CascadeApplicableIndices {
    @Shadow @Final private TilePredicate filter;
    private BitSet applicableIndices = new BitSet(0);

    @Nullable
    @Override
    public BitSet getApplicableIndices() {
        if(applicableIndices==null || applicableIndices.isEmpty()) {
            var blockCount = FixatedBlockIDsKt.getBLOCKS().size();
            applicableIndices=new BitSet(blockCount);
            var iteartor = TileMapperKt.getIndices(filter).iterator();
            while (iteartor.hasNext()) {
                var idx = iteartor.next();
                if (applicableIndices != null) {
                    if (idx < 0 || idx >= blockCount) applicableIndices = null;
                    else applicableIndices.set(idx);
                }
            }
        }
        return applicableIndices;
    }
}
