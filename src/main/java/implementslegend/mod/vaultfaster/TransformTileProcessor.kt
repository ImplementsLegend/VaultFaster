package implementslegend.mod.vaultfaster

import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.processor.tile.MirrorTileProcessor
import iskallia.vault.core.world.processor.tile.RotateTileProcessor
import iskallia.vault.core.world.processor.tile.TileProcessor
import iskallia.vault.core.world.processor.tile.TranslateTileProcessor
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation

data class RotateMirror(val rotation: Rotation, val mirror: Mirror)

data class Vec4i(val x:Int,val y:Int,val z:Int,val w:Int)

data class Mat4i(val a:Vec4i,val b:Vec4i,val c:Vec4i,val d:Vec4i)

operator fun Vec4i.times(it:Int) = Vec4i(x*it,y*it,z*it,w*it)
operator fun Vec4i.plus(it:Vec4i) = Vec4i(x+it.x,y+it.y,z+it.z,w+it.w)

operator fun Vec4i.times(it:Mat4i)=it.a*x+it.b*y+it.c*z+it.d*w
operator fun Mat4i.times(it:Mat4i)=Mat4i(it.a*this,it.b*this,it.c*this,it.d*this)

class TransformTileProcessor(val rotateMirror: RotateMirror, val positionTransform:Mat4i):TileProcessor() {


    override fun process(p0: PartialTile, p1: ProcessorContext): PartialTile? {

        if(rotateMirror.mirror===Mirror.NONE && rotateMirror.rotation===Rotation.NONE){
            p0.pos= BlockPos(p0.pos.x+positionTransform.d.x,p0.pos.y+positionTransform.d.y,p0.pos.z+positionTransform.d.z)
            return p0
        }

        val pos = Vec4i(p0.pos.x,p0.pos.y,p0.pos.z,1)


        p0.state.mapAndSet {
            (if (rotateMirror.rotation!==Rotation.NONE)it.rotate(rotateMirror.rotation) else it).let {
                (if (rotateMirror.mirror!==Mirror.NONE)it.mirror(rotateMirror.mirror) else it)
            }
        }


        p0.pos=(pos*positionTransform).let { BlockPos(it.x,it.y,it.z) }
        return p0
    }

}

fun RotateTileProcessor.toTransformTileProcessor():TransformTileProcessor= when(this.rotation){
    Rotation.NONE -> transformIdentity()

    Rotation.CLOCKWISE_90 -> {
        val termX = pivotX+pivotZ
        val termZ = pivotZ-pivotX-if(centered)0 else 1
        TransformTileProcessor(
            RotateMirror(Rotation.CLOCKWISE_90,Mirror.NONE),
            Mat4i(
                Vec4i(0,0,1,0),
                Vec4i(0,1,0,0),
                Vec4i(-1,0,0,0),
                Vec4i(termX,0,termZ,1)
            )
        )
    }
    Rotation.COUNTERCLOCKWISE_90 -> {
        val termX = pivotX-pivotZ-if(centered)0 else 1
        val termZ = pivotX+pivotZ
        TransformTileProcessor(
            RotateMirror(Rotation.COUNTERCLOCKWISE_90,Mirror.NONE),
            Mat4i(
                Vec4i(0,0,-1,0),
                Vec4i(0,1,0,0),
                Vec4i(1,0,0,0),
                Vec4i(termX,0,termZ,1)
            )
        )
    }

    Rotation.CLOCKWISE_180 -> {
        val termX = 2*pivotX-if(centered)0 else 1
        val termZ = 2*pivotZ-if(centered)0 else 1
        TransformTileProcessor(
            RotateMirror(Rotation.CLOCKWISE_180,Mirror.NONE),
            Mat4i(
                Vec4i(-1,0,0,0),
                Vec4i(0,1,0,0),
                Vec4i(0,0,-1,0),
                Vec4i(termX,0,termZ,1)
            )
        )
    }
}

fun TranslateTileProcessor.toTransformTileProcessor():TransformTileProcessor= TransformTileProcessor(
    RotateMirror(Rotation.NONE,Mirror.NONE),
    Mat4i(
        Vec4i(1,0,0,0),
        Vec4i(0,1,0,0),
        Vec4i(0,0,1,0),
        Vec4i(this.offsetX,this.offsetY,this.offsetZ,1)
    )
)
fun MirrorTileProcessor.term() = 2*plane-if(centered)0 else 1
fun MirrorTileProcessor.toTransformTileProcessor():TransformTileProcessor= when(this.mirror){
    Mirror.NONE -> transformIdentity()
    Mirror.LEFT_RIGHT -> TransformTileProcessor(
        RotateMirror(Rotation.NONE,Mirror.LEFT_RIGHT),
        Mat4i(
            Vec4i(1,0,0,0),
            Vec4i(0,1,0,0),
            Vec4i(0,0,-1,0),
            Vec4i(0,0,this.term(),1)
        )
    )
    Mirror.FRONT_BACK -> TransformTileProcessor(
        RotateMirror(Rotation.NONE,Mirror.FRONT_BACK),
        Mat4i(
            Vec4i(-1,0,0,0),
            Vec4i(0,1,0,0),
            Vec4i(0,0,1,0),
            Vec4i(this.term(),0,0,1)
        )
    )
}

fun mat4Identity(): Mat4i = Mat4i(
    Vec4i(1,0,0,0),
    Vec4i(0,1,0,0),
    Vec4i(0,0,1,0),
    Vec4i(0,0,0,1),
)

fun transformIdentity(): TransformTileProcessor = TransformTileProcessor(
    RotateMirror(Rotation.NONE,Mirror.NONE),
    mat4Identity()
)
