(()=>{const d=document,q=s=>[...d.querySelectorAll(s)],rm=matchMedia('(prefers-reduced-motion:reduce)').matches;
const tg=d.querySelector('.tg');if(tg){const ic=()=>tg.textContent=d.documentElement.dataset.theme=='dark'?'☀':'☾';ic();tg.onclick=()=>{const t=d.documentElement.dataset.theme=='dark'?'light':'dark';d.documentElement.dataset.theme=t;try{localStorage.setItem('lb',t)}catch(e){}ic()}}
const px=q('[data-speed]'),pin=d.querySelector('.pin'),bar=d.querySelector('.bar');
function frame(){const vh=innerHeight,H=d.documentElement.scrollHeight-vh;bar.style.setProperty('--p',H>0?scrollY/H:0);
if(!rm)px.forEach(e=>{const b=(e.closest('[data-pz]')||e.parentElement).getBoundingClientRect();if(b.bottom<-300||b.top>vh+300)return;e.style.setProperty('--sy',(-(b.top+b.height/2-vh/2)*e.dataset.speed).toFixed(1))});
if(pin){const r=pin.getBoundingClientRect(),p=Math.min(1,Math.max(0,-r.top/(r.height-vh))),n=pin.querySelectorAll('.step').length,i=Math.min(n-1,Math.floor(p*n));
pin.style.setProperty('--pp',p.toFixed(3));pin.querySelectorAll('.step,.phone img,.dots i').forEach((e,k)=>e.classList.toggle('on',k%n==i))}}
let t=0;const tick=()=>{if(!t){t=1;requestAnimationFrame(()=>{t=0;frame()})}};addEventListener('scroll',tick,{passive:true});addEventListener('resize',tick);frame();
q('[data-ptr]').forEach(s=>{s.onpointermove=e=>{const b=s.getBoundingClientRect();s.style.setProperty('--mx',((e.clientX-b.left)/b.width*2-1).toFixed(2));s.style.setProperty('--my',((e.clientY-b.top)/b.height*2-1).toFixed(2))};s.onpointerleave=()=>{s.style.setProperty('--mx',0);s.style.setProperty('--my',0)}});
q('[data-tilt]').forEach(c=>{c.onpointermove=e=>{if(rm)return;const b=c.getBoundingClientRect(),x=(e.clientX-b.left)/b.width-.5,y=(e.clientY-b.top)/b.height-.5;c.style.transform=`perspective(900px) rotateY(${x*10}deg) rotateX(${-y*10}deg) translateY(-4px)`};c.onpointerleave=()=>c.style.transform=''});
const io=new IntersectionObserver(es=>es.forEach(e=>{if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target)}}),{threshold:.15});q('.rv').forEach(e=>io.observe(e));
const links=q('.toc a');if(links.length){const so=new IntersectionObserver(es=>es.forEach(e=>{if(e.isIntersecting)links.forEach(a=>a.classList.toggle('on',a.getAttribute('href')=='#'+e.target.id))}),{rootMargin:'-30% 0px -60% 0px'});q('.sec[id]').forEach(s=>so.observe(s))}
})();
