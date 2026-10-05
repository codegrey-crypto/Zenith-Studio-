/**
 * Zenith Studio — Motion Graphics Engine
 * 16:9 60FPS Parametric Motion Video Renderer
 * Inspired by latent-spaces/brag launch video architecture
 */

class ZenithMotionEngine {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    
    // Internal rendering resolution: 1920 x 1080 (16:9 Full HD)
    this.width = 1920;
    this.height = 1080;
    this.canvas.width = this.width;
    this.canvas.height = this.height;

    // Timeline Configuration
    this.fps = 60;
    this.durationSec = 20;
    this.totalFrames = this.fps * this.durationSec; // 1200 frames
    this.currentFrame = 0;
    this.isPlaying = false;
    this.isLooping = true;
    this.playbackRate = 1.0;
    
    // Audio Synthesizer (Zero-dependency Web Audio API soundscape)
    this.audioEnabled = false;
    this.audioCtx = null;
    this.lastTriggeredAudioMarker = -1;

    // Starfield & Grid Points for Background Atmosphere
    this.initBackgroundParticles();

    // Callbacks
    this.onFrameUpdate = null;
    this.onPlayStateChange = null;

    this.lastTimestamp = null;
    this.accumulatedTime = 0;

    // Render initial poster frame
    this.renderFrame(0);
  }

  initBackgroundParticles() {
    this.particles = [];
    for (let i = 0; i < 160; i++) {
      this.particles.push({
        x: Math.random() * this.width,
        y: Math.random() * this.height,
        size: Math.random() * 2 + 0.8,
        speed: Math.random() * 0.4 + 0.1,
        alpha: Math.random() * 0.6 + 0.2
      });
    }
  }

  // --- Playback Control ---

  play() {
    if (this.isPlaying) return;
    this.isPlaying = true;
    this.lastTimestamp = performance.now();
    if (this.audioEnabled && !this.audioCtx) {
      this.initAudio();
    }
    if (this.onPlayStateChange) this.onPlayStateChange(true);
    requestAnimationFrame(this.tick.bind(this));
  }

  pause() {
    if (!this.isPlaying) return;
    this.isPlaying = false;
    if (this.onPlayStateChange) this.onPlayStateChange(false);
  }

  seek(frame) {
    this.currentFrame = Math.max(0, Math.min(this.totalFrames - 1, Math.round(frame)));
    this.renderFrame(this.currentFrame);
    if (this.onFrameUpdate) this.onFrameUpdate(this.currentFrame, this.totalFrames);
  }

  seekNormalized(t) {
    this.seek(t * (this.totalFrames - 1));
  }

  setPlaybackRate(rate) {
    this.playbackRate = rate;
  }

  toggleLoop() {
    this.isLooping = !this.isLooping;
    return this.isLooping;
  }

  toggleAudio() {
    this.audioEnabled = !this.audioEnabled;
    if (this.audioEnabled && !this.audioCtx) {
      this.initAudio();
    }
    return this.audioEnabled;
  }

  initAudio() {
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      this.audioCtx = new AudioContext();
    } catch (e) {
      console.warn("Web Audio not supported or blocked", e);
    }
  }

  playSciFiTone(freq, type = 'sine', duration = 0.5, gainLevel = 0.15) {
    if (!this.audioEnabled || !this.audioCtx) return;
    try {
      const now = this.audioCtx.currentTime;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = type;
      osc.frequency.setValueAtTime(freq, now);
      
      gain.gain.setValueAtTime(gainLevel, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start(now);
      osc.stop(now + duration);
    } catch (e) {}
  }

  tick(timestamp) {
    if (!this.isPlaying) return;

    if (!this.lastTimestamp) this.lastTimestamp = timestamp;
    const delta = (timestamp - this.lastTimestamp) / 1000;
    this.lastTimestamp = timestamp;

    this.accumulatedTime += delta * this.playbackRate;
    const frameDuration = 1 / this.fps;

    while (this.accumulatedTime >= frameDuration) {
      this.currentFrame++;
      this.accumulatedTime -= frameDuration;

      // Handle sound triggers on scene transitions
      this.checkAudioTriggers(this.currentFrame);

      if (this.currentFrame >= this.totalFrames) {
        if (this.isLooping) {
          this.currentFrame = 0;
          this.lastTriggeredAudioMarker = -1;
        } else {
          this.currentFrame = this.totalFrames - 1;
          this.pause();
          break;
        }
      }
    }

    this.renderFrame(this.currentFrame);

    if (this.onFrameUpdate) {
      this.onFrameUpdate(this.currentFrame, this.totalFrames);
    }

    if (this.isPlaying) {
      requestAnimationFrame(this.tick.bind(this));
    }
  }

  checkAudioTriggers(frame) {
    if (!this.audioEnabled) return;

    // Intro Chord
    if (frame === 10 && this.lastTriggeredAudioMarker < 1) {
      this.playSciFiTone(110, 'triangle', 2.0, 0.2); // Low A drone
      setTimeout(() => this.playSciFiTone(220, 'sine', 1.8, 0.15), 100);
      setTimeout(() => this.playSciFiTone(440, 'sine', 1.5, 0.1), 300);
      this.lastTriggeredAudioMarker = 1;
    }
    // Vector Reveal Riser
    else if (frame === 240 && this.lastTriggeredAudioMarker < 2) {
      this.playSciFiTone(330, 'sawtooth', 0.8, 0.08);
      this.playSciFiTone(587.33, 'sine', 1.2, 0.15);
      this.lastTriggeredAudioMarker = 2;
    }
    // 3D GPU Explosion Sub-drop
    else if (frame === 480 && this.lastTriggeredAudioMarker < 3) {
      this.playSciFiTone(80, 'sine', 1.8, 0.35); // Sub punch
      setTimeout(() => this.playSciFiTone(523.25, 'triangle', 1.0, 0.12), 80);
      this.lastTriggeredAudioMarker = 3;
    }
    // Kinetic Typography Stabs
    else if (frame === 780 && this.lastTriggeredAudioMarker < 4) {
      this.playSciFiTone(440, 'square', 0.15, 0.06);
      setTimeout(() => this.playSciFiTone(659.25, 'sine', 0.4, 0.12), 150);
      this.lastTriggeredAudioMarker = 4;
    }
    // Finale Lockup Chime
    else if (frame === 1020 && this.lastTriggeredAudioMarker < 5) {
      this.playSciFiTone(523.25, 'sine', 2.5, 0.2);
      setTimeout(() => this.playSciFiTone(659.25, 'sine', 2.2, 0.18), 120);
      setTimeout(() => this.playSciFiTone(783.99, 'sine', 2.0, 0.15), 240);
      setTimeout(() => this.playSciFiTone(1046.5, 'sine', 2.8, 0.12), 360);
      this.lastTriggeredAudioMarker = 5;
    }
  }

  // --- Mathematical Easing Utilities ---

  easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }

  easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
  }

  easeOutExpo(t) {
    return t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
  }

  easeInOutQuad(t) {
    return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
  }

  clamp(val, min = 0, max = 1) {
    return Math.max(min, Math.min(max, val));
  }

  // --- Master Frame Renderer ---

  renderFrame(frame) {
    const ctx = this.ctx;
    const w = this.width;
    const h = this.height;

    // Clear Base Frame
    ctx.fillStyle = '#06070a';
    ctx.fillRect(0, 0, w, h);

    // Common Ambient Background (Grid & Floating Particles)
    this.drawAtmosphericBackdrop(ctx, frame);

    // Timeline Scene Segmentation
    // 0 - 240   (0s - 4s)  : Scene 1: The Monolith Reveal
    // 240 - 480 (4s - 8s)  : Scene 2: Sub-pixel Vector Precision
    // 480 - 780 (8s - 13s) : Scene 3: 3D Realtime GPU Pipeline & Layer Explosion
    // 780 - 1020(13s - 17s): Scene 4: Kinetic Typography & Spans
    // 1020 - 1200(17s - 20s): Scene 5: Finale Lockup & Call To Action

    if (frame < 240) {
      const progress = frame / 240;
      this.renderScene1(ctx, progress, frame);
    } else if (frame < 480) {
      const progress = (frame - 240) / 240;
      this.renderScene2(ctx, progress, frame);
    } else if (frame < 780) {
      const progress = (frame - 480) / 300;
      this.renderScene3(ctx, progress, frame);
    } else if (frame < 1020) {
      const progress = (frame - 780) / 240;
      this.renderScene4(ctx, progress, frame);
    } else {
      const progress = (frame - 1020) / 180;
      this.renderScene5(ctx, progress, frame);
    }

    // Top Cinematic Vignette & Corner HUD
    this.drawCinematicBorderHUD(ctx, frame);
  }

  // --- Global Atmosphere ---

  drawAtmosphericBackdrop(ctx, frame) {
    const w = this.width;
    const h = this.height;

    // Perspective Neon Horizon Grid
    ctx.save();
    ctx.strokeStyle = 'rgba(0, 242, 254, 0.05)';
    ctx.lineWidth = 1;

    const horizonY = h * 0.72;
    const vpX = w / 2;
    const vpY = horizonY;

    // Vanishing rays
    for (let angle = -Math.PI * 0.45; angle <= Math.PI * 0.45; angle += 0.07) {
      ctx.beginPath();
      ctx.moveTo(vpX, vpY);
      ctx.lineTo(vpX + Math.tan(angle) * h * 0.9, h);
      ctx.stroke();
    }

    // Horizontal receding lines
    const gridSpeed = (frame * 0.6) % 30;
    for (let y = horizonY; y < h; y += 12) {
      const norm = (y - horizonY) / (h - horizonY);
      const curvedY = horizonY + Math.pow(norm, 2.2) * (h - horizonY) + gridSpeed * norm;
      if (curvedY <= h) {
        ctx.strokeStyle = `rgba(127, 0, 255, ${0.03 + norm * 0.09})`;
        ctx.beginPath();
        ctx.moveTo(0, curvedY);
        ctx.lineTo(w, curvedY);
        ctx.stroke();
      }
    }
    ctx.restore();

    // Floating Stardust Particles
    ctx.save();
    this.particles.forEach(p => {
      const currentY = (p.y - frame * p.speed + h) % h;
      ctx.fillStyle = `rgba(200, 220, 255, ${p.alpha * 0.7})`;
      ctx.beginPath();
      ctx.arc(p.x, currentY, p.size, 0, Math.PI * 2);
      ctx.fill();
    });
    ctx.restore();

    // Radial Vignette
    const radGrad = ctx.createRadialGradient(w/2, h/2, 200, w/2, h/2, w * 0.7);
    radGrad.addColorStop(0, 'rgba(0, 242, 254, 0.04)');
    radGrad.addColorStop(0.5, 'rgba(127, 0, 255, 0.02)');
    radGrad.addColorStop(1, 'rgba(6, 7, 10, 0.7)');
    ctx.fillStyle = radGrad;
    ctx.fillRect(0, 0, w, h);
  }

  // --- SCENE 1: The Monolith Reveal (0s - 4s) ---

  renderScene1(ctx, p, frame) {
    const w = this.width;
    const h = this.height;
    const cx = w / 2;
    const cy = h / 2 - 20;

    const easeP = this.easeOutExpo(Math.min(1, p * 1.3));

    // Glowing Prismatic Portal Rings
    ctx.save();
    ctx.translate(cx, cy);

    const rotation = frame * 0.005;
    ctx.rotate(rotation);

    for (let r = 0; r < 3; r++) {
      ctx.beginPath();
      ctx.arc(0, 0, (140 + r * 50) * easeP, 0, Math.PI * 2);
      ctx.strokeStyle = r % 2 === 0 ? 'rgba(0, 242, 254, 0.25)' : 'rgba(127, 0, 255, 0.3)';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([15 + r * 10, 30 + r * 15]);
      ctx.lineDashOffset = -frame * (1 + r * 0.5);
      ctx.stroke();
    }
    ctx.restore();

    // Geometric Zenith "Z" Icon Drawing
    ctx.save();
    ctx.translate(cx, cy);
    const scale = 0.8 + easeP * 0.2;
    ctx.scale(scale, scale);

    // Glowing Diamond Enclosure
    ctx.beginPath();
    const dSize = 130;
    ctx.moveTo(0, -dSize);
    ctx.lineTo(dSize * 1.15, 0);
    ctx.lineTo(0, dSize);
    ctx.lineTo(-dSize * 1.15, 0);
    ctx.closePath();
    ctx.fillStyle = 'rgba(14, 15, 23, 0.85)';
    ctx.fill();
    ctx.lineWidth = 3;
    const borderGrad = ctx.createLinearGradient(-dSize, -dSize, dSize, dSize);
    borderGrad.addColorStop(0, '#00f2fe');
    borderGrad.addColorStop(0.5, '#7f00ff');
    borderGrad.addColorStop(1, '#fa709a');
    ctx.strokeStyle = borderGrad;
    ctx.shadowColor = '#00f2fe';
    ctx.shadowBlur = 24 * easeP;
    ctx.stroke();

    // Inner Stylized "Z" Polyline
    ctx.beginPath();
    ctx.moveTo(-60, -55);
    ctx.lineTo(60, -55);
    ctx.lineTo(-60, 55);
    ctx.lineTo(60, 55);
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 14;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.shadowColor = '#ffffff';
    ctx.shadowBlur = 15;
    ctx.stroke();
    ctx.restore();

    // Typographic Reveal
    ctx.save();
    ctx.textAlign = 'center';

    // Main App Title
    const titleAlpha = this.clamp((p - 0.25) * 2);
    ctx.fillStyle = `rgba(255, 255, 255, ${titleAlpha})`;
    ctx.font = '800 68px "Plus Jakarta Sans", sans-serif';
    ctx.letterSpacing = '6px';
    const textY = cy + 220 + (1 - this.easeOutCubic(titleAlpha)) * 20;
    ctx.fillText("ZENITH STUDIO", cx, textY);

    // Subtitle Tagline
    const subAlpha = this.clamp((p - 0.45) * 2.5);
    ctx.fillStyle = `rgba(148, 163, 184, ${subAlpha})`;
    ctx.font = '500 24px "Plus Jakarta Sans", sans-serif';
    ctx.letterSpacing = '2px';
    ctx.fillText("DESKTOP-CLASS COMPOSITING • IN YOUR POCKET", cx, textY + 45);

    // Tech Badge
    if (subAlpha > 0) {
      const badgeY = textY + 95;
      const bW = 280;
      const bH = 34;
      ctx.fillStyle = 'rgba(0, 242, 254, 0.08)';
      ctx.strokeStyle = 'rgba(0, 242, 254, 0.3)';
      ctx.lineWidth = 1;
      this.drawRoundedRect(ctx, cx - bW/2, badgeY - bH/2, bW, bH, 17);
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = '#00f2fe';
      ctx.font = '600 13px "JetBrains Mono", monospace';
      ctx.fillText("OPENGL + VULKAN SHADER PIPELINE", cx, badgeY + 4);
    }
    ctx.restore();
  }

  // --- SCENE 2: Sub-pixel Vector Precision & Bezier Math (4s - 8s) ---

  renderScene2(ctx, p, frame) {
    const w = this.width;
    const h = this.height;
    const cx = w / 2;
    const cy = h / 2;

    const easeP = this.easeInOutCubic(p);

    // Title Header
    ctx.save();
    ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
    ctx.font = '700 44px "Plus Jakarta Sans", sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText("SUB-PIXEL BEZIER CONTROL", 140, 160);

    ctx.font = '500 20px "Plus Jakarta Sans", sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.fillText("Mathematical precision vector engine with real-time curvature & SDF rendering", 140, 196);
    ctx.restore();

    // Canvas CAD-style Workplane
    const padX = 140;
    const padY = 240;
    const boxW = w - 280;
    const boxH = h - 360;

    ctx.save();
    // Glass Workspace Frame
    ctx.fillStyle = 'rgba(11, 13, 19, 0.8)';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.12)';
    ctx.lineWidth = 1;
    this.drawRoundedRect(ctx, padX, padY, boxW, boxH, 16);
    ctx.fill();
    ctx.stroke();

    // CAD Grid lines
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.04)';
    for (let gx = padX; gx < padX + boxW; gx += 40) {
      ctx.beginPath();
      ctx.moveTo(gx, padY);
      ctx.lineTo(gx, padY + boxH);
      ctx.stroke();
    }
    for (let gy = padY; gy < padY + boxH; gy += 40) {
      ctx.beginPath();
      ctx.moveTo(padX, gy);
      ctx.lineTo(padX + boxW, gy);
      ctx.stroke();
    }

    // Dynamic Bezier Control Points (animating over time)
    const wave = Math.sin(frame * 0.04);
    const p0 = { x: padX + 160, y: padY + boxH * 0.65 };
    const c0 = { x: padX + boxW * 0.28, y: padY + boxH * 0.15 + wave * 60 };
    const c1 = { x: padX + boxW * 0.65, y: padY + boxH * 0.85 - wave * 70 };
    const p1 = { x: padX + boxW - 160, y: padY + boxH * 0.35 };

    // Tangent Lines
    ctx.strokeStyle = 'rgba(0, 242, 254, 0.4)';
    ctx.setLineDash([6, 6]);
    ctx.lineWidth = 2;
    
    ctx.beginPath();
    ctx.moveTo(p0.x, p0.y);
    ctx.lineTo(c0.x, c0.y);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(c1.x, c1.y);
    ctx.stroke();
    ctx.setLineDash([]);

    // Master Glowing Bezier Spline
    ctx.beginPath();
    ctx.moveTo(p0.x, p0.y);
    ctx.bezierCurveTo(c0.x, c0.y, c1.x, c1.y, p1.x, p1.y);
    ctx.strokeStyle = '#00f2fe';
    ctx.lineWidth = 6;
    ctx.shadowColor = '#00f2fe';
    ctx.shadowBlur = 24;
    ctx.stroke();

    // Laser Particle Trailing along the Bezier Curve
    const laserT = (frame * 0.015) % 1;
    const laserPos = this.computeCubicBezierPoint(p0, c0, c1, p1, laserT);
    ctx.beginPath();
    ctx.arc(laserPos.x, laserPos.y, 10, 0, Math.PI * 2);
    ctx.fillStyle = '#ffffff';
    ctx.shadowColor = '#ffffff';
    ctx.shadowBlur = 28;
    ctx.fill();

    // Control Handle Points (P0, C0, C1, P1)
    this.drawAnchorHandle(ctx, p0.x, p0.y, "P0 Anchor", "#00f2fe");
    this.drawAnchorHandle(ctx, c0.x, c0.y, "C0 Tangent", "#fa709a", true);
    this.drawAnchorHandle(ctx, c1.x, c1.y, "C1 Tangent", "#fa709a", true);
    this.drawAnchorHandle(ctx, p1.x, p1.y, "P1 Anchor", "#00f2fe");

    // Real-time HUD Telemetry Panel
    const hudW = 320;
    const hudH = 130;
    const hudX = padX + boxW - hudW - 24;
    const hudY = padY + 24;
    ctx.fillStyle = 'rgba(7, 8, 12, 0.9)';
    ctx.strokeStyle = 'rgba(0, 242, 254, 0.25)';
    this.drawRoundedRect(ctx, hudX, hudY, hudW, hudH, 10);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#00f2fe';
    ctx.font = '600 12px "JetBrains Mono", monospace';
    ctx.textAlign = 'left';
    ctx.fillText("REALTIME SDF GEOMETRY ENGINE", hudX + 16, hudY + 28);

    ctx.fillStyle = '#94a3b8';
    ctx.font = '400 12px "JetBrains Mono", monospace';
    ctx.fillText(`t-parameter : ${laserT.toFixed(3)}`, hudX + 16, hudY + 54);
    ctx.fillText(`laser coord : [${Math.round(laserPos.x)}, ${Math.round(laserPos.y)}]`, hudX + 16, hudY + 76);
    ctx.fillText(`raster mode : Anti-Aliased GPU SDF`, hudX + 16, hudY + 98);

    ctx.restore();
  }

  // --- SCENE 3: 3D Realtime GPU Pipeline & Layer Explosion (8s - 13s) ---

  renderScene3(ctx, p, frame) {
    const w = this.width;
    const h = this.height;
    const cx = w / 2;
    const cy = h / 2 + 10;

    const easeP = this.easeInOutCubic(p);

    // Title Header
    ctx.save();
    ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
    ctx.font = '700 44px "Plus Jakarta Sans", sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText("NON-DESTRUCTIVE GPU PIPELINE", cx, 150);

    ctx.font = '500 20px "Plus Jakarta Sans", sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.fillText("Layered shader nodes composited in real-time with zero render generation latency", cx, 186);
    ctx.restore();

    // 3D Isometric Exploded Layer Stack
    // We render 4 stacked glass panes tilted in isometric 3D space
    const layers = [
      { name: "04. Vector Bezier Overlay", color: "#00f2fe", offsetZ: 140, tag: "SDF Rasterizer" },
      { name: "03. Camera RAW & LUT Color", color: "#fa709a", offsetZ: 70, tag: "ACEScg Tone Map" },
      { name: "02. Glass Dispersion & Blur", color: "#7f00ff", offsetZ: 0, tag: "Kawase Dual Blur" },
      { name: "01. Background Canvas Base", color: "#4facfe", offsetZ: -70, tag: "16-bit Float Artboard" }
    ];

    const spreadFactor = Math.sin(Math.min(1, p * 1.5) * Math.PI) * 1.4;

    ctx.save();
    ctx.translate(cx, cy + 30);

    // Isometric tilt angles
    const tiltX = -0.42;
    const tiltY = 0.28;

    layers.forEach((layer, idx) => {
      ctx.save();
      // Apply isometric slant transform
      const layerZ = layer.offsetZ * (1 + spreadFactor);
      const shiftX = -layerZ * 1.4;
      const shiftY = layerZ * 0.9;

      ctx.translate(shiftX, shiftY);
      ctx.transform(1, tiltX, tiltY, 0.65, 0, 0);

      // Glass Plane
      const cardW = 540;
      const cardH = 340;
      const rx = -cardW / 2;
      const ry = -cardH / 2;

      // Drop shadow
      ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
      ctx.shadowBlur = 30;

      // Surface fill
      const grad = ctx.createLinearGradient(rx, ry, rx + cardW, ry + cardH);
      grad.addColorStop(0, 'rgba(255, 255, 255, 0.08)');
      grad.addColorStop(1, 'rgba(10, 12, 18, 0.7)');
      ctx.fillStyle = grad;
      this.drawRoundedRect(ctx, rx, ry, cardW, cardH, 20);
      ctx.fill();

      // Border with layer accent
      ctx.strokeStyle = layer.color;
      ctx.lineWidth = 2.5;
      ctx.shadowColor = layer.color;
      ctx.shadowBlur = 16;
      ctx.stroke();

      // Mock UI graphic inside the card
      ctx.shadowBlur = 0;
      ctx.fillStyle = 'rgba(255, 255, 255, 0.05)';
      ctx.fillRect(rx + 30, ry + 30, cardW - 60, 40);

      // Layer Title
      ctx.fillStyle = '#ffffff';
      ctx.font = '700 20px "Plus Jakarta Sans", sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText(layer.name, rx + 45, ry + 56);

      // Layer Tag
      ctx.fillStyle = layer.color;
      ctx.font = '600 13px "JetBrains Mono", monospace';
      ctx.fillText(layer.tag, rx + 45, ry + 110);

      // Animated parameter sliders on the layer
      for (let s = 0; s < 3; s++) {
        const barY = ry + 150 + s * 45;
        ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
        ctx.fillRect(rx + 45, barY, cardW - 90, 8);

        const val = 0.4 + Math.sin(frame * 0.05 + idx + s) * 0.35;
        ctx.fillStyle = layer.color;
        ctx.fillRect(rx + 45, barY, (cardW - 90) * val, 8);
      }

      ctx.restore();
    });

    ctx.restore();
  }

  // --- SCENE 4: Kinetic Typography & Presets (13s - 17s) ---

  renderScene4(ctx, p, frame) {
    const w = this.width;
    const h = this.height;
    const cx = w / 2;
    const cy = h / 2;

    const easeP = this.easeInOutCubic(p);

    // Dynamic background strobe pulses
    const pulse = Math.sin(frame * 0.1) * 0.5 + 0.5;

    // Three rhythmic headline cuts
    // 0.0 - 0.33 : "EVERY STROKE."
    // 0.33 - 0.66: "EVERY PIXEL."
    // 0.66 - 1.0 : "INFINITE DEPTH."

    let word = "EVERY STROKE.";
    let sub = "Sub-millisecond touch tracking and zero-latency rendering";
    let accent = "#00f2fe";

    if (p > 0.66) {
      word = "INFINITE DEPTH.";
      sub = "Realtime GPU blend modes, displacement shaders, and displacement passes";
      accent = "#fa709a";
    } else if (p > 0.33) {
      word = "EVERY PIXEL.";
      sub = "Native 32-bit floating point color pipeline with Camera RAW precision";
      accent = "#7f00ff";
    }

    ctx.save();
    ctx.textAlign = 'center';

    // Massive Backdrop Watermark Typography
    ctx.font = '900 180px "Plus Jakarta Sans", sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.02)';
    ctx.letterSpacing = '12px';
    ctx.fillText("ZENITH", cx, cy + 50);

    // Main Kinetic Punch Word
    ctx.font = '800 84px "Plus Jakarta Sans", sans-serif';
    ctx.letterSpacing = '4px';

    const textGrad = ctx.createLinearGradient(cx - 300, cy, cx + 300, cy);
    textGrad.addColorStop(0, '#ffffff');
    textGrad.addColorStop(0.5, accent);
    textGrad.addColorStop(1, '#ffffff');
    ctx.fillStyle = textGrad;
    ctx.shadowColor = accent;
    ctx.shadowBlur = 30;
    ctx.fillText(word, cx, cy - 20);

    // Kinetic Subtitle
    ctx.shadowBlur = 0;
    ctx.font = '500 24px "Plus Jakarta Sans", sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.letterSpacing = '1px';
    ctx.fillText(sub, cx, cy + 45);

    // Typographic Font Engine Showcase Cards
    const fontCards = [
      { name: "JetBrains Mono", sample: "const pipeline = GPU.compile();", style: "Code & Presets" },
      { name: "Plus Jakarta Sans", sample: "Ultra-crisp UI Compositing", style: "Modern Display" },
      { name: "Cinzel Decorative", sample: "CREATIVE ARTISTRY", style: "Rich Text Spans" }
    ];

    const cardStartX = cx - 460;
    fontCards.forEach((fc, i) => {
      const fcX = cardStartX + i * 320;
      const fcY = cy + 130;
      const fcW = 280;
      const fcH = 120;

      ctx.fillStyle = 'rgba(14, 16, 24, 0.85)';
      ctx.strokeStyle = i === 1 ? accent : 'rgba(255, 255, 255, 0.1)';
      ctx.lineWidth = i === 1 ? 2 : 1;
      this.drawRoundedRect(ctx, fcX, fcY, fcW, fcH, 12);
      ctx.fill();
      ctx.stroke();

      ctx.textAlign = 'left';
      ctx.fillStyle = accent;
      ctx.font = '600 12px "JetBrains Mono", monospace';
      ctx.fillText(fc.style, fcX + 18, fcY + 30);

      ctx.fillStyle = '#ffffff';
      ctx.font = '700 16px "Plus Jakarta Sans", sans-serif';
      ctx.fillText(fc.name, fcX + 18, fcY + 58);

      ctx.fillStyle = '#94a3b8';
      ctx.font = '400 12px "JetBrains Mono", monospace';
      ctx.fillText(fc.sample, fcX + 18, fcY + 86);
    });

    ctx.restore();
  }

  // --- SCENE 5: Finale Lockup & Call To Action (17s - 20s) ---

  renderScene5(ctx, p, frame) {
    const w = this.width;
    const h = this.height;
    const cx = w / 2;
    const cy = h / 2 - 20;

    const easeP = this.easeOutExpo(Math.min(1, p * 1.2));

    // Radiant Solar Corona
    const sunGrad = ctx.createRadialGradient(cx, cy, 20, cx, cy, 500 * easeP);
    sunGrad.addColorStop(0, 'rgba(0, 242, 254, 0.25)');
    sunGrad.addColorStop(0.4, 'rgba(127, 0, 255, 0.15)');
    sunGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = sunGrad;
    ctx.fillRect(0, 0, w, h);

    // Glowing Mobile Frame Device Silhouette
    ctx.save();
    ctx.translate(cx, cy);
    const phoneW = 320;
    const phoneH = 520;
    ctx.fillStyle = 'rgba(10, 11, 16, 0.92)';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.2)';
    ctx.lineWidth = 3;
    ctx.shadowColor = '#00f2fe';
    ctx.shadowBlur = 40 * easeP;
    this.drawRoundedRect(ctx, -phoneW/2, -phoneH/2, phoneW, phoneH, 36);
    ctx.fill();
    ctx.stroke();

    // Screen content inside phone
    ctx.beginPath();
    this.drawRoundedRect(ctx, -phoneW/2 + 8, -phoneH/2 + 8, phoneW - 16, phoneH - 16, 30);
    ctx.clip();

    // App header & preview inside mockup
    ctx.fillStyle = '#0f111a';
    ctx.fillRect(-phoneW/2, -phoneH/2, phoneW, phoneH);

    // Stylized Canvas Art Preview inside phone
    const wave = Math.sin(frame * 0.05);
    ctx.beginPath();
    ctx.arc(0, 0, 80, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(127, 0, 255, 0.3)';
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(-100, 60);
    ctx.bezierCurveTo(-40, -60, 40, 80, 100, -30);
    ctx.strokeStyle = '#00f2fe';
    ctx.lineWidth = 5;
    ctx.stroke();
    ctx.restore();

    // Text & CTA Side Callout Badges
    ctx.save();
    ctx.textAlign = 'left';

    // Left Feature Lockup
    ctx.fillStyle = '#ffffff';
    ctx.font = '800 52px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("ZENITH STUDIO", 120, cy - 40);

    ctx.font = '500 22px "Plus Jakarta Sans", sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.fillText("Create Without Limits.", 120, cy + 4);

    const featureList = [
      "✓ 60 FPS Native Vulkan & OpenGL Engine",
      "✓ Alight Motion XML & Ibis Paint Layer Support",
      "✓ Camera RAW & Realtime GPU Displacement",
      "✓ Zero Subscription • Open Architecture"
    ];

    featureList.forEach((feat, fi) => {
      ctx.font = '500 16px "JetBrains Mono", monospace';
      ctx.fillStyle = fi === 0 ? '#00f2fe' : '#cbd5e1';
      ctx.fillText(feat, 120, cy + 60 + fi * 34);
    });

    // Right Action CTA Lockup
    const ctaX = w - 480;
    ctx.fillStyle = 'rgba(14, 16, 24, 0.9)';
    ctx.strokeStyle = 'rgba(0, 242, 254, 0.4)';
    ctx.lineWidth = 1.5;
    this.drawRoundedRect(ctx, ctaX, cy - 80, 360, 220, 16);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#ffffff';
    ctx.font = '700 22px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("Ready to Elevate Your Art?", ctaX + 28, cy - 40);

    ctx.fillStyle = '#94a3b8';
    ctx.font = '400 14px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("Experience the mobile compositing revolution.", ctaX + 28, cy - 14);

    // Button Mock
    ctx.fillStyle = 'linear-gradient(135deg, #00f2fe, #4facfe)';
    const btnGrad = ctx.createLinearGradient(ctaX + 28, cy + 20, ctaX + 332, cy + 20);
    btnGrad.addColorStop(0, '#00f2fe');
    btnGrad.addColorStop(1, '#7f00ff');
    ctx.fillStyle = btnGrad;
    this.drawRoundedRect(ctx, ctaX + 28, cy + 20, 304, 52, 10);
    ctx.fill();

    ctx.textAlign = 'center';
    ctx.fillStyle = '#07070a';
    ctx.font = '700 15px "JetBrains Mono", monospace';
    ctx.fillText("LAUNCH ZENITH STUDIO ➔", ctaX + 28 + 152, cy + 52);

    ctx.restore();
  }

  // --- Helper Drawing Routines ---

  drawAnchorHandle(ctx, x, y, label, color, isTangent = false) {
    ctx.save();
    ctx.beginPath();
    if (isTangent) {
      ctx.arc(x, y, 7, 0, Math.PI * 2);
    } else {
      ctx.rect(x - 7, y - 7, 14, 14);
    }
    ctx.fillStyle = color;
    ctx.shadowColor = color;
    ctx.shadowBlur = 14;
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = '#cbd5e1';
    ctx.font = '500 11px "JetBrains Mono", monospace';
    ctx.textAlign = 'center';
    ctx.fillText(label, x, y - 14);
    ctx.restore();
  }

  computeCubicBezierPoint(p0, c0, c1, p1, t) {
    const u = 1 - t;
    const tt = t * t;
    const uu = u * u;
    const uuu = uu * u;
    const ttt = tt * t;

    return {
      x: uuu * p0.x + 3 * uu * t * c0.x + 3 * u * tt * c1.x + ttt * p1.x,
      y: uuu * p0.y + 3 * uu * t * c0.y + 3 * u * tt * c1.y + ttt * p1.y
    };
  }

  drawRoundedRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
  }

  drawCinematicBorderHUD(ctx, frame) {
    const w = this.width;
    const h = this.height;

    // Corner targeting reticles
    ctx.save();
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.2)';
    ctx.lineWidth = 1.5;

    const len = 18;
    const margin = 32;

    // Top-left
    ctx.beginPath();
    ctx.moveTo(margin, margin + len);
    ctx.lineTo(margin, margin);
    ctx.lineTo(margin + len, margin);
    ctx.stroke();

    // Top-right
    ctx.beginPath();
    ctx.moveTo(w - margin - len, margin);
    ctx.lineTo(w - margin, margin);
    ctx.lineTo(w - margin, margin + len);
    ctx.stroke();

    // Bottom-left
    ctx.beginPath();
    ctx.moveTo(margin, h - margin - len);
    ctx.lineTo(margin, h - margin);
    ctx.lineTo(margin + len, h - margin);
    ctx.stroke();

    // Bottom-right
    ctx.beginPath();
    ctx.moveTo(w - margin - len, h - margin);
    ctx.lineTo(w - margin, h - margin);
    ctx.lineTo(w - margin, h - margin - len);
    ctx.stroke();

    // Header Timecode Watermark (Rec • 60 FPS)
    ctx.fillStyle = '#ef4444';
    ctx.beginPath();
    ctx.arc(margin + 36, margin + 7, 5, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.font = '600 12px "JetBrains Mono", monospace';
    ctx.textAlign = 'left';
    ctx.fillText("REC  60FPS  1080P  ACQUIRED", margin + 50, margin + 11);

    // Frame Counter
    ctx.textAlign = 'right';
    ctx.fillText(`FRAME ${String(frame).padStart(4, '0')} / ${this.totalFrames}`, w - margin - 30, margin + 11);

    ctx.restore();
  }

  // --- Snapshot Utility ---

  captureSnapshot() {
    return this.canvas.toDataURL('image/png');
  }
}

// Export to window for browser usage
window.ZenithMotionEngine = ZenithMotionEngine;
