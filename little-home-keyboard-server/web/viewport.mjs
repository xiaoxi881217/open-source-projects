export function viewSettings(value={}) {
 value=value&&typeof value==='object'&&!Array.isArray(value)?value:{};
 const percent=Number(value.percent);
 return {percent:Number.isFinite(percent)?Math.max(28,Math.min(75,Math.round(percent))):33,pinned:value.pinned!==false,dark:value.dark===true};
}
export function previewScale(width,height,availableWidth,availableHeight) {
 if(![width,height,availableWidth,availableHeight].every(n=>Number.isFinite(n)&&n>0))return 1;
 return Math.min(1,availableWidth/width,availableHeight/height);
}
export function localKeyRect(key,root,logicalWidth,logicalHeight) {
 const x=root.width>0?logicalWidth/root.width:1,y=root.height>0?logicalHeight/root.height:1;
 return {left:(key.left-root.left)*x,top:(key.top-root.top)*y,width:key.width*x,height:key.height*y};
}
