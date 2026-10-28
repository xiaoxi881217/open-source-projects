export const angles={TOP:270,RIGHT:0,BOTTOM:90,LEFT:180};
export const clone=value=>structuredClone(value);
export const shadow=side=>({enabled:false,inset:false,color:0xff55404b,opacity:.18,blurDp:6,spreadDp:0,distanceDp:3,angleDegrees:angles[side]});
export const sticker=()=>({asset:'builtin:heart',offsetX:.3,offsetY:-.3,scale:.65,rotation:0,opacity:1,clipToKey:false,layer:"ABOVE_KEY"});
export const border=()=>({kind:"NONE",colorLight:null,colorDark:null,widthDp:1,opacity:1});
export const style=()=>({shape:null,border:null,backgroundLight:null,backgroundDark:null,textLight:null,textDark:null,cornerRadius:null,shadows:{},shadowShape:'ROUNDED',motion:'NONE',motionStrength:.65,motionDurationMs:120,sticker:null,labelOffsetX:0,labelOffsetY:0,labelFontSize:null,hintOffsetX:0,hintOffsetY:0,hintFontSize:null,hintLight:null,hintDark:null,hintOpacity:1,hintVisible:true,keyOpacity:1,frost:0});
export function valid(p){
 const number=(n,a,b)=>typeof n==='number'&&Number.isFinite(n)&&n>=a&&n<=b;
 const color=n=>Number.isInteger(n)&&number(n,0xff000000,0xffffffff);
 const keys=(o,allowed)=>o&&typeof o==='object'&&!Array.isArray(o)&&Object.keys(o).every(k=>allowed.includes(k));
 if(!keys(p,['schemaVersion','name','light','dark','gradient','gradientAngle','cornerRadius','spacingX','spacingY','candidateTextSize','keys','animationsEnabled','pressPopupEnabled','wallpaper','panelStyle','floatingStickers','keyShape','keyBorder','frameBorder','sectionSpacing'])||p.schemaVersion!==1||typeof p.name!=='string'||!p.name.trim()||p.name.length>80)throw Error('这份主题的格式或版本不合适');
 for(const mode of ['light','dark'])if(!keys(p[mode],['background','gradientEnd','key','special','text','candidate','accent'])||['background','gradientEnd','key','special','text','candidate','accent'].some(k=>!color(p[mode][k])))throw Error('主题颜色不合适');
 if(p.pressPopupEnabled!==undefined&&typeof p.pressPopupEnabled!=='boolean')throw Error('按键放大提示开关不合适');
 if(typeof p.gradient!=='boolean'||typeof p.animationsEnabled!=='boolean'||!Number.isInteger(p.gradientAngle)||!number(p.gradientAngle,0,360)||!Number.isInteger(p.cornerRadius)||!number(p.cornerRadius,0,24)||!number(p.spacingX,0,4)||!number(p.spacingY,0,8)||!Number.isInteger(p.candidateTextSize)||!number(p.candidateTextSize,14,28))throw Error('尺寸或开关超出范围');
 if(p.wallpaper!=null){const w=p.wallpaper;if(!keys(w,['asset','offsetX','offsetY','scale','opacity'])||typeof w.asset!=='string'||!/^wallpaper:[a-f0-9]{64}$/.test(w.asset)||!number(w.offsetX??0,-1,1)||!number(w.offsetY??0,-1,1)||!number(w.scale??1,1,4)||!number(w.opacity??1,0,1))throw Error('壁纸位置、大小或资源不合适')}
 const checkBorder=b=>{if(!keys(b,Object.keys(border())))throw Error('边框字段不合适');const v={...border(),...b};if(!['NONE','SOLID','DASHED'].includes(v.kind)||!number(v.widthDp,.5,6)||!number(v.opacity,0,1)||[v.colorLight,v.colorDark].some(c=>c!==null&&!color(c)))throw Error('边框参数不合适')};
 if(!['ROUNDED','SQUARE','CIRCLE'].includes(p.keyShape??'ROUNDED'))throw Error('形状不合适');for(const b of [p.keyBorder,p.frameBorder])if(b!=null)checkBorder(b);
 if(p.sectionSpacing!=null&&!number(p.sectionSpacing,0,12))throw Error('分区留白超出范围');
 if(!p.keys||typeof p.keys!=='object'||Array.isArray(p.keys)||Object.keys(p.keys).length>200)throw Error('单键设置太多了');
 const floating=p.floatingStickers??[];if(!Array.isArray(floating)||floating.length>12||new Set(floating.map(f=>f.id)).size!==floating.length)throw Error('自由贴纸数量或编号不合适');for(const f of floating)if(!keys(f,['id','sticker'])||!/^[-_a-zA-Z0-9]{1,64}$/.test(f.id)||!f.sticker||f.sticker.clipToKey===true)throw Error('自由贴纸模式不合适');
 const styles=Object.entries(p.keys);styles.push(...floating.map(f=>['qwerty:free_style',{sticker:f.sticker}]));if(p.panelStyle!=null)styles.push(["qwerty:panel_style",p.panelStyle]);
 for(const [id,s]of styles){
  if(!/^(qwerty|t9|toolbar|chrome):[a-z0-9_-]{1,40}$/.test(id)||!keys(s,Object.keys(style())))throw Error('按键设置不合适');
  const merged={...style(),...s};for(const k of ['backgroundLight','backgroundDark','textLight','textDark','hintLight','hintDark'])if(merged[k]!==null&&!color(merged[k]))throw Error('按键颜色不合适');
  if(merged.sticker!==null){const t=merged.sticker;if(!keys(t,Object.keys(sticker())))throw Error('贴纸格式不合适');const v={...sticker(),...t};if(typeof v.asset!=='string'||!(/^(builtin:(heart|star|bow|cloud)|image:[a-f0-9]{64})$/.test(v.asset))||!number(v.offsetX,-2,2)||!number(v.offsetY,-2,2)||!number(v.scale,.25,12)||!number(v.rotation,-180,180)||!number(v.opacity,0,1)||typeof v.clipToKey!=='boolean'||!['BELOW_SHADOW','ABOVE_SHADOW','ABOVE_KEY'].includes(v.layer))throw Error('贴纸位置或大小超出范围');}
  if(merged.shape!==null&&!['ROUNDED','SQUARE','CIRCLE'].includes(merged.shape))throw Error('按键形状不合适');if(merged.border!==null)checkBorder(merged.border);
  if(!number(merged.frost,0,1))throw Error('磨砂度超出范围');
  if(!number(merged.keyOpacity,0,1))throw Error('键帽透明度超出范围');
  if(merged.labelFontSize!==null&&!number(merged.labelFontSize,2,32))throw Error('文字字号请保持在2到32之间');
  if(merged.hintFontSize!==null&&!number(merged.hintFontSize,2,24)||!number(merged.hintOffsetX,-.8,.8)||!number(merged.hintOffsetY,-.8,.8)||!number(merged.hintOpacity,0,1)||typeof merged.hintVisible!=='boolean')throw Error('小字设置超出范围');
  if(!number(merged.labelOffsetX,-.4,.4)||!number(merged.labelOffsetY,-.4,.4))throw Error('文字位置超出范围');
  if(merged.cornerRadius!==null&&(!Number.isInteger(merged.cornerRadius)||!number(merged.cornerRadius,0,24)))throw Error('按键圆角超出范围');
  if(!['ROUNDED','RECTANGLE','OVAL','CUT'].includes(merged.shadowShape)||!['NONE','JELLY','TYPEWRITER','BUBBLE'].includes(merged.motion)||!number(merged.motionStrength,0,4)||!Number.isInteger(merged.motionDurationMs)||!number(merged.motionDurationMs,40,600)||!keys(merged.shadows,Object.keys(angles)))throw Error('阴影或动态设置不合适');
  for(const [side,supplied] of Object.entries(merged.shadows)){
   if(!keys(supplied,Object.keys(shadow(side))))throw Error('阴影格式不合适');const layer={...shadow(side),...supplied};
   if(typeof layer.enabled!=='boolean'||typeof layer.inset!=='boolean'||!color(layer.color)||!number(layer.opacity,0,1)||!number(layer.blurDp,0,12)||!number(layer.spreadDp,-4,6)||!number(layer.distanceDp,0,12)||!number(layer.angleDegrees,0,360))throw Error('阴影数值超出范围');
  }
 }
 p={...p,keys:Object.fromEntries(Object.entries(p.keys).map(([id,s])=>[id,s.sticker?{...s,sticker:{...sticker(),...s.sticker}}:s]))};
 if(new TextEncoder().encode(JSON.stringify(p)).length>128*1024)throw Error('主题文件太大了');return p;
}
export function synchronize(p,source,targets,property,side){
 p=valid(p);const next=clone(p), from={...style(),...(p.keys[source]||{})};
 if(!targets.length||targets.some(id=>id.split(':')[0]!==source.split(':')[0]))throw Error('请在当前键盘中选择目标');
 for(const id of targets){const into={...style(),...(next.keys[id]||{})};
  if(property==='ALL')next.keys[id]=clone(from);
  else if(property==='shape'){into.shape=from.shape??p.keyShape??'ROUNDED';next.keys[id]=into;}
  else if(property==='border'){into.border=clone(from.border??p.keyBorder??border());next.keys[id]=into;}
  else if(property.startsWith('border.')){const field=property.slice(7);if(!Object.hasOwn(border(),field))throw Error('边框属性不合适');into.border={...border(),...(into.border??p.keyBorder??{}),[field]:({...border(),...(from.border??p.keyBorder??{})})[field]};next.keys[id]=into;}
  else if(property==='shadowAll'){into.shadows=clone(from.shadows);into.shadowShape=from.shadowShape;next.keys[id]=into;}
  else if(property==='shadowSide'){if(!Object.hasOwn(angles,side))throw Error('请选择阴影边');into.shadows={...into.shadows,[side]:clone({...shadow(side),...(from.shadows[side]||{})})};next.keys[id]=into;}
  else if(property==='labelPosition'){into.labelOffsetX=from.labelOffsetX;into.labelOffsetY=from.labelOffsetY;next.keys[id]=into;}
  else if(property==='hintPosition'){into.hintOffsetX=from.hintOffsetX;into.hintOffsetY=from.hintOffsetY;next.keys[id]=into;}
  else if(property==='hintAll'){for(const k of ['hintOffsetX','hintOffsetY','hintFontSize','hintLight','hintDark','hintOpacity','hintVisible'])into[k]=from[k];next.keys[id]=into;}
  else if(property.startsWith('sticker.')){const field=property.slice(8);if(!Object.hasOwn(sticker(),field))throw Error('贴纸属性不合适');if(!from.sticker)throw Error('先给来源按键选一张贴纸');if(field==='asset')into.sticker={...sticker(),...(into.sticker||{}),asset:from.sticker.asset};else if(into.sticker)into.sticker={...into.sticker,[field]:from.sticker[field]};next.keys[id]=into;}
  else if(property.startsWith('shadow.')){const field=property.slice(7);if(!Object.hasOwn(shadow(side),field))throw Error('阴影属性不合适');const a={...shadow(side),...(from.shadows[side]||{})},b={...shadow(side),...(into.shadows[side]||{})};b[field]=a[field];into.shadows={...into.shadows,[side]:b};next.keys[id]=into;}
  else {if(!Object.hasOwn(style(),property))throw Error('属性不合适');into[property]=property==='cornerRadius'?(from[property]??p.cornerRadius):clone(from[property]);next.keys[id]=into;}
 }return valid(next);
}

export function moveSticker(value,dx,dy,zoom,turn){if(![dx,dy,zoom,turn].every(Number.isFinite)||zoom<=0)return value;return {...value,offsetX:Math.max(-2,Math.min(2,value.offsetX+dx)),offsetY:Math.max(-2,Math.min(2,value.offsetY+dy)),scale:Math.max(.25,Math.min(12,value.scale*zoom)),rotation:((value.rotation+turn+180)%360+360)%360-180};}

export function moveLabel(value,dx,dy){if(![dx,dy].every(Number.isFinite))return value;return {...value,labelOffsetX:Math.max(-.4,Math.min(.4,(value.labelOffsetX??0)+dx)),labelOffsetY:Math.max(-.4,Math.min(.4,(value.labelOffsetY??0)+dy))};}

export function resizeLabel(value,zoom){if(!Number.isFinite(zoom)||zoom<=0||Math.abs(zoom-1)<.0001)return value;return {...value,labelFontSize:Math.max(2,Math.min(32,(value.labelFontSize??17)*zoom))};}

export function moveHint(value,dx,dy){if(![dx,dy].every(Number.isFinite))return value;return {...value,hintOffsetX:Math.max(-.8,Math.min(.8,(value.hintOffsetX??0)+dx)),hintOffsetY:Math.max(-.8,Math.min(.8,(value.hintOffsetY??0)+dy))};}
export function resizeHint(value,zoom){if(!Number.isFinite(zoom)||zoom<=0||Math.abs(zoom-1)<.0001)return value;return {...value,hintFontSize:Math.max(2,Math.min(24,(value.hintFontSize??9)*zoom))};}

export function withGlobalRadius(p,value){return valid({...p,cornerRadius:value,panelStyle:p.panelStyle?{...p.panelStyle,cornerRadius:null}:p.panelStyle,keys:Object.fromEntries(Object.entries(p.keys).map(([id,s])=>[id,{...s,cornerRadius:null}]))})}

export function moveWallpaper(w,dx,dy,zoom){if(![dx,dy,zoom].every(Number.isFinite)||zoom<=0)return w;return {...w,offsetX:Math.max(-1,Math.min(1,(w.offsetX??0)+dx)),offsetY:Math.max(-1,Math.min(1,(w.offsetY??0)+dy)),scale:Math.max(1,Math.min(4,(w.scale??1)*zoom))}}
export function wallpaperRect(iw,ih,w,h,p){const scale=Math.max(w/iw,h/ih)*(p.scale??1),width=iw*scale,height=ih*scale;return {x:(w-width)/2+(p.offsetX??0)*w,y:(h-height)/2+(p.offsetY??0)*h,width,height}}

export function withPanelStyle(p,source,functionKey=false){const s={...style(),...(p.keys[source]||{})};return valid({...p,panelStyle:{...clone(s),backgroundLight:s.backgroundLight??(functionKey?p.light.special:p.light.key),backgroundDark:s.backgroundDark??(functionKey?p.dark.special:p.dark.key),textLight:s.textLight??p.light.text,textDark:s.textDark??p.dark.text,cornerRadius:s.cornerRadius??p.cornerRadius}})}

export function faceRect(shape,w,h){if(shape==='ROUNDED'||w>=h*1.6)return {left:0,top:0,width:w,height:h};const edge=Math.min(w,h);return {left:(w-edge)/2,top:(h-edge)/2,width:edge,height:edge}}
export function faceClip(shape,w,h,radius=0){const b=faceRect(shape,w,h);return shape==='CIRCLE'?`inset(${b.top}px ${w-b.left-b.width}px ${h-b.top-b.height}px ${b.left}px round ${Math.min(b.width,b.height)/2}px)`:`inset(${b.top}px ${w-b.left-b.width}px ${h-b.top-b.height}px ${b.left}px round ${shape==='SQUARE'?0:radius}px)`}
