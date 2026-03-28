import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/base.css'
import { library } from '@fortawesome/fontawesome-svg-core';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faPlay, faPause, faBackwardStep, faForwardStep, faVolumeHigh, faVolumeXmark, faAngleLeft, faClockRotateLeft, faMicrophone } from '@fortawesome/free-solid-svg-icons';

library.add(faPlay, faPause, faBackwardStep, faForwardStep, faVolumeHigh, faVolumeXmark, faAngleLeft, faClockRotateLeft, faMicrophone);

const app = createApp(App);
const pinia = createPinia();
app.component('font-awesome-icon', FontAwesomeIcon);
app.use(router);
app.use(ElementPlus);
app.use(pinia);
app.mount('#app');