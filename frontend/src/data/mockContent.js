export const homeFeatures = [
  {
    title: '情绪陪伴',
    text: '用轻柔的引导语和沉静的界面，帮助用户在开口之前先安定下来。',
  },
  {
    title: '纯音乐舒缓',
    text: '围绕专注、睡前、舒压与独处场景，提供纯音乐推荐与沉浸式播放体验。',
  },
  {
    title: '语音倾诉',
    text: '保留说话这一最自然的表达方式，让情绪表达不像填写表单那样生硬。',
  },
  {
    title: '冥想延展',
    text: '未来可从服务页无缝进入冥想室与个人空间，形成持续陪伴的路径。',
  },
]

export const homeScenes = [
  '独处时需要一点被接住的感觉',
  '高压之后想用音乐慢慢放松',
  '睡前想整理情绪，不想再刷信息流',
  '专注工作前，需要一个安静的过渡仪式',
]

export const musicCategories = [
  {
    id: 'recommend',
    name: '今日推荐20首',
    description: '为你精选的今日放松旋律。',
    tracks: [
      {
        id: 'night-1',
        title: '暮光落在窗边',
        artist: 'Calm Echo Studio',
        duration: 196,
        cover: '/images/feature-img-1.jpg',
      },
      {
        id: 'night-2',
        title: '月影轻舟',
        artist: 'Still Water Ensemble',
        duration: 228,
        cover: '/images/feature-img-2.jpg',
      },
      {
        id: 'night-3',
        title: '云层之后的呼吸',
        artist: 'Nocturne Field',
        duration: 184,
        cover: '/images/feature-img-3.jpg',
      },
    ],
  },
  {
    id: 'guess',
    name: '猜你喜欢',
    description: '根据你的收听习惯生成的专属推荐。',
    tracks: [
      {
        id: 'guess-1',
        title: '林间细雨',
        artist: 'Moss and Wind',
        duration: 212,
        cover: '/images/feature-img-4.jpg',
      },
      {
        id: 'guess-2',
        title: '青苔回音',
        artist: 'Green Hollow',
        duration: 238,
        cover: '/images/feature-img-2.jpg',
      },
    ],
  },
  {
    id: 'leaderboard',
    name: '排行榜',
    description: '最受欢迎的放松与专注音乐。',
    tracks: [
      {
        id: 'top-1',
        title: '安静醒来',
        artist: 'Morning Vessel',
        duration: 214,
        cover: '/images/feature-img-3.jpg',
      },
      {
        id: 'top-2',
        title: '湿润的清晨',
        artist: 'Forest Tape',
        duration: 205,
        cover: '/images/feature-img-1.jpg',
      },
    ],
  },
  {
    id: 'new',
    name: '新音乐',
    description: '最新收录的治愈系纯音乐。',
    tracks: [
      {
        id: 'new-1',
        title: '缓慢展开的白日',
        artist: 'Soft Horizon',
        duration: 232,
        cover: '/images/feature-img-2.jpg',
      },
      {
        id: 'new-2',
        title: '纸页与风',
        artist: 'Quiet Routine',
        duration: 246,
        cover: '/images/feature-img-4.jpg',
      },
    ],
  },
  {
    id: 'favorite',
    name: '我喜欢',
    description: '你标记为喜欢的治愈旋律。',
    tracks: [
      {
        id: 'fav-1',
        title: '星空下的呢喃',
        artist: 'Night Whisper',
        duration: 210,
        cover: '/images/feature-img-1.jpg',
      },
      {
        id: 'fav-2',
        title: '晨露',
        artist: 'Morning Dew',
        duration: 185,
        cover: '/images/feature-img-3.jpg',
      },
    ],
  },
  {
    id: 'collection',
    name: '我收藏',
    description: '你收藏的专属歌单与氛围音。',
    tracks: [
      {
        id: 'col-1',
        title: '深海回音',
        artist: 'Deep Ocean',
        duration: 320,
        cover: '/images/feature-img-4.jpg',
      },
      {
        id: 'col-2',
        title: '壁炉边的猫',
        artist: 'Cozy Winter',
        duration: 275,
        cover: '/images/feature-img-2.jpg',
      },
    ],
  }
]

export const initialMessages = [
  {
    id: 'msg-1',
    role: 'assistant',
    content: '晚上好。这里可以先听一会儿音乐，也可以直接说说你现在的感受。',
    timestamp: '刚刚',
  },
  {
    id: 'msg-2',
    role: 'assistant',
    content: '如果你还没有想好怎么表达，也没关系。我们可以从一句最简单的话开始。',
    timestamp: '刚刚',
  },
]

export const mockReplies = [
  '我听见你在努力把情绪说清楚，这本身已经很不容易了。',
  '先不用急着解决一切，我们可以先把心绪放慢一点。',
  '你的感受是有重量的，值得被温柔地接住。',
  '如果愿意，我们可以继续把这句话往下展开一点。',
  '此刻先让呼吸稳下来，再决定接下来要不要进入冥想室。',
]
