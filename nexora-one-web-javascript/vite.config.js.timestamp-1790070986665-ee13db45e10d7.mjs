// vite.config.js
import { resolve } from "path";
import vue from "file:///D:/IdeaProjects/Nexora-One/nexora-one-web-javascript/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { loadEnv } from "file:///D:/IdeaProjects/Nexora-One/nexora-one-web-javascript/node_modules/vite/dist/node/index.js";

// src/theme/custom-variables.js
import { theme } from "file:///D:/IdeaProjects/Nexora-One/nexora-one-web-javascript/node_modules/ant-design-vue/lib/index.js";
import convertLegacyToken from "file:///D:/IdeaProjects/Nexora-One/nexora-one-web-javascript/node_modules/ant-design-vue/lib/theme/convertLegacyToken.js";
var { defaultAlgorithm, defaultSeed } = theme;
var mapToken = defaultAlgorithm(defaultSeed);
var token = convertLegacyToken.default(mapToken);
var custom_variables_default = {
  "@primary-color": token["primary-color"],
  // 全局主色
  "@base-bg-color": "#fff",
  "@hover-bg-color": "rgba(0, 0, 0, 0.025)",
  "@hover-bg-color-night": "rgba(255, 255, 255, 0.025)",
  "@header-light-bg-hover-color": "#f6f6f6",
  "@header-height": "80px",
  "@header-user-height": "40px",
  "@page-tag-height": "40px",
  "@theme-list": ["light", "dark", "night"]
};

// vite.config.js
var __vite_injected_original_dirname = "D:\\IdeaProjects\\Nexora-One\\nexora-one-web-javascript";
var pathResolve = (dir) => {
  return resolve(__vite_injected_original_dirname, ".", dir);
};
var vite_config_default = ({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    // base: process.env.NODE_ENV === 'production' ? '/' : '/',
    base: "/",
    root: process.cwd(),
    resolve: {
      alias: [
        // 国际化替换
        {
          find: "vue-i18n",
          replacement: "vue-i18n/dist/vue-i18n.cjs.js"
        },
        // 绝对路径重命名：/@/xxxx => src/xxxx
        {
          find: /\/@\//,
          replacement: pathResolve("src") + "/"
        },
        {
          find: /^~/,
          replacement: ""
        }
      ]
    },
    server: {
      host: "0.0.0.0",
      port: 8081,
      proxy: {
        // 代理路径
        "/api": {
          target: env.VITE_APP_API_URL,
          // 目标服务器地址
          changeOrigin: true,
          // 是否修改请求头中的 Origin 字段
          rewrite: (path) => path.replace(/^\/api/, "")
          // 重写路径
        }
      }
    },
    plugins: [vue()],
    optimizeDeps: {
      include: ["ant-design-vue/es/locale/zh_CN", "dayjs/locale/zh-cn", "ant-design-vue/es/locale/en_US"],
      exclude: ["vue-demi"]
    },
    build: {
      // 清除console和debugger
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true
        }
      },
      // rollupOptions: {
      //   output: {
      //     //配置这个是让不同类型文件放在不同文件夹，不会显得太乱
      //     chunkFileNames: 'js/[name]-[hash].js',
      //     entryFileNames: 'js/[name]-[hash].js',
      //     assetFileNames: '[ext]/[name]-[hash].[ext]',
      //     manualChunks(id) {
      //       //静态资源分拆打包
      //       if (id.includes('node_modules')) {
      //         return id.toString().split('node_modules/')[1].split('/')[0].toString();
      //       }
      //     },
      //   },
      // },
      target: "esnext",
      outDir: "dist",
      // 指定输出路径
      assetsDir: "assets",
      // 指定生成静态文件目录
      assetsInlineLimit: "4096",
      // 小于此阈值的导入或引用资源将内联为 base64 编码
      chunkSizeWarningLimit: 500,
      // chunk 大小警告的限制
      minify: "terser",
      // 混淆器，terser构建后文件体积更小
      emptyOutDir: true
      //打包前先清空原有打包文件
    },
    css: {
      preprocessorOptions: {
        less: {
          modifyVars: custom_variables_default,
          javascriptEnabled: true
        }
      }
    },
    define: {
      __INTLIFY_PROD_DEVTOOLS__: false,
      "process.env": process.env
    }
  };
};
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiLCAic3JjL3RoZW1lL2N1c3RvbS12YXJpYWJsZXMuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxJZGVhUHJvamVjdHNcXFxcTmV4b3JhLU9uZVxcXFxuZXhvcmEtb25lLXdlYi1qYXZhc2NyaXB0XCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJEOlxcXFxJZGVhUHJvamVjdHNcXFxcTmV4b3JhLU9uZVxcXFxuZXhvcmEtb25lLXdlYi1qYXZhc2NyaXB0XFxcXHZpdGUuY29uZmlnLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9EOi9JZGVhUHJvamVjdHMvTmV4b3JhLU9uZS9uZXhvcmEtb25lLXdlYi1qYXZhc2NyaXB0L3ZpdGUuY29uZmlnLmpzXCI7LypcclxuICogdml0ZVx1OTE0RFx1N0Y2RVxyXG4gKlxyXG4gKiBAQXV0aG9yOiAgICBOZXhvcmFPbmUtXHU0RTNCXHU0RUZCXHVGRjFBXHU1MzUzXHU1OTI3XHJcbiAqIEBEYXRlOiAgICAgIDIwMjItMDUtMDIgMjM6NDQ6NTZcclxuICogQFdlY2hhdDogICAgTmV4b3JhT25lXHJcbiAqIEBFbWFpbDogICAgIE5leG9yYU9uZVxyXG4gKiBAQ29weXJpZ2h0ICBOZXhvcmFPbmUgXHVGRjA4ICMgXHVGRjA5XHVGRjBDU2luY2UgMjAxMlxyXG4gKi9cclxuaW1wb3J0IHsgcmVzb2x2ZSB9IGZyb20gJ3BhdGgnO1xyXG5pbXBvcnQgdnVlIGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZSc7XHJcbmltcG9ydCB7IGxvYWRFbnYgfSBmcm9tICd2aXRlJztcclxuaW1wb3J0IGN1c3RvbVZhcmlhYmxlcyBmcm9tICcvQC90aGVtZS9jdXN0b20tdmFyaWFibGVzJztcclxuXHJcbmNvbnN0IHBhdGhSZXNvbHZlID0gKGRpcikgPT4ge1xyXG4gIHJldHVybiByZXNvbHZlKF9fZGlybmFtZSwgJy4nLCBkaXIpO1xyXG59O1xyXG5cclxuZXhwb3J0IGRlZmF1bHQgKHsgbW9kZSB9KSA9PiB7XHJcbiAgY29uc3QgZW52ID0gbG9hZEVudihtb2RlLCBwcm9jZXNzLmN3ZCgpKTtcclxuICByZXR1cm4ge1xyXG4gICAgLy8gYmFzZTogcHJvY2Vzcy5lbnYuTk9ERV9FTlYgPT09ICdwcm9kdWN0aW9uJyA/ICcvJyA6ICcvJyxcclxuICAgIGJhc2U6ICcvJyxcclxuICAgIHJvb3Q6IHByb2Nlc3MuY3dkKCksXHJcbiAgICByZXNvbHZlOiB7XHJcbiAgICAgIGFsaWFzOiBbXHJcbiAgICAgICAgLy8gXHU1NkZEXHU5NjQ1XHU1MzE2XHU2NkZGXHU2MzYyXHJcbiAgICAgICAge1xyXG4gICAgICAgICAgZmluZDogJ3Z1ZS1pMThuJyxcclxuICAgICAgICAgIHJlcGxhY2VtZW50OiAndnVlLWkxOG4vZGlzdC92dWUtaTE4bi5janMuanMnLFxyXG4gICAgICAgIH0sXHJcbiAgICAgICAgLy8gXHU3RUREXHU1QkY5XHU4REVGXHU1Rjg0XHU5MUNEXHU1NDdEXHU1NDBEXHVGRjFBL0AveHh4eCA9PiBzcmMveHh4eFxyXG4gICAgICAgIHtcclxuICAgICAgICAgIGZpbmQ6IC9cXC9AXFwvLyxcclxuICAgICAgICAgIHJlcGxhY2VtZW50OiBwYXRoUmVzb2x2ZSgnc3JjJykgKyAnLycsXHJcbiAgICAgICAgfSxcclxuICAgICAgICB7XHJcbiAgICAgICAgICBmaW5kOiAvXn4vLFxyXG4gICAgICAgICAgcmVwbGFjZW1lbnQ6ICcnLFxyXG4gICAgICAgIH0sXHJcbiAgICAgIF0sXHJcbiAgICB9LFxyXG4gICAgc2VydmVyOiB7XHJcbiAgICAgIGhvc3Q6ICcwLjAuMC4wJyxcclxuICAgICAgcG9ydDogODA4MSxcclxuICAgICAgcHJveHk6IHtcclxuICAgICAgICAvLyBcdTRFRTNcdTc0MDZcdThERUZcdTVGODRcclxuICAgICAgICAnL2FwaSc6IHtcclxuICAgICAgICAgIHRhcmdldDogZW52LlZJVEVfQVBQX0FQSV9VUkwsIC8vIFx1NzZFRVx1NjgwN1x1NjcwRFx1NTJBMVx1NTY2OFx1NTczMFx1NTc0MFxyXG4gICAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLCAvLyBcdTY2MkZcdTU0MjZcdTRGRUVcdTY1MzlcdThCRjdcdTZDNDJcdTU5MzRcdTRFMkRcdTc2ODQgT3JpZ2luIFx1NUI1N1x1NkJCNVxyXG4gICAgICAgICAgcmV3cml0ZTogKHBhdGgpID0+IHBhdGgucmVwbGFjZSgvXlxcL2FwaS8sICcnKSwgLy8gXHU5MUNEXHU1MTk5XHU4REVGXHU1Rjg0XHJcbiAgICAgICAgfSxcclxuICAgICAgfSxcclxuICAgIH0sXHJcbiAgICBwbHVnaW5zOiBbdnVlKCldLFxyXG4gICAgb3B0aW1pemVEZXBzOiB7XHJcbiAgICAgIGluY2x1ZGU6IFsnYW50LWRlc2lnbi12dWUvZXMvbG9jYWxlL3poX0NOJywgJ2RheWpzL2xvY2FsZS96aC1jbicsICdhbnQtZGVzaWduLXZ1ZS9lcy9sb2NhbGUvZW5fVVMnXSxcclxuICAgICAgZXhjbHVkZTogWyd2dWUtZGVtaSddLFxyXG4gICAgfSxcclxuICAgIGJ1aWxkOiB7XHJcbiAgICAgIC8vIFx1NkUwNVx1OTY2NGNvbnNvbGVcdTU0OENkZWJ1Z2dlclxyXG4gICAgICB0ZXJzZXJPcHRpb25zOiB7XHJcbiAgICAgICAgY29tcHJlc3M6IHtcclxuICAgICAgICAgIGRyb3BfY29uc29sZTogdHJ1ZSxcclxuICAgICAgICAgIGRyb3BfZGVidWdnZXI6IHRydWUsXHJcbiAgICAgICAgfSxcclxuICAgICAgfSxcclxuICAgICAgLy8gcm9sbHVwT3B0aW9uczoge1xyXG4gICAgICAvLyAgIG91dHB1dDoge1xyXG4gICAgICAvLyAgICAgLy9cdTkxNERcdTdGNkVcdThGRDlcdTRFMkFcdTY2MkZcdThCQTlcdTRFMERcdTU0MENcdTdDN0JcdTU3OEJcdTY1ODdcdTRFRjZcdTY1M0VcdTU3MjhcdTRFMERcdTU0MENcdTY1ODdcdTRFRjZcdTU5MzlcdUZGMENcdTRFMERcdTRGMUFcdTY2M0VcdTVGOTdcdTU5MkFcdTRFNzFcclxuICAgICAgLy8gICAgIGNodW5rRmlsZU5hbWVzOiAnanMvW25hbWVdLVtoYXNoXS5qcycsXHJcbiAgICAgIC8vICAgICBlbnRyeUZpbGVOYW1lczogJ2pzL1tuYW1lXS1baGFzaF0uanMnLFxyXG4gICAgICAvLyAgICAgYXNzZXRGaWxlTmFtZXM6ICdbZXh0XS9bbmFtZV0tW2hhc2hdLltleHRdJyxcclxuICAgICAgLy8gICAgIG1hbnVhbENodW5rcyhpZCkge1xyXG4gICAgICAvLyAgICAgICAvL1x1OTc1OVx1NjAwMVx1OEQ0NFx1NkU5MFx1NTIwNlx1NjJDNlx1NjI1M1x1NTMwNVxyXG4gICAgICAvLyAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ25vZGVfbW9kdWxlcycpKSB7XHJcbiAgICAgIC8vICAgICAgICAgcmV0dXJuIGlkLnRvU3RyaW5nKCkuc3BsaXQoJ25vZGVfbW9kdWxlcy8nKVsxXS5zcGxpdCgnLycpWzBdLnRvU3RyaW5nKCk7XHJcbiAgICAgIC8vICAgICAgIH1cclxuICAgICAgLy8gICAgIH0sXHJcbiAgICAgIC8vICAgfSxcclxuICAgICAgLy8gfSxcclxuICAgICAgdGFyZ2V0OiAnZXNuZXh0JyxcclxuICAgICAgb3V0RGlyOiAnZGlzdCcsIC8vIFx1NjMwN1x1NUI5QVx1OEY5M1x1NTFGQVx1OERFRlx1NUY4NFxyXG4gICAgICBhc3NldHNEaXI6ICdhc3NldHMnLCAvLyBcdTYzMDdcdTVCOUFcdTc1MUZcdTYyMTBcdTk3NTlcdTYwMDFcdTY1ODdcdTRFRjZcdTc2RUVcdTVGNTVcclxuICAgICAgYXNzZXRzSW5saW5lTGltaXQ6ICc0MDk2JywgLy8gXHU1QzBGXHU0RThFXHU2QjY0XHU5NjA4XHU1MDNDXHU3Njg0XHU1QkZDXHU1MTY1XHU2MjE2XHU1RjE1XHU3NTI4XHU4RDQ0XHU2RTkwXHU1QzA2XHU1MTg1XHU4MDU0XHU0RTNBIGJhc2U2NCBcdTdGMTZcdTc4MDFcclxuICAgICAgY2h1bmtTaXplV2FybmluZ0xpbWl0OiA1MDAsIC8vIGNodW5rIFx1NTkyN1x1NUMwRlx1OEI2Nlx1NTQ0QVx1NzY4NFx1OTY1MFx1NTIzNlxyXG4gICAgICBtaW5pZnk6ICd0ZXJzZXInLCAvLyBcdTZERjdcdTZEQzZcdTU2NjhcdUZGMEN0ZXJzZXJcdTY3ODRcdTVFRkFcdTU0MEVcdTY1ODdcdTRFRjZcdTRGNTNcdTc5RUZcdTY2RjRcdTVDMEZcclxuICAgICAgZW1wdHlPdXREaXI6IHRydWUsIC8vXHU2MjUzXHU1MzA1XHU1MjREXHU1MTQ4XHU2RTA1XHU3QTdBXHU1MzlGXHU2NzA5XHU2MjUzXHU1MzA1XHU2NTg3XHU0RUY2XHJcbiAgICB9LFxyXG4gICAgY3NzOiB7XHJcbiAgICAgIHByZXByb2Nlc3Nvck9wdGlvbnM6IHtcclxuICAgICAgICBsZXNzOiB7XHJcbiAgICAgICAgICBtb2RpZnlWYXJzOiBjdXN0b21WYXJpYWJsZXMsXHJcbiAgICAgICAgICBqYXZhc2NyaXB0RW5hYmxlZDogdHJ1ZSxcclxuICAgICAgICB9LFxyXG4gICAgICB9LFxyXG4gICAgfSxcclxuICAgIGRlZmluZToge1xyXG4gICAgICBfX0lOVExJRllfUFJPRF9ERVZUT09MU19fOiBmYWxzZSxcclxuICAgICAgJ3Byb2Nlc3MuZW52JzogcHJvY2Vzcy5lbnYsXHJcbiAgICB9LFxyXG4gIH07XHJcbn07XHJcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiRDpcXFxcSWRlYVByb2plY3RzXFxcXE5leG9yYS1PbmVcXFxcbmV4b3JhLW9uZS13ZWItamF2YXNjcmlwdFxcXFxzcmNcXFxcdGhlbWVcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXElkZWFQcm9qZWN0c1xcXFxOZXhvcmEtT25lXFxcXG5leG9yYS1vbmUtd2ViLWphdmFzY3JpcHRcXFxcc3JjXFxcXHRoZW1lXFxcXGN1c3RvbS12YXJpYWJsZXMuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6L0lkZWFQcm9qZWN0cy9OZXhvcmEtT25lL25leG9yYS1vbmUtd2ViLWphdmFzY3JpcHQvc3JjL3RoZW1lL2N1c3RvbS12YXJpYWJsZXMuanNcIjtpbXBvcnQgeyB0aGVtZSB9IGZyb20gJ2FudC1kZXNpZ24tdnVlL2xpYic7XHJcbmltcG9ydCBjb252ZXJ0TGVnYWN5VG9rZW4gZnJvbSAnYW50LWRlc2lnbi12dWUvbGliL3RoZW1lL2NvbnZlcnRMZWdhY3lUb2tlbic7XHJcblxyXG5jb25zdCB7IGRlZmF1bHRBbGdvcml0aG0sIGRlZmF1bHRTZWVkIH0gPSB0aGVtZTtcclxuXHJcbmNvbnN0IG1hcFRva2VuID0gZGVmYXVsdEFsZ29yaXRobShkZWZhdWx0U2VlZCk7XHJcbmNvbnN0IHRva2VuID0gY29udmVydExlZ2FjeVRva2VuLmRlZmF1bHQobWFwVG9rZW4pO1xyXG5cclxuZXhwb3J0IGRlZmF1bHQge1xyXG4gICdAcHJpbWFyeS1jb2xvcic6IHRva2VuWydwcmltYXJ5LWNvbG9yJ10sIC8vIFx1NTE2OFx1NUM0MFx1NEUzQlx1ODI3MlxyXG4gICdAYmFzZS1iZy1jb2xvcic6ICcjZmZmJyxcclxuICAnQGhvdmVyLWJnLWNvbG9yJzogJ3JnYmEoMCwgMCwgMCwgMC4wMjUpJyxcclxuICAnQGhvdmVyLWJnLWNvbG9yLW5pZ2h0JzogJ3JnYmEoMjU1LCAyNTUsIDI1NSwgMC4wMjUpJyxcclxuICAnQGhlYWRlci1saWdodC1iZy1ob3Zlci1jb2xvcic6ICcjZjZmNmY2JyxcclxuICAnQGhlYWRlci1oZWlnaHQnOiAnODBweCcsXHJcbiAgJ0BoZWFkZXItdXNlci1oZWlnaHQnOiAnNDBweCcsXHJcbiAgJ0BwYWdlLXRhZy1oZWlnaHQnOiAnNDBweCcsXHJcbiAgJ0B0aGVtZS1saXN0JzogWydsaWdodCcsICdkYXJrJywgJ25pZ2h0J10sXHJcbn07XHJcbiJdLAog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
