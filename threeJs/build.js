const esbuild = require('esbuild');
const path = require('path');

esbuild.build({
  entryPoints: ['main.js'],
  bundle: true,
  outfile: path.resolve(__dirname, '../composeApp/src/commonMain/composeResources/files/bundle.js'),
  format: 'iife', // Wrap for browser
  target: ['es2017'],
  minify: true,
}).then(() => {
  console.log('✔ Build complete');
}).catch(() => process.exit(1));
