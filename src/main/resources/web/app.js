let equipamentos = [];
let reservas = [];
const $ = (selector) => document.querySelector(selector);
const nomesTurno = {MANHA:'Manhã', TARDE:'Tarde', NOITE:'Noite'};
let mensagemTimer;
function hojeLocal() {
  const d=new Date();
  return [d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
}

async function api(path, options={}) {
  const resposta = await fetch(path, options);
  const dados = await resposta.json();
  if (!resposta.ok) throw new Error(dados.erro || 'Falha na comunicação com o servidor.');
  return dados;
}
function mensagem(texto, erro=false) {
  const t = $('#toast'); t.textContent = texto; t.classList.toggle('error', erro);
  t.style.display = 'block'; clearTimeout(mensagemTimer);
  mensagemTimer = setTimeout(() => t.style.display = 'none', 4500);
}
function elemento(tag, classe, texto) {
  const el=document.createElement(tag); if (classe) el.className=classe;
  if (texto !== undefined) el.textContent=texto; return el;
}
function selecionar(id) {
  $('#equipamento').value=id;
  document.querySelectorAll('.equipment').forEach(el=>el.classList.toggle('selected',el.dataset.id===id));
}
async function carregar() {
  try {
    [equipamentos, reservas] = await Promise.all([api('/api/equipamentos'), api('/api/reservas')]);
    renderizar();
  } catch (err) { mensagem(err.message,true); }
}
function renderizar() {
  $('#nEquip').textContent=equipamentos.length;
  $('#nAtivas').textContent=reservas.filter(r=>r.status==='ATIVA').length;
  $('#nCanceladas').textContent=reservas.filter(r=>r.status==='CANCELADA').length;
  $('.softtag').textContent=`${equipamentos.length} itens`;
  const cards=$('#equipamentos'); cards.replaceChildren();
  const select=$('#equipamento'); const atual=select.value; select.replaceChildren(elemento('option','', 'Selecione'));
  select.firstChild.value='';
  for(const eq of equipamentos) {
    const opt=elemento('option','',eq.nome);opt.value=eq.id;select.appendChild(opt);
    const card=elemento('div','equipment');card.dataset.id=eq.id;card.tabIndex=0;card.setAttribute('role','button');
    card.appendChild(elemento('span','equip-icon',eq.simbolo));
    const info=elemento('div');info.appendChild(elemento('h3','',eq.nome));info.appendChild(elemento('p','',eq.categoria));card.appendChild(info);
    card.onclick=()=>selecionar(eq.id);
    card.onkeydown=(ev)=>{if(ev.key==='Enter'||ev.key===' ') {ev.preventDefault();selecionar(eq.id);}};
    cards.appendChild(card);
  }
  if(equipamentos.some(x=>x.id===atual)) selecionar(atual);
  const lista=$('#reservas');lista.replaceChildren();
  if(!reservas.length) { lista.appendChild(elemento('div','empty','Nenhuma reserva registrada. Crie a primeira!'));return; }
  const nomes=Object.fromEntries(equipamentos.map(x=>[x.id,x.nome]));
  for(const r of reservas) {
    const row=elemento('div','reservation');
    const left=elemento('div');left.appendChild(elemento('div','res-title',nomes[r.equipamentoId]||r.equipamentoId));
    const meta=elemento('div','res-meta',`${r.responsavel} · ${r.data.split('-').reverse().join('/')} · ${nomesTurno[r.turno]||r.turno}`);
    left.appendChild(meta); row.appendChild(left);
    const right=elemento('div','res-right');right.appendChild(elemento('span',`status ${r.status==='ATIVA'?'active':'cancel'}`,r.status==='ATIVA'?'ATIVA':'CANCELADA'));
    if(r.status==='ATIVA') {
      const button=elemento('button','cancel-btn','Cancelar');button.type='button';
      button.onclick=async()=>{if(!confirm('Cancelar esta reserva?'))return;
        try {await api('/api/cancelar',{method:'POST',body:new URLSearchParams({id:String(r.id)})});mensagem('Reserva cancelada.');await carregar();}catch(err){mensagem(err.message,true);}};
      right.appendChild(button);
    }
    row.appendChild(right);lista.appendChild(row);
  }
}
$('#formReserva').addEventListener('submit',async(e)=>{
  e.preventDefault(); const f=e.currentTarget;
  try {await api('/api/reservas',{method:'POST',body:new URLSearchParams(new FormData(f))});
    mensagem('Reserva criada com sucesso!');f.reset();$('#data').value=hojeLocal();await carregar();}
  catch(err){mensagem(err.message,true);}
});
$('#atualizar').onclick=carregar;
$('#data').min=hojeLocal();
$('#data').value=$('#data').min;
carregar();
