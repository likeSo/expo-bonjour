// Reexport the native module. On web, it will be resolved to ExpoBonjourModule.web.ts
// and on native platforms to ExpoBonjourModule.ts
export { default } from './ExpoBonjourModule';
export * from  './ExpoBonjour.types';
