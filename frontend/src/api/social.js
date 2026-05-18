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
export const getPostInteractionsApi = (postId) => http.get(`/api/social/v1/posts/${postId}/interactions`)
export const createPostApi = (payload) => http.post('/api/social/v1/me/posts', payload)
export const updatePostApi = (postId, payload) => http.put(`/api/social/v1/me/posts/${postId}`, payload)
export const deletePostApi = (postId) => http.delete(`/api/social/v1/me/posts/${postId}`)

// --- Interactions ---
export const likePostApi = (postId) => http.post(`/api/social/v1/posts/${postId}/like`)
export const commentPostApi = (postId, payload) => http.post(`/api/social/v1/posts/${postId}/comments`, payload)
export const likeCommentApi = (postId, commentId) => http.post(`/api/social/v1/posts/${postId}/comments/${commentId}/like`)
export const replyCommentApi = (postId, commentId, payload) => http.post(`/api/social/v1/posts/${postId}/comments/${commentId}/replies`, payload)
export const getMySocialNotificationsApi = () => http.get('/api/social/v1/me/social-notifications')
export const deleteInteractionApi = (interactionId) => http.delete(`/api/social/v1/me/interactions/${interactionId}`)

// --- Friends ---
export const getMyFriendsApi = () => http.get('/api/social/v1/me/friends')
export const addFriendApi = (payload) => http.post('/api/social/v1/me/friends', payload)
export const updateFriendIntimacyApi = (friendshipId, payload) => http.put(`/api/social/v1/me/friends/${friendshipId}/intimacy`, payload)
export const deleteFriendshipApi = (friendshipId) => http.delete(`/api/social/v1/me/friends/${friendshipId}`)

// --- Friend Requests ---
export const searchUsersApi = (keyword) => http.get('/api/users/search', { params: { q: keyword } })
export const sendFriendRequestApi = (payload) => http.post('/api/social/v1/me/friend-requests', payload)
export const getReceivedFriendRequestsApi = () => http.get('/api/social/v1/me/friend-requests/received')
export const handleFriendRequestApi = (requestId, payload) => http.put(`/api/social/v1/me/friend-requests/${requestId}`, payload)

// --- Friend Chat ---
export const getChatConversationsApi = () => http.get('/api/social/v1/me/chat/conversations')
export const getChatMessagesApi = (peerUserId, params) =>
  http.get(`/api/social/v1/me/chat/with/${peerUserId}/messages`, { params })
export const sendChatMessageApi = (peerUserId, payload) =>
  http.post(`/api/social/v1/me/chat/with/${peerUserId}/messages`, payload)
export const markChatAsReadApi = (peerUserId) =>
  http.put(`/api/social/v1/me/chat/with/${peerUserId}/read`)
