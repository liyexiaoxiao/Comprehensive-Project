import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSpeechStore = defineStore('speech', () => {
    const emotion = ref('')

    function setEmotion(newEmotion) {
        emotion.value = newEmotion
    }

    return {
        emotion,
        setEmotion
    }
})
