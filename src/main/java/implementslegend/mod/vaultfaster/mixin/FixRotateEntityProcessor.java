package implementslegend.mod.vaultfaster.mixin;


import iskallia.vault.core.world.data.entity.PartialEntity;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.entity.RotateEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotateEntityProcessor.class)
public abstract class FixRotateEntityProcessor {

    private static ResourceLocation GLASS_FRAME_ID = new ResourceLocation("quark","glass_frame");

    @Shadow public abstract BlockPos transform(BlockPos pos);

    @Shadow public abstract Vec3 transform(Vec3 pos);

    @Shadow public abstract Tuple<Float, Direction> transformHangingEntity(float yaw, Direction direction);

    @Shadow @Final public Rotation rotation;

    @Inject(method = "process(Liskallia/vault/core/world/data/entity/PartialEntity;Liskallia/vault/core/world/processor/ProcessorContext;)Liskallia/vault/core/world/data/entity/PartialEntity;",at= @At(value = "HEAD"),cancellable = true,remap = false)
    private void rotateGlassItemFrame(PartialEntity entity, ProcessorContext context, CallbackInfoReturnable<PartialEntity> cir){
        if(GLASS_FRAME_ID.equals(entity.getId())) {
            entity.setBlockPos(this.transform(entity.getBlockPos()));
            entity.setPos(this.transform(entity.getPos()));
            CompoundTag nbt = (CompoundTag) entity.getNbt().asWhole().orElse(null);
            if (nbt != null && nbt.contains("Rotation")) {
                System.out.println("mirror: "+rotation);
                ListTag rotation = nbt.getList("Rotation", 5);
                float yaw = rotation.getFloat(0);
                EntityType<?> type = (EntityType<?>)EntityType.by(nbt).orElse(EntityType.ARMOR_STAND);
                Direction direction = Direction.from3DDataValue(nbt.getByte("Facing"));
                System.out.println("old: "+direction);
                Tuple<Float, Direction> result = this.transformHangingEntity(yaw, direction);
                System.out.println("new: "+result.getB());
                direction = (Direction) result.getB();
                nbt.putByte("Facing", (byte) direction.get3DDataValue());
                rotation.set(0, FloatTag.valueOf((Float) result.getA() - rotation.getFloat(0)));
            }

            cir.setReturnValue(entity);
        }
    }

    @Redirect(method = "process(Liskallia/vault/core/world/data/entity/PartialEntity;Liskallia/vault/core/world/processor/ProcessorContext;)Liskallia/vault/core/world/data/entity/PartialEntity;",at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"))
    private boolean fixContains(CompoundTag instance, String p_128426_, int p_128427_){
        return instance.contains(p_128426_);
    }
}
