export const quotes = [
    [
        { zh: "你所担心的事情，99%都不会发生。", en: "99% of the things you worry about never happen." },
        { zh: "呼吸，感受当下。", en: "Breathe and feel the present moment." },
        { zh: "没有风暴能永远持续。", en: "No storm lasts forever." }
    ],
    [
        { zh: "你已经做得很好了。", en: "You have done very well." },
        { zh: "慢慢来，比较快。", en: "Slow and steady wins the race." }
    ],
    [
        { zh: "希望是长着羽翼的生灵。", en: "Hope is the thing with feathers." },
        { zh: "栖居在灵魂深处。", en: "That perches in the soul," },
        { zh: "狂风纵然猛烈，", en: "And sore must be the storm" },
        { zh: "也难摧毁那温暖众生的小小生命。", en: "That could abash the little bird That kept so many warm." }
    ]
];

export const getRandomQuote = () => {
    return quotes[Math.floor(Math.random() * quotes.length)];
};