import http from './http'

export const registerApi = (payload) => http.post('/api/users/register', payload)

export const loginApi = (payload) => http.post('/api/users/login', payload)

export const getMeApi = () => http.get('/api/users/me')
