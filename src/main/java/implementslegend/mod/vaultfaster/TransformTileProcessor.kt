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

data class RotNorm private constructor(val rotation:Int, val invertedNormal:Int,val unused:Unit){
    constructor(rotation:Int,normal:Int):this(rotation and 0b11, normal and 0b1,Unit)
}

operator fun RotNorm.plus(other:RotNorm):RotNorm = RotNorm(rotation+other.rotation,invertedNormal+other.invertedNormal)
fun RotateMirror.toRotNorm():RotNorm = when(mirror){
    Mirror.NONE -> when(rotation){
        Rotation.NONE -> RotNorm(0,0)
        Rotation.CLOCKWISE_90 -> RotNorm(1,0)
        Rotation.CLOCKWISE_180 -> RotNorm(2,0)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(3,0)
    }
    Mirror.LEFT_RIGHT -> when(rotation){
        Rotation.NONE -> RotNorm(0,1)
        Rotation.CLOCKWISE_90 -> RotNorm(3,1)
        Rotation.CLOCKWISE_180 -> RotNorm(2,1)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(1,1)
    }
    Mirror.FRONT_BACK -> when(rotation){
        Rotation.NONE -> RotNorm(2,1)
        Rotation.CLOCKWISE_90 -> RotNorm(1,1)
        Rotation.CLOCKWISE_180 -> RotNorm(0,1)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(3,1)
    }
}

fun RotNorm.toRotateMirror():RotateMirror = RotateMirror(
    when (rotation and 0b11) {
        0 -> Rotation.NONE
        1 -> Rotation.CLOCKWISE_90
        2 -> Rotation.CLOCKWISE_180
        3 -> Rotation.COUNTERCLOCKWISE_90
        else -> throw IllegalStateException("what the hell?")
    }, if (invertedNormal==0) Mirror.NONE else if (rotation and 1 == 0) Mirror.LEFT_RIGHT else Mirror.FRONT_BACK
)

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
fun mergeRotateMirror(a:RotateMirror,b:RotateMirror):RotateMirror = (a.toRotNorm()+b.toRotNorm()).toRotateMirror()

fun mergeTransforms(a:TransformTileProcessor,b:TransformTileProcessor):TransformTileProcessor{
    return TransformTileProcessor(mergeRotateMirror(a.rotateMirror,b.rotateMirror),b.positionTransform*a.positionTransform)/*.apply {
        val a1 = this.positionTransform.a.x
        val a2 = this.positionTransform.c.x
        val a3 = this.positionTransform.a.z
        val a4 = this.positionTransform.c.z

        val b1 = when(rotateMirror.rotation){
            Rotation.NONE -> 1
            Rotation.CLOCKWISE_90 -> 0
            Rotation.CLOCKWISE_180 -> -1
            Rotation.COUNTERCLOCKWISE_90 -> 0
        }

        val b2 = when(rotateMirror.rotation){
            Rotation.NONE -> 0
            Rotation.CLOCKWISE_90 -> 1
            Rotation.CLOCKWISE_180 -> 0
            Rotation.COUNTERCLOCKWISE_90 -> -1
        }

        val b3 = when(rotateMirror.rotation){
            Rotation.NONE -> if (rotateMirror.mirror===Mirror.NONE)0 else 0
            Rotation.CLOCKWISE_90 -> if (rotateMirror.mirror===Mirror.NONE)-1 else 1
            Rotation.CLOCKWISE_180 ->  if (rotateMirror.mirror===Mirror.NONE)0 else 0
            Rotation.COUNTERCLOCKWISE_90 ->  if (rotateMirror.mirror===Mirror.NONE)1 else -1
        }

        val b4 =  when(rotateMirror.rotation){
            Rotation.NONE -> if (rotateMirror.mirror===Mirror.NONE)1 else -1
            Rotation.CLOCKWISE_90 -> if (rotateMirror.mirror===Mirror.NONE)0 else 0
            Rotation.CLOCKWISE_180 ->  if (rotateMirror.mirror===Mirror.NONE)-1 else 1
            Rotation.COUNTERCLOCKWISE_90 ->  if (rotateMirror.mirror===Mirror.NONE)0 else 0
        }

        if(
            a1!=b1 &&
            a2!=b2 &&
            a3!=b3 &&
            a4!=b4
            ) {
            println("${a.rotateMirror} ${b.rotateMirror}")
            println("actual:")
            println("${rotateMirror}")
            println("$a1 $a2")
            println("$a3 $a4")

            println("expected:")

            println("$b1 $b2")
            println("$b3 $b4")
        }
    }*/

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
