import axiosInstance from '../axiosConfig';

export const AuthService = {

    logout: async () => {
        const response = await axiosInstance.post('/auth/logout');
        return response.data;
    },

    currentUser : async () => {
        const response = await axiosInstance.get('/auth/user');
        return response.data;
    },

    updatePassword: async () => {
        const response = await axiosInstance.post('/auth/change-password');
        return response.data;
    }
}