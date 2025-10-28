import { registerWebModule, NativeModule } from 'expo';

import { ExpoBonjourModuleEvents } from './ExpoBonjour.types';

class ExpoBonjourModule extends NativeModule<ExpoBonjourModuleEvents> {
  
}

export default registerWebModule(ExpoBonjourModule, 'ExpoBonjourModule');
