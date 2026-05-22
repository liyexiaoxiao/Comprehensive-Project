import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { restoreSession } from './api/session'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/base.css'
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faPlay,
  faPause,
  faBackwardStep,
  faForwardStep,
  faVolumeHigh,
  faVolumeXmark,
  faAngleLeft,
  faAngleRight,
  faClockRotateLeft,
  faMicrophone,
  faCircleInfo,
  faXmark,
  faTimes,
  faMusic,
  faUsers,
  faUserFriends,
  faUser,
  faCamera,
  faIdCard,
  faBook,
  faChartLine,
  faLeaf,
  faHeart,
  faStar,
  faFolderPlus,
} from '@fortawesome/free-solid-svg-icons'

library.add(
  faPlay,
  faPause,
  faBackwardStep,
  faForwardStep,
  faVolumeHigh,
  faVolumeXmark,
  faAngleLeft,
  faAngleRight,
  faClockRotateLeft,
  faMicrophone,
  faCircleInfo,
  faXmark,
  faTimes,
  faMusic,
  faUsers,
  faUserFriends,
  faUser,
  faCamera,
  faIdCard,
  faBook,
  faChartLine,
  faLeaf,
  faHeart,
  faStar,
  faFolderPlus,
)

const app = createApp(App)
const pinia = createPinia()
app.component('font-awesome-icon', FontAwesomeIcon)

const bootstrap = async () => {
  try {
    await restoreSession({ force: true })
  } catch {
    // Ignore invalid or expired local session and let router handle guest flow.
  }

  app.use(router)
  app.use(ElementPlus)
  app.use(pinia)
  app.mount('#app')
}

bootstrap()
