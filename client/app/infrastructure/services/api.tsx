import axios from 'axios';


export const createService = (baseURL:any, headers = {}) => {
    const instance = axios.create({
        baseURL,
        headers: {
            'Content-Type': 'application/json',
            ...headers,
        },
    });

    instance.interceptors.response.use(
        (response) => response,
        (error) => {
            console.error('Erro na requisição:', error.response?.data || error.message);
            return Promise.reject(error);
        }
    );

    return instance;
};