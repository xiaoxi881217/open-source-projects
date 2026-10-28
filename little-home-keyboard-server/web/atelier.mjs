import {viewSettings,previewScale} from './viewport.mjs?v=20261056';
const $=id=>document.getElementById(id),key='little-home-editor-ui-v1'+(new URLSearchParams(location.search).has('qa')?'-qa'+(new URLSearchParams(location.search).get('case')?'-'+new URLSearchParams(location.search).get('case').replace(/[^a-zA-Z0-9_-]/g,'').slice(0,48):''):'');
let settings=viewSettings();try{settings=viewSettings(JSON.parse(localStorage.getItem(key)||'{}'))}catch{}
let lastScale=0;
function fit(){
 const frame=$('previewWindow'),canvas=$('previewCanvas'),keyboard=$('keyboard');
 if(!frame?.clientWidth||!keyboard)return;
 canvas.style.width=Math.min(frame.clientWidth,430)+'px';
 const scale=previewScale(keyboard.offsetWidth,keyboard.offsetHeight,frame.clientWidth,frame.clientHeight);
 canvas.style.setProperty('--preview-scale',scale);
 if(Math.abs(lastScale-scale)>.0001){lastScale=scale;window.dispatchEvent(new Event('resize'))}
}
function apply(save=false){
 document.documentElement.style.setProperty('--preview-part',settings.percent/100);
 document.body.classList.toggle('workbench-dark',settings.dark);
 document.querySelector('.stage').classList.toggle('is-unpinned',!settings.pinned);
 $('previewPercent').value=settings.percent;$('previewPercentLabel').textContent=settings.percent+'%';
 $('previewThird').setAttribute('aria-pressed',settings.percent===33);$('previewHalf').setAttribute('aria-pressed',settings.percent===50);
 $('pinPreview').checked=settings.pinned;$('workbenchTheme').setAttribute('aria-pressed',settings.dark);
 if(save)try{localStorage.setItem(key,JSON.stringify(settings))}catch{$('status').textContent='界面偏好暂时没保存，装扮仍可继续编辑。'}
 window.dispatchEvent(new Event('atelier-theme-change'));requestAnimationFrame(fit);
}
$('previewPercent').oninput=e=>{settings.percent=Number(e.target.value);apply(true)};
$('previewThird').onclick=()=>{settings.percent=33;apply(true)};$('previewHalf').onclick=()=>{settings.percent=50;apply(true)};
$('pinPreview').onchange=e=>{settings.pinned=e.target.checked;apply(true)};
$('workbenchTheme').onclick=()=>{settings.dark=!settings.dark;apply(true)};
for(const tab of document.querySelectorAll('[data-section]'))tab.onclick=()=>{
 for(const button of document.querySelectorAll('[data-section]'))button.setAttribute('aria-pressed',button===tab);
 for(const panel of document.querySelectorAll('[data-panel]')){panel.hidden=tab.dataset.section!=='all'&&panel.dataset.panel!==tab.dataset.section;if(tab.dataset.section!=='all'&&!panel.hidden)panel.open=true}
};
const observer=new ResizeObserver(()=>requestAnimationFrame(fit));observer.observe($('previewWindow'));observer.observe($('keyboard'));
window.addEventListener('resize',()=>requestAnimationFrame(fit));window.visualViewport?.addEventListener('resize',()=>requestAnimationFrame(fit));
apply();
