<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goToMeditation() {
  router.push('/meditation')
}
</script>

<template>
  <div style="text-align:center; margin-top: 100px;">
    <h1>Welcome to Home Page</h1>
    <button @click="goToMeditation" style="padding: 10px 20px; font-size: 16px;">
      Go to Meditation Page
    </button>
  </div>
</template>

<style scoped>
button {
  cursor: pointer;
  border: none;
  background-color: #42b983;
  color: white;
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #36976f;
}
</style>
