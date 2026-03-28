import axios from 'axios';
import { env } from '../lib/env';

export const apiClient = axios.create({
    baseURL: env.apiGatewayUrl,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

apiClient.interceptors.request.use(
    (config) => {
    
        const activeNodeId = localStorage.getItem('ACTIVE_NODE_ID') || 'bootstrap';
        

        const routePrefix = activeNodeId === 'bootstrap' 
            ? '/api/bootstrap' 
            : `/api/peer-${activeNodeId}`;
            
        config.url = `${routePrefix}${config.url}`;

    
        // config.headers['Authorization'] = `Bearer ${token}`;

        return config;
    },
    (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('Communication failure with the infrastructure:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);