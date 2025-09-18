import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';

import vaadinAppLayoutCss from 'themes/oxide/components/vaadin-app-layout.css?inline';


if (!document['_vaadintheme_oxide_componentCss']) {
  registerStyles(
        'vaadin-app-layout',
        unsafeCSS(vaadinAppLayoutCss.toString())
      );
      
  document['_vaadintheme_oxide_componentCss'] = true;
}

if (import.meta.hot) {
  import.meta.hot.accept((module) => {
    window.location.reload();
  });
}

