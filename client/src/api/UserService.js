import axiosInstance from '../axiosConfig';

export const UserService = {

        addUser: async (id) => {
            await axiosInstance.post('/users/', {id});
        },
}