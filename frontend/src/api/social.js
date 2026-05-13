import http from './http'

// --- Mood Diaries ---
export const getMyMoodDiariesApi = (date) => {
  const params = date ? { date } : {}
  return http.get('/api/social/v1/me/mood-diaries', { params })
}
export const createMoodDiaryApi = (payload) => http.post('/api/social/v1/me/mood-diaries', payload)
export const updateMoodDiaryApi = (diaryId, payload) => http.put(`/api/social/v1/me/mood-diaries/${diaryId}`, payload)
export const deleteMoodDiaryApi = (diaryId) => http.delete(`/api/social/v1/me/mood-diaries/${diaryId}`)

// --- Social Posts ---
export const getPostsApi = (page = 0, size = 20) => http.get('/api/social/v1/posts', { params: { page, size } })
export const getMyPostsApi = (page = 0, size = 20) => http.get('/api/social/v1/me/posts', { params: { page, size } })
export const createPostApi = (payload) => http.post('/api/social/v1/me/posts', payload)
export const updatePostApi = (postId, payload) => http.put(`/api/social/v1/me/posts/${postId}`, payload)
export const deletePostApi = (postId) => http.delete(`/api/social/v1/me/posts/${postId}`)

// --- Interactions ---
export const likePostApi = (postId) => http.post(`/api/social/v1/posts/${postId}/like`)
export const commentPostApi = (postId, payload) => http.post(`/api/social/v1/posts/${postId}/comments`, payload)
export const deleteInteractionApi = (interactionId) => http.delete(`/api/social/v1/me/interactions/${interactionId}`)
