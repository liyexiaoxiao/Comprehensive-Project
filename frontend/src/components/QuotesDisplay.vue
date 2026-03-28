<template>
    <div class="quote-box">
        <p v-for="(line, index) in currentQuote" :key="index">
            <span class="zh">{{ line.zh }}</span><br/>
            <span class="en">{{ line.en }}</span>
        </p>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getRandomQuote } from '@/utils/quotes'

const currentQuote = ref({ zh: '', en: '' });

const updateQuote = () => {
    currentQuote.value = getRandomQuote();
};

onMounted(() => {
    updateQuote();
    setInterval(updateQuote, 15000);  // 每15s更新
});
</script>

<style scoped>
.quote-box {
    padding: 20px;
    font-size: 1.8rem;
    color: var(--color-text-primary);
    animation: fadeIn 1s ease-in;
    max-width: 500px;
    margin: auto;
    text-align: center;
}

.zh {
    font-family: var(--font-sans);
    font-weight: 500;
    font-style: normal; /* 中文正常体 */
}

.en {
    font-family: var(--font-serif);
    font-weight: 400;
    font-style: italic;
    color: var(--color-text-secondary);
    margin-top: 0.3em;
    display: block;
    font-size: 1.4rem;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>