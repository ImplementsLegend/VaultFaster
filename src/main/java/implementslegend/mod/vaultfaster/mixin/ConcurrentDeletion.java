package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.core.world.storage.VirtualWorld;
import iskallia.vault.nbt.VListNBT;
import iskallia.vault.world.data.VirtualWorlds;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;

@Mixin(VirtualWorlds.class)
public class ConcurrentDeletion {

    @Shadow private VListNBT<VirtualWorld, CompoundTag> entries;

    @Redirect(method = "tickDeletions",at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;walkFileTree(Ljava/nio/file/Path;Ljava/nio/file/FileVisitor;)Ljava/nio/file/Path;"),remap = false)
    private static Path deleteConcurrently(Path start, FileVisitor<? super @NotNull Path> visitor) throws IOException {

        Util.ioPool().execute(()-> {
            try {
                FileUtils.deleteDirectory(start.toFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return Path.of("");
    }
}
