/**
 * Zenith Studio Motion Video Exporter
 * Frame-accurate renderer script inspired by latent-spaces/brag & Hyperframes
 * 
 * Usage:
 *   node export.js
 * 
 * Requirements:
 *   npm install puppeteer
 *   ffmpeg installed on system PATH
 */

const fs = require('fs');
const path = require('path');
const { execSync, spawn } = require('child_process');

async function renderVideo() {
  let puppeteer;
  try {
    puppeteer = require('puppeteer');
  } catch (err) {
    console.error("\x1b[31m[Error]\x1b[0m 'puppeteer' is not installed. Run: npm install puppeteer");
    process.exit(1);
  }

  const outputDir = path.join(__dirname, 'frames');
  const outputFile = path.join(__dirname, 'zenith-launch.mp4');

  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  console.log("\x1b[36m⚡ Starting Zenith Studio Motion Graphics Renderer...\x1b[0m");
  console.log("Resolution: 1920x1080 (16:9) | Framerate: 60 FPS | Total Duration: 20s (1200 frames)\n");

  const browser = await puppeteer.launch({
    headless: "new",
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080, deviceScaleFactor: 1 });

  const htmlPath = 'file://' + path.resolve(__dirname, 'index.html');
  await page.goto(htmlPath, { waitUntil: 'networkidle0' });

  // Pause playback engine to control frame-by-frame progression deterministically
  await page.evaluate(() => {
    if (window.ZenithMotionEngine) {
      // Find the active engine instance
      const canvas = document.getElementById('motion-canvas');
      const ctx = canvas.getContext('2d');
      // Pause any ongoing auto-tick
      if (window.engineInstance) {
        window.engineInstance.pause();
      }
    }
  });

  const totalFrames = 1200;
  console.log(`[1/2] Rendering ${totalFrames} frames...`);

  const startTime = Date.now();

  for (let frame = 0; frame < totalFrames; frame++) {
    // Step engine directly to target frame
    await page.evaluate((f) => {
      const canvas = document.getElementById('motion-canvas');
      // If engine accessible on canvas or window
      if (window.engineInstance) {
        window.engineInstance.seek(f);
      } else {
        // Fallback seeking through canvas
        const e = new window.ZenithMotionEngine(canvas);
        e.seek(f);
      }
    }, frame);

    const frameFile = path.join(outputDir, `frame_${String(frame).padStart(5, '0')}.png`);
    const canvasElement = await page.$('#motion-canvas');
    await canvasElement.screenshot({ path: frameFile, type: 'png' });

    if (frame % 60 === 0 || frame === totalFrames - 1) {
      const progress = ((frame / totalFrames) * 100).toFixed(1);
      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
      process.stdout.write(`\rRendered Frame ${frame}/${totalFrames} (${progress}%) - ${elapsed}s elapsed`);
    }
  }

  console.log("\n\n[2/2] Encoding frames to MP4 using FFmpeg...");
  await browser.close();

  try {
    const ffmpegCmd = `ffmpeg -y -r 60 -i "${outputDir}/frame_%05d.png" -c:v libx264 -pix_fmt yuv420p -preset fast -crf 18 "${outputFile}"`;
    execSync(ffmpegCmd, { stdio: 'inherit' });
    console.log(`\n\x1b[32m✔ Success! Video rendered to: ${outputFile}\x1b[0m\n`);
  } catch (err) {
    console.warn("\x1b[33mFFmpeg encode failed or FFmpeg is not installed.\x1b[0m");
    console.log(`Rendered frames are safely saved in: ${outputDir}`);
    console.log("You can encode them manually with:");
    console.log(`ffmpeg -y -r 60 -i "frames/frame_%05d.png" -c:v libx264 -pix_fmt yuv420p output.mp4`);
  }
}

if (require.main === module) {
  renderVideo().catch(console.error);
}
