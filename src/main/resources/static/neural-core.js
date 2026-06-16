/* ── J.A.R.V.I.S. // OS — Neural Core ─────────────────────────────────────
   A holographic, rotating node-sphere ("neural map") rendered to <canvas>.
   Nodes are distributed on a sphere (Fibonacci spiral), wired to their nearest
   neighbours, and lit by depth so the front of the globe glows brighter than
   the back. Concentric HUD rings, a pulsing core, traveling data-pulses and
   expanding ripples react to the assistant's state.

   Usage:
     const core = new NeuralCore(canvasEl);
     core.setState('idle' | 'listening' | 'thinking' | 'speaking');
─────────────────────────────────────────────────────────────────────────── */

class NeuralCore {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');

    // Visual energy: lerps toward a target driven by state.
    this.energy = 0.25;
    this.targetEnergy = 0.25;
    this.state = 'idle';

    this.angle = 0;          // yaw (spin)
    this.tilt = -0.42;       // fixed X-tilt so we see the sphere from above
    this.pulses = [];        // data dots traveling along edges
    this.ripples = [];       // expanding rings emitted on activity
    this.rippleTimer = 0;
    this.pulseTimer = 0;
    this.lastT = performance.now();

    this._buildSphere(150);
    this._resize();

    this._ro = new ResizeObserver(() => this._resize());
    this._ro.observe(canvas.parentElement || canvas);

    this._loop = this._loop.bind(this);
    requestAnimationFrame(this._loop);
  }

  // ── Geometry ──────────────────────────────────────────────────────────
  _buildSphere(count) {
    this.nodes = [];
    const golden = Math.PI * (3 - Math.sqrt(5));
    for (let i = 0; i < count; i++) {
      const y = 1 - (i / (count - 1)) * 2;       // 1 → -1
      const r = Math.sqrt(1 - y * y);
      const theta = golden * i;
      this.nodes.push({
        x: Math.cos(theta) * r,
        y,
        z: Math.sin(theta) * r,
        flick: Math.random() * Math.PI * 2,      // per-node twinkle phase
      });
    }

    // Edges: connect each node to its few nearest neighbours (short arcs only).
    this.edges = [];
    const seen = new Set();
    for (let i = 0; i < count; i++) {
      const a = this.nodes[i];
      const dists = [];
      for (let j = 0; j < count; j++) {
        if (i === j) continue;
        const b = this.nodes[j];
        const dot = a.x * b.x + a.y * b.y + a.z * b.z; // higher = closer on sphere
        dists.push([dot, j]);
      }
      dists.sort((p, q) => q[0] - p[0]);
      const k = 3;
      for (let n = 0; n < k; n++) {
        const j = dists[n][1];
        const key = i < j ? `${i}_${j}` : `${j}_${i}`;
        if (seen.has(key)) continue;
        seen.add(key);
        this.edges.push([i, j]);
      }
    }
  }

  _resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const box = (this.canvas.parentElement || this.canvas).getBoundingClientRect();
    const w = Math.max(1, box.width);
    const h = Math.max(1, box.height);
    this.canvas.width = w * dpr;
    this.canvas.height = h * dpr;
    this.canvas.style.width = w + 'px';
    this.canvas.style.height = h + 'px';
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    this.W = w;
    this.H = h;
    this.cx = w / 2;
    this.cy = h / 2;
    this.R = Math.min(w, h) * 0.30;   // sphere radius in px
    this.focal = this.R * 3.2;        // perspective depth
  }

  // ── State ─────────────────────────────────────────────────────────────
  setState(state) {
    this.state = state;
    this.targetEnergy = ({
      idle: 0.25, listening: 0.7, thinking: 1.0, speaking: 0.85,
    })[state] ?? 0.25;
  }

  // ── Projection ────────────────────────────────────────────────────────
  _project(n) {
    // Rotate around Y (yaw) then X (tilt).
    const ca = Math.cos(this.angle), sa = Math.sin(this.angle);
    let x = n.x * ca - n.z * sa;
    let z = n.x * sa + n.z * ca;
    let y = n.y;
    const ct = Math.cos(this.tilt), st = Math.sin(this.tilt);
    const y2 = y * ct - z * st;
    const z2 = y * st + z * ct;
    const scale = this.focal / (this.focal - z2 * this.R);
    return {
      sx: this.cx + x * this.R * scale,
      sy: this.cy + y2 * this.R * scale,
      depth: (z2 + 1) / 2,   // 0 (back) → 1 (front)
      scale,
    };
  }

  // ── Animation loop ────────────────────────────────────────────────────
  _loop(t) {
    const dt = Math.min(0.05, (t - this.lastT) / 1000);
    this.lastT = t;

    this.energy += (this.targetEnergy - this.energy) * Math.min(1, dt * 3);
    this.angle += dt * (0.12 + this.energy * 0.5);

    this._spawn(dt, t);
    this._draw(t);
    requestAnimationFrame(this._loop);
  }

  _spawn(dt, t) {
    // Data pulses along edges — more frequent with energy.
    this.pulseTimer -= dt;
    const rate = 0.45 - this.energy * 0.38;
    if (this.pulseTimer <= 0 && this.edges.length) {
      this.pulseTimer = Math.max(0.03, rate);
      const burst = this.state === 'thinking' ? 3 : 1;
      for (let b = 0; b < burst; b++) {
        this.pulses.push({ edge: (Math.random() * this.edges.length) | 0, p: 0, speed: 0.6 + Math.random() * 1.2 });
      }
    }
    this.pulses.forEach(p => (p.p += dt * p.speed));
    this.pulses = this.pulses.filter(p => p.p < 1);

    // Ripples — only when active.
    this.rippleTimer -= dt;
    const wantRipple = this.state === 'listening' || this.state === 'speaking';
    if (wantRipple && this.rippleTimer <= 0) {
      this.rippleTimer = this.state === 'listening' ? 1.1 : 0.7;
      this.ripples.push({ r: this.R * 0.6, a: 0.5 });
    }
    this.ripples.forEach(r => { r.r += dt * this.R * 1.4; r.a -= dt * 0.45; });
    this.ripples = this.ripples.filter(r => r.a > 0);
  }

  // ── Render ────────────────────────────────────────────────────────────
  _draw(t) {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, this.W, this.H);

    const cyan = (a) => `rgba(0, 242, 255, ${a})`;
    const proj = this.nodes.map(n => this._project(n));

    // Background glow
    const bg = ctx.createRadialGradient(this.cx, this.cy, 0, this.cx, this.cy, this.R * 2.2);
    bg.addColorStop(0, cyan(0.06 + this.energy * 0.05));
    bg.addColorStop(0.5, cyan(0.02));
    bg.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, this.W, this.H);

    this._drawRings(t, cyan);

    // Ripples
    this.ripples.forEach(r => {
      ctx.beginPath();
      ctx.arc(this.cx, this.cy, r.r, 0, Math.PI * 2);
      ctx.strokeStyle = cyan(Math.max(0, r.a));
      ctx.lineWidth = 1;
      ctx.stroke();
    });

    // Edges
    ctx.lineWidth = 0.7;
    this.edges.forEach(([i, j]) => {
      const a = proj[i], b = proj[j];
      const depth = (a.depth + b.depth) / 2;
      ctx.strokeStyle = cyan((0.04 + depth * 0.22) * (0.4 + this.energy * 0.6));
      ctx.beginPath();
      ctx.moveTo(a.sx, a.sy);
      ctx.lineTo(b.sx, b.sy);
      ctx.stroke();
    });

    // Pulses
    this.pulses.forEach(p => {
      const [i, j] = this.edges[p.edge];
      const a = proj[i], b = proj[j];
      const x = a.sx + (b.sx - a.sx) * p.p;
      const y = a.sy + (b.sy - a.sy) * p.p;
      const fade = Math.sin(p.p * Math.PI);
      ctx.beginPath();
      ctx.arc(x, y, 1.6, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(180, 250, 255, ${fade})`;
      ctx.shadowColor = cyan(0.9);
      ctx.shadowBlur = 8;
      ctx.fill();
      ctx.shadowBlur = 0;
    });

    // Nodes
    proj.forEach((p, idx) => {
      const twinkle = 0.7 + 0.3 * Math.sin(t * 0.003 + this.nodes[idx].flick);
      const r = (0.7 + p.depth * 1.8) * p.scale * twinkle;
      ctx.beginPath();
      ctx.arc(p.sx, p.sy, r, 0, Math.PI * 2);
      ctx.fillStyle = cyan((0.2 + p.depth * 0.8) * (0.6 + this.energy * 0.4));
      ctx.fill();
    });

    this._drawCore(t, cyan);
  }

  _drawRings(t, cyan) {
    const ctx = this.ctx;
    const ringDefs = [
      { r: 1.18, dash: [],       w: 1,   a: 0.30, spin: 0.10 },
      { r: 1.30, dash: [2, 10],  w: 1,   a: 0.40, spin: -0.18 },
      { r: 1.55, dash: [40, 24], w: 1.5, a: 0.35, spin: 0.07 },
    ];
    ringDefs.forEach(rd => {
      const rad = this.R * rd.r;
      ctx.save();
      ctx.translate(this.cx, this.cy);
      ctx.rotate(t * 0.001 * rd.spin * (1 + this.energy));
      ctx.beginPath();
      ctx.ellipse(0, 0, rad, rad * 0.94, 0, 0, Math.PI * 2);
      ctx.setLineDash(rd.dash);
      ctx.lineWidth = rd.w;
      ctx.strokeStyle = cyan(rd.a * (0.5 + this.energy * 0.5));
      ctx.stroke();
      ctx.restore();
    });
    ctx.setLineDash([]);

    // Tick marks around the outer ring
    const ticks = 60;
    ctx.save();
    ctx.translate(this.cx, this.cy);
    ctx.rotate(t * 0.00007);
    for (let i = 0; i < ticks; i++) {
      const ang = (i / ticks) * Math.PI * 2;
      const r1 = this.R * 1.62, r2 = this.R * (i % 5 === 0 ? 1.70 : 1.66);
      ctx.beginPath();
      ctx.moveTo(Math.cos(ang) * r1, Math.sin(ang) * r1 * 0.94);
      ctx.lineTo(Math.cos(ang) * r2, Math.sin(ang) * r2 * 0.94);
      ctx.strokeStyle = cyan(0.25);
      ctx.lineWidth = 1;
      ctx.stroke();
    }
    ctx.restore();
  }

  _drawCore(t, cyan) {
    const ctx = this.ctx;
    const pulse = 0.6 + 0.4 * Math.sin(t * 0.004);
    const r = this.R * (0.05 + this.energy * 0.03) * (0.8 + pulse * 0.4);
    const g = ctx.createRadialGradient(this.cx, this.cy, 0, this.cx, this.cy, r * 4);
    g.addColorStop(0, cyan(0.9 * pulse));
    g.addColorStop(0.4, cyan(0.3));
    g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(this.cx, this.cy, r * 4, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.arc(this.cx, this.cy, r, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(200, 250, 255, 0.95)';
    ctx.fill();
  }
}

window.NeuralCore = NeuralCore;
