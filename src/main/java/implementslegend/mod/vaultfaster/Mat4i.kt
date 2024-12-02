package implementslegend.mod.vaultfaster

data class Vec4i(val x:Int,val y:Int,val z:Int,val w:Int)
data class Mat4i(val a: Vec4i, val b: Vec4i, val c: Vec4i, val d: Vec4i)

operator fun Vec4i.times(it:Int) = Vec4i(x * it, y * it, z * it, w * it)
operator fun Vec4i.plus(it: Vec4i) = Vec4i(x + it.x, y + it.y, z + it.z, w + it.w)
operator fun Vec4i.times(it: Mat4i)=it.a*x+it.b*y+it.c*z+it.d*w
operator fun Mat4i.times(it: Mat4i)= Mat4i(it.a * this, it.b * this, it.c * this, it.d * this)
fun mat4Identity(): Mat4i = Mat4i(
    Vec4i(1, 0, 0, 0),
    Vec4i(0, 1, 0, 0),
    Vec4i(0, 0, 1, 0),
    Vec4i(0, 0, 0, 1),
)