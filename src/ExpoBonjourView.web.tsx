import * as React from 'react';

import { ExpoBonjourViewProps } from './ExpoBonjour.types';

export default function ExpoBonjourView(props: ExpoBonjourViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
