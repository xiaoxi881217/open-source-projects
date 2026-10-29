package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable

@Serializable enum class HomeKeyShape(val label:String) { ROUNDED("圆角"), SQUARE("正方形"), CIRCLE("圆形") }
@Serializable enum class HomeBorderKind(val label:String) { NONE("无"), SOLID("实线"), DASHED("虚线") }
@Serializable data class HomeBorder(val kind:HomeBorderKind=HomeBorderKind.NONE,val colorLight:Long?=null,val colorDark:Long?=null,
    val widthDp:Float=1f,val opacity:Float=1f) {
    fun validate(){require(widthDp.isFinite()&&widthDp in .5f..6f&&opacity.isFinite()&&opacity in 0f..1f){"边框粗细或透明度超出范围"};require(listOfNotNull(colorLight,colorDark).all{it in 0xFF000000..0xFFFFFFFF}){"边框颜色不合适"}}
}

data class HomeFaceRect(val left:Float,val top:Float,val width:Float,val height:Float)
fun homeFaceCorner(kind:HomeKeyShape,width:Float,height:Float,radius:Float):Float =
    if(kind==HomeKeyShape.CIRCLE)minOf(width,height).coerceAtLeast(0f)/2 else radius.coerceIn(0f,minOf(width,height).coerceAtLeast(0f)/2)
/** 长功能键保留长条，其余键帽居中正方/正圆；布局和命中区域不改。 */
fun homeFaceRect(shape:HomeKeyShape,width:Float,height:Float):HomeFaceRect {
    if(shape==HomeKeyShape.ROUNDED || width>=height*1.6f)return HomeFaceRect(0f,0f,width,height)
    val edge=minOf(width,height)
    return HomeFaceRect((width-edge)/2,(height-edge)/2,edge,edge)
}
