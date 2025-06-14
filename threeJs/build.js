const esbuild = require('esbuild');
const path = require('path');

esbuild.build({
  entryPoints: [path.resolve(__dirname, 'src/main.js')],
  bundle: true,
  outdir: path.resolve(__dirname, '../composeApp/src/commonMain/composeResources/files'),
  outbase: 'src',
  format: 'iife',
  target: ['es2017'],
  minify: true,
  sourcemap: true,
  loader: {
    '.png': 'dataurl',
    '.jpg': 'dataurl',
    '.glb': 'binary'
  },
  define: {
    'process.env.NODE_ENV': '"production"'
  },
  plugins: [{
    name: 'on-rebuild',
    setup(build) {
      build.onEnd(result => {
        if (result.errors.length) {
          console.error('❌ Build failed:');
          result.errors.forEach(error => console.error(error));
        } else {
          console.log('✔ Build successful');
        }
      });
    }
  }]
}).catch(() => process.exit(1));