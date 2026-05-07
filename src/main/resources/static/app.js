/* ── J.A.R.V.I.S. // OS — app.js ──────────────────────────────────────── */

// ═══════════════════════════════════════════════════════════════════
//  DOM REFS
// ═══════════════════════════════════════════════════════════════════
const micBtn          = document.getElementById('mic-btn');
const textInput       = document.getElementById('text-input');
const sendBtn         = document.getElementById('send-btn');
const statusDot       = document.getElementById('status-dot');
const statusLabel     = document.getElementById('status-label');
const dataStream      = document.getElementById('data-stream');
const waveform        = document.getElementById('waveform');
const waveLabel       = document.getElementById('wave-label');
const voiceDb         = document.getElementById('voice-db');
const targetCoords    = document.getElementById('target-coords');
const terminalOverlay = document.getElementById('terminal');
const termBody        = document.getElementById('term-body');
const termInput       = document.getElementById('term-input');

// ═══════════════════════════════════════════════════════════════════
//  TAB SWITCHING — keeps topbar tabs + sidebar nav in sync
// ═══════════════════════════════════════════════════════════════════
function activateTab(name) {
  document.querySelectorAll('.ttab').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.snav-item').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab-panel').forEach(p => {
    const isActive = p.id === `tab-${name}`;
    p.classList.toggle('hidden', !isActive);
    if (isActive) p.classList.add('fade-in');
  });
  if (name === 'analytics') initAnalyticsCharts();
}

document.querySelectorAll('.ttab, .snav-item').forEach(btn => {
  btn.addEventListener('click', () => activateTab(btn.dataset.tab));
});

// ═══════════════════════════════════════════════════════════════════
//  TERMINAL TOGGLE
// ═══════════════════════════════════════════════════════════════════
document.getElementById('terminal-toggle').addEventListener('click', () => {
  terminalOverlay.classList.toggle('hidden');
  if (!terminalOverlay.classList.contains('hidden')) termInput.focus();
});
document.getElementById('close-terminal').addEventListener('click', () => {
  terminalOverlay.classList.add('hidden');
});

termInput.addEventListener('keydown', e => {
  if (e.key !== 'Enter') return;
  const cmd = termInput.value.trim();
  if (!cmd) return;
  addTermLine('JARVIS> ' + cmd, 'cmd');
  termInput.value = '';
  sendCommand(cmd, true);
});

function addTermLine(text, type = 'out') {
  const div = document.createElement('div');
  div.className = `term-line term-line--${type}`;
  div.textContent = text;
  termBody.appendChild(div);
  termBody.scrollTop = termBody.scrollHeight;
}

// ═══════════════════════════════════════════════════════════════════
//  STATUS HELPERS
// ═══════════════════════════════════════════════════════════════════
function setStatus(state) {
  statusDot.className = `ss-dot ss-dot--${state === 'busy' ? 'yellow' : state === 'offline' ? 'red' : 'green'}`;
  statusLabel.textContent = state.toUpperCase();
  document.getElementById('sys-status').textContent = state === 'offline' ? 'OFFLINE' : 'ONLINE';
}

// ═══════════════════════════════════════════════════════════════════
//  DATA STREAM LOG (core tab right panel)
// ═══════════════════════════════════════════════════════════════════
function addStreamEntry(text, type = 'system') {
  const now  = new Date();
  const ts   = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`;
  const wrap = document.createElement('div');
  wrap.className = `ds-entry ds-entry--${type} fade-in`;
  wrap.innerHTML = `<span class="ds-time">${ts}</span><span class="ds-text">${text}</span>`;
  dataStream.appendChild(wrap);
  dataStream.scrollTop = dataStream.scrollHeight;

  // Mirror to analytics log
  const al = document.getElementById('analytics-log');
  if (al) {
    const entry = document.createElement('div');
    entry.className = 'lsp-entry fade-in';
    entry.innerHTML = `<span class="lsp-ts">[${ts}]</span><span class="lsp-msg">${text}</span>`;
    al.appendChild(entry);
    al.scrollTop = al.scrollHeight;
  }
}

// ═══════════════════════════════════════════════════════════════════
//  WEBSOCKET (STOMP over SockJS)
// ═══════════════════════════════════════════════════════════════════
let stompClient = null;

function connectWS() {
  try {
    const socket = new SockJS('/ws');
    stompClient  = Stomp.over(socket);
    stompClient.debug = () => {};

    stompClient.connect({}, () => {
      stompClient.subscribe('/topic/jarvis', msg => {
        const { type, content } = JSON.parse(msg.body);
        if (type === 'thinking') {
          setStatus('busy');
          waveform.classList.add('listening');
          waveLabel.textContent = 'PROCESSING';
        }
        if (type === 'response') {
          setStatus('online');
          addStreamEntry('JARVIS: ' + content, 'jarvis');
          addTermLine('JARVIS: ' + content, 'out');
          stopListening();
        }
      });
    }, () => setTimeout(connectWS, 3000));
  } catch (_) {}
}

connectWS();

// ═══════════════════════════════════════════════════════════════════
//  SEND COMMAND
// ═══════════════════════════════════════════════════════════════════
async function sendCommand(text, fromTerminal = false) {
  if (!text.trim()) return;
  if (!fromTerminal) addStreamEntry('> ' + text, 'user');
  setStatus('busy');

  try {
    const res  = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    });
    const data = await res.json();
    setStatus('online');

    if (data.error) {
      addStreamEntry(data.error, 'error');
      addTermLine('ERROR: ' + data.error, 'error');
    } else if (!stompClient?.connected) {
      addStreamEntry('JARVIS: ' + data.response, 'jarvis');
      addTermLine('JARVIS: ' + data.response, 'out');
    }
  } catch (err) {
    addStreamEntry('Connection error: ' + err.message, 'error');
    addTermLine('Connection error: ' + err.message, 'error');
    setStatus('offline');
  } finally {
    stopListening();
  }
}

sendBtn.addEventListener('click', () => {
  const t = textInput.value.trim();
  if (t) { sendCommand(t); textInput.value = ''; }
});

textInput.addEventListener('keydown', e => {
  if (e.key === 'Enter') sendBtn.click();
});

// ═══════════════════════════════════════════════════════════════════
//  MICROPHONE — Web Speech API
// ═══════════════════════════════════════════════════════════════════
const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
let listening   = false;

function startListening() {
  waveform.classList.add('listening');
  waveLabel.textContent = 'LISTENING';
  micBtn.classList.add('listening');
  listening = true;
  updateVoiceLevel();
}

function stopListening() {
  waveform.classList.remove('listening');
  waveLabel.textContent = 'STANDBY';
  micBtn.classList.remove('listening');
  listening = false;
  voiceDb.textContent = '—';
  if (voiceLevelTimer) { clearInterval(voiceLevelTimer); voiceLevelTimer = null; }
}

let voiceLevelTimer = null;
function updateVoiceLevel() {
  voiceLevelTimer = setInterval(() => {
    if (!listening) return;
    const db = (-60 + Math.random() * 30).toFixed(1);
    voiceDb.textContent = db;
  }, 300);
}

// Animate mouse coords in listening zone as target lock
document.querySelector('.listening-zone')?.addEventListener('mousemove', e => {
  const rect = e.currentTarget.getBoundingClientRect();
  const x    = (e.clientX - rect.left).toFixed(2);
  const y    = (e.clientY - rect.top).toFixed(2);
  targetCoords.textContent = `TARGET_LOCK // X: ${x} // Y: ${y}`;
});

if (SpeechRec) {
  recognition               = new SpeechRec();
  recognition.lang          = 'en-US';
  recognition.continuous    = false;
  recognition.interimResults = false;

  recognition.onresult = e => {
    const text = e.results[0][0].transcript;
    addStreamEntry('> ' + text, 'user');
    sendCommand(text);
  };
  recognition.onend  = stopListening;
  recognition.onerror = err => {
    addStreamEntry('Mic error: ' + err.error, 'error');
    stopListening();
  };

  micBtn.addEventListener('click', () => {
    if (listening) {
      recognition.stop();
      stopListening();
    } else {
      recognition.start();
      startListening();
      addStreamEntry('Listening for voice command…', 'system');
    }
  });

  // Clicking the waveform circle also toggles mic
  waveform.addEventListener('click', () => micBtn.click());

} else {
  micBtn.title   = 'Voice input unavailable — use Chrome/Edge';
  micBtn.style.opacity = '0.5';
  addStreamEntry('Voice input unavailable. Use Chrome or Edge.', 'system');
}

// ═══════════════════════════════════════════════════════════════════
//  CPU BAR ANIMATION (core tab)
// ═══════════════════════════════════════════════════════════════════
function animateCpuBars() {
  const bars = document.querySelectorAll('#cpu-bars span');
  const base = parseFloat(document.getElementById('cpu-value').textContent) || 25;
  bars.forEach(b => {
    const h = Math.max(4, Math.min(24, base * 0.24 * (0.5 + Math.random())));
    b.style.height = h + 'px';
  });
}
setInterval(animateCpuBars, 1200);
animateCpuBars();

// Simulate slow CPU/MEM drift
setInterval(() => {
  const cpu = (20 + Math.random() * 15).toFixed(1);
  document.getElementById('cpu-value').textContent = cpu + '%';
  const mem = (10 + Math.random() * 4).toFixed(1);
  document.getElementById('mem-value').textContent = mem + ' GB';
}, 4000);

// ═══════════════════════════════════════════════════════════════════
//  UPTIME COUNTER (analytics tab)
// ═══════════════════════════════════════════════════════════════════
let startTime = Date.now();
function updateUptime() {
  const el = document.getElementById('uptime-val');
  if (!el) return;
  const diff = Math.floor((Date.now() - startTime) / 1000);
  const d    = Math.floor(diff / 86400);
  const h    = Math.floor((diff % 86400) / 3600);
  const m    = Math.floor((diff % 3600) / 60);
  const s    = diff % 60;
  el.textContent = `${d}D ${String(h).padStart(2,'0')}H ${String(m).padStart(2,'0')}M ${String(s).padStart(2,'0')}S`;
}
setInterval(updateUptime, 1000);
updateUptime();

// ═══════════════════════════════════════════════════════════════════
//  ANALYTICS CHARTS (Chart.js) — only init once
// ═══════════════════════════════════════════════════════════════════
let fluxChart     = null;
let fluxChartData = [];

function initAnalyticsCharts() {
  if (fluxChart) return;

  const canvas = document.getElementById('flux-chart');
  if (!canvas || typeof Chart === 'undefined') return;

  // Seed data
  for (let i = 0; i < 40; i++) {
    fluxChartData.push(200 + Math.sin(i * 0.3) * 80 + Math.random() * 60 - 30);
  }

  const labels = fluxChartData.map((_, i) => i);
  const ctx    = canvas.getContext('2d');

  const gradient = ctx.createLinearGradient(0, 0, 0, 100);
  gradient.addColorStop(0, 'rgba(0, 242, 255, 0.25)');
  gradient.addColorStop(1, 'rgba(0, 242, 255, 0)');

  fluxChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        data:            fluxChartData,
        borderColor:     '#00f2ff',
        borderWidth:     1.5,
        backgroundColor: gradient,
        pointRadius:     0,
        tension:         0.4,
        fill:            true
      }]
    },
    options: {
      responsive:          true,
      maintainAspectRatio: false,
      animation:           { duration: 300 },
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
      scales: {
        x: { display: false },
        y: {
          display:  true,
          position: 'left',
          grid: { color: 'rgba(15, 37, 64, 0.8)', drawBorder: false },
          ticks: {
            color:     '#2a4060',
            font:      { family: 'Space Grotesk', size: 9 },
            maxTicksLimit: 4
          }
        }
      }
    }
  });

  // Live update
  setInterval(() => {
    if (!fluxChart) return;
    const last = fluxChartData[fluxChartData.length - 1];
    const next = Math.max(80, Math.min(500, last + (Math.random() - 0.48) * 30));
    fluxChartData.shift();
    fluxChartData.push(next);
    fluxChart.data.datasets[0].data = [...fluxChartData];
    fluxChart.update('none');
  }, 800);
}

// ═══════════════════════════════════════════════════════════════════
//  SIMULATED SYSTEM LOG STREAM
// ═══════════════════════════════════════════════════════════════════
const sysMessages = [
  'Satellite uplink re-established. Signal strength: HIGH.',
  'Minor power fluctuation detected in East Wing relay.',
  'Personal assistant core transitioned to standby mode.',
  'Thermal management active. Core temp stabilized at 34°C.',
  'Voice command registered. Processing "initialize morning routine"...',
  'Neural load at 74%. All subsystems nominal.',
  'Secure tunnel established // AES-256 handshake complete.',
  'Mapping data nodes: 34,821 detected.',
  'Latency spike detected in SECTOR_70. Auto-rerouting traffic.',
  'Optimization complete: 14ms average latency.',
  'Snapshot captured // Analytics v4.',
  'IDLE_DAEMON listening...',
  'SCTR-04 flux index updated. Δ +2.1%.',
  'Deep core relay signal nominal at 55%.',
  'Orbital Station 04 handshake confirmed. Uplink 88%.',
];
let msgIdx = 0;

function pushSysMessage() {
  addStreamEntry(sysMessages[msgIdx % sysMessages.length], 'system');
  msgIdx++;
}

// Stagger first messages on load
[2000, 4500, 7000, 10000].forEach(t => setTimeout(pushSysMessage, t));
// Then every 12s
setInterval(pushSysMessage, 12000);

// ═══════════════════════════════════════════════════════════════════
//  WEATHER — try /api/weather, fallback to dummy data
// ═══════════════════════════════════════════════════════════════════
async function fetchWeather() {
  try {
    const res  = await fetch('/api/weather');
    const data = await res.json();
    document.getElementById('w-temp').textContent = data.temp      || '21°C';
    document.getElementById('w-wind').textContent = data.wind      || '14km/h';
    document.getElementById('w-hum').textContent  = data.humidity  || '42%';
    document.getElementById('w-pres').textContent = data.pressure  || '1012hPa';
    document.getElementById('w-condition').textContent = '◎ ' + (data.condition || 'OVERCAST_SKIES');
  } catch (_) {
    // fallback
    document.getElementById('w-temp').textContent = '21°C';
    document.getElementById('w-wind').textContent = '14km/h';
    document.getElementById('w-hum').textContent  = '42%';
    document.getElementById('w-pres').textContent = '1012hPa';
    document.getElementById('w-condition').textContent = '◎ OVERCAST_SKIES';
  }
}
fetchWeather();

// ═══════════════════════════════════════════════════════════════════
//  STARTUP — ping backend status
// ═══════════════════════════════════════════════════════════════════
fetch('/api/status')
  .then(r => r.json())
  .then(d => {
    addStreamEntry(`${d.assistant || 'JARVIS'} online — ${d.model || 'AI'} · ${d.stt || 'STT'} · ${d.tts || 'TTS'}`, 'system');
    addTermLine('J.A.R.V.I.S. OS — All systems nominal.', 'system');
    addTermLine('Type a command below or use the voice interface.', 'system');
  })
  .catch(() => {
    addStreamEntry('Backend offline — start the Spring Boot app.', 'error');
    setStatus('offline');
    addTermLine('Backend offline. Start the Spring Boot application.', 'error');
  });
