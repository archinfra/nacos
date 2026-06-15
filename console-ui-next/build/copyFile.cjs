/*
 * Copy console-ui-next build output into Nacos console backend static resources.
 * This script is intentionally used instead of shell `cp -r` so GitHub Actions,
 * Linux shells and developer machines get the same behavior.
 */
const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '../dist');
const destDir = path.resolve(__dirname, '../../console/src/main/resources/static/next');

function die(message) {
  console.error(`[copy-ui] ${message}`);
  process.exit(1);
}

function copyDir(src, dest) {
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else if (entry.isSymbolicLink()) {
      const target = fs.readlinkSync(srcPath);
      try { fs.unlinkSync(destPath); } catch (_) {}
      fs.symlinkSync(target, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

if (!fs.existsSync(srcDir)) {
  die(`Build output not found: ${srcDir}. Run npm run build:standalone first, or run npm run build.`);
}

fs.mkdirSync(path.dirname(destDir), { recursive: true });
fs.rmSync(destDir, { recursive: true, force: true });
copyDir(srcDir, destDir);

const indexFile = path.join(destDir, 'index.html');
if (!fs.existsSync(indexFile)) {
  die(`Copied files, but ${indexFile} does not exist. Please check Vite build output.`);
}

console.log(`[copy-ui] Copied ${srcDir} -> ${destDir}`);
