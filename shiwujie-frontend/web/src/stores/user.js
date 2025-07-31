import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    currentUser: null,
    currentVolunteer: null,
    token: localStorage.getItem('token') || '',
    isLoggedIn: false
  }),

  getters: {
    userInfo: (state) => state.currentUser,
    volunteerInfo: (state) => state.currentVolunteer,
    hasToken: (state) => !!state.token
  },

  actions: {
    setToken(token) {
      this.token = token
      this.isLoggedIn = true
      localStorage.setItem('token', token)
    },

    setUser(userData) {
      this.currentUser = userData
    },

    setVolunteer(volunteerData) {
      this.currentVolunteer = volunteerData
    },

    logout() {
      this.currentUser = null
      this.currentVolunteer = null
      this.token = ''
      this.isLoggedIn = false
      localStorage.removeItem('token')
    }
  }
}) 