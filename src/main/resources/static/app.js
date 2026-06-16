/* ── J.A.R.V.I.S. — app.js ───────────────────────────────────────────────
   Minimal interface: the orb, your voice, its reply.
─────────────────────────────────────────────────────────────────────────── */

// ── DOM refs ────────────────────────────────────────────────────────────
const micBtn      = document.getElementById('mic-btn');
const textInput   = document.getElementById('text-input');
const commandBar  = document.getElementById('command-bar');
const statusDot   = document.getElementById('status-dot');
const statusLabel = document.getElementById('status-label');
const waveLabel   = document.getElementById('wave-label');
const convo       = document.getElementById('convo');
const convoYou    = document.getElementById('convo-you');
const convoJarvis = document.getElementById('convo-jarvis');

// ── Neural core ─────────────────────────────────────────────────────────
const neuralCanvas = document.getElementById('neural-core');
const neuralCore   = (neuralCanvas && window.NeuralCore) ? new NeuralCore(neuralCanvas) : null;
function coreState(s) { if (neuralCore) neuralCore.setState(s); }

// ── Status + core label ─────────────────────────────────────────────────
function setStatus(state) {
  statusDot.className = 'status-dot' + (state === 'busy' ? ' busy' : state === 'offline' ? ' offline' : '');
  statusLabel.textContent = state === 'offline' ? 'OFFLINE' : state === 'busy' ? 'PROCESSING' : 'ONLINE';
}
function setLabel(text, active) {
  waveLabel.textContent = text;
  waveLabel.classList.toggle('active', !!active);
}

// ── Conversation display ────────────────────────────────────────────────
let convoTimer = null;
let typeTimer  = null;

function showYou(text) {
  convoYou.textContent = text ? '“' + text + '”' : '';
  convoJarvis.textContent = '';
  convo.classList.add('show');
  clearTimeout(convoTimer);
}

function showJarvis(text) {
  convo.classList.add('show');
  clearInterval(typeTimer);
  // Typewriter reveal
  let i = 0;
  convoJarvis.innerHTML = '<span class="caret"></span>';
  typeTimer = setInterval(() => {
    i++;
    const shown = text.slice(0, i);
    convoJarvis.innerHTML = shown + (i < text.length ? '<span class="caret"></span>' : '');
    if (i >= text.length) clearInterval(typeTimer);
  }, 18);
  // Auto-fade after a while of no activity
  clearTimeout(convoTimer);
  convoTimer = setTimeout(() => convo.classList.remove('show'), 9000);
}

// ── WebSocket (live thinking / response from backend) ───────────────────
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
          setLabel('PROCESSING', true);
          coreState('thinking');
        }
        if (type === 'response') {
          setStatus('online');
          showJarvis(content);
          setLabel('RESPONDING', true);
          coreState('speaking');
          stopListening(true);
          setTimeout(() => { setLabel('STANDBY', false); coreState('idle'); }, 2600);
        }
      });
    }, () => setTimeout(connectWS, 3000));
  } catch (_) {}
}
connectWS();

// ── Send a command ──────────────────────────────────────────────────────
async function sendCommand(text) {
  text = text.trim();
  if (!text) return;
  showYou(text);
  setStatus('busy');
  setLabel('PROCESSING', true);
  coreState('thinking');

  try {
    const res  = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    });
    const data = await res.json();

    if (data.error) {
      showJarvis(data.error);
    } else if (!stompClient?.connected) {
      // No websocket — show the HTTP response directly
      showJarvis(data.response);
    }
    setStatus('online');
  } catch (err) {
    setStatus('offline');
    showJarvis('Connection lost. Is the JARVIS service running?');
  } finally {
    if (!stompClient?.connected) {
      setLabel('STANDBY', false);
      coreState('idle');
    }
    stopListening(true);
  }
}

commandBar.addEventListener('submit', e => {
  e.preventDefault();
  const t = textInput.value.trim();
  if (t) { sendCommand(t); textInput.value = ''; }
});

// ── Voice (Web Speech API) ──────────────────────────────────────────────
const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
let listening   = false;

function startListening() {
  listening = true;
  micBtn.classList.add('listening');
  setLabel('LISTENING', true);
  coreState('listening');
}
function stopListening(keepLabel) {
  listening = false;
  micBtn.classList.remove('listening');
  if (!keepLabel) { setLabel('STANDBY', false); coreState('idle'); }
}

if (SpeechRec) {
  recognition = new SpeechRec();
  recognition.lang = 'en-US';
  recognition.continuous = false;
  recognition.interimResults = false;

  recognition.onresult = e => sendCommand(e.results[0][0].transcript);
  recognition.onend = () => { if (listening) stopListening(false); };
  recognition.onerror = () => stopListening(false);

  const toggleMic = () => {
    if (listening) { recognition.stop(); stopListening(false); }
    else { recognition.start(); startListening(); }
  };
  micBtn.addEventListener('click', toggleMic);
  neuralCanvas?.addEventListener('click', toggleMic);
} else {
  micBtn.style.opacity = '0.4';
  micBtn.title = 'Voice unavailable — use Chrome or Edge';
}

// ── Startup ─────────────────────────────────────────────────────────────
fetch('/api/status')
  .then(r => r.json())
  .then(() => setStatus('online'))
  .catch(() => setStatus('offline'));
