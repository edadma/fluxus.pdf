import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [scalaJSPlugin({
    cwd: ".",  // Root directory of the project
    projectID: "examples" // The SBT project ID
  })],
//  build: {
//    rollupOptions: {
//      output: {
//        manualChunks: {
//          pdfjs: ['pdfjs-dist']
//        }
//      }
//    }
//  },
//  optimizeDeps: {
//    include: ['pdfjs-dist']
//  },
//  server: {
//    // This will serve PDF.js worker file at root level
//    setupMiddleware: (middleware, server) => {
//      server.middlewares.use((req, res, next) => {
//        if (req.url === '/pdf.worker.min.js') {
//          const workerPath = resolve(
//            './node_modules/pdfjs-dist/build/pdf.worker.min.js'
//          );
//
//          if (fs.existsSync(workerPath)) {
//            res.setHeader('Content-Type', 'application/javascript');
//            fs.createReadStream(workerPath).pipe(res);
//          } else {
//            console.error('PDF.js worker file not found at:', workerPath);
//            next();
//          }
//        } else {
//          next();
//        }
//      });
//    }
//  }
});
