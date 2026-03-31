import axios from 'axios';
import { env } from './env';

export const apiClient = axios.create({
    baseURL: env.apiGatewayUrl,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

apiClient.interceptors.request.use(
    (config) => {
        let targetNode = 'bootstrap';
        
        if (config.headers) {
            const rawHeader = (typeof config.headers.get === 'function' ? config.headers.get('X-Target-Node') : null)
                           || (config.headers as Record<string, any>)['x-target-node']
                           || (config.headers as Record<string, any>)['X-Target-Node'];
            
            if (rawHeader) {
                targetNode = rawHeader.toString();
            }

            if (typeof config.headers.delete === 'function') {
                config.headers.delete('X-Target-Node');
            } else {
                delete (config.headers as Record<string, any>)['x-target-node'];
                delete (config.headers as Record<string, any>)['X-Target-Node'];
            }
        }

        const routePrefix = targetNode === 'bootstrap' 
            ? '/api/bootstrap' 
            : `/api/peer-${targetNode}`;
            
        config.url = `${routePrefix}${config.url}`;

        return config;
    },
    (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('[AXIOS] Falha de comunicação com a infraestrutura:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);