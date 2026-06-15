import client from './client';
import type { ApiResult } from './types';
import type {
  AgentKitAgent,
  AgentKitRuntimeManifest,
  AgentKitSkill,
} from '@/types/registry';

const BASE = 'v3/console/ai/registry';
const CLIENT_BASE = 'v3/client/ai/registry';

export const registryApi = {
  getSkill: (params: {
    namespaceId?: string;
    name: string;
    version?: string;
    label?: string;
  }): ApiResult<AgentKitSkill> => {
    const { name, ...query } = params;
    return client.get(`${BASE}/skills/${encodeURIComponent(name)}`, { params: query }) as ApiResult<AgentKitSkill>;
  },

  createSkill: (data: AgentKitSkill, params?: {
    namespaceId?: string;
    publish?: boolean;
    force?: boolean;
  }): ApiResult<string> => client.post(`${BASE}/skills`, data, { params }) as ApiResult<string>,

  updateSkill: (name: string, data: AgentKitSkill, params?: {
    namespaceId?: string;
    publish?: boolean;
    force?: boolean;
    ifMatch?: string;
  }): ApiResult<string> => {
    const { ifMatch, ...query } = params || {};
    return client.put(`${BASE}/skills/${encodeURIComponent(name)}`, data, {
      params: query,
      headers: ifMatch ? { 'If-Match': ifMatch } : undefined,
    }) as ApiResult<string>;
  },

  deleteSkill: (params: { namespaceId?: string; name: string }): ApiResult<string> => {
    const { name, ...query } = params;
    return client.delete(`${BASE}/skills/${encodeURIComponent(name)}`, { params: query }) as ApiResult<string>;
  },

  publishSkill: (params: {
    namespaceId?: string;
    name: string;
    version?: string;
    force?: boolean;
  }): ApiResult<string> => {
    const { name, ...query } = params;
    return client.post(`${BASE}/skills/${encodeURIComponent(name)}/publish`, undefined, { params: query }) as ApiResult<string>;
  },

  getAgent: (params: {
    namespaceId?: string;
    name: string;
    version?: string;
    label?: string;
  }): ApiResult<AgentKitAgent> => {
    const { name, ...query } = params;
    return client.get(`${BASE}/agents/${encodeURIComponent(name)}`, { params: query }) as ApiResult<AgentKitAgent>;
  },

  createAgent: (data: AgentKitAgent, params?: {
    namespaceId?: string;
    publish?: boolean;
    force?: boolean;
  }): ApiResult<string> => client.post(`${BASE}/agents`, data, { params }) as ApiResult<string>,

  updateAgent: (name: string, data: AgentKitAgent, params?: {
    namespaceId?: string;
    publish?: boolean;
    force?: boolean;
    ifMatch?: string;
  }): ApiResult<string> => {
    const { ifMatch, ...query } = params || {};
    return client.put(`${BASE}/agents/${encodeURIComponent(name)}`, data, {
      params: query,
      headers: ifMatch ? { 'If-Match': ifMatch } : undefined,
    }) as ApiResult<string>;
  },

  deleteAgent: (params: { namespaceId?: string; name: string }): ApiResult<string> => {
    const { name, ...query } = params;
    return client.delete(`${BASE}/agents/${encodeURIComponent(name)}`, { params: query }) as ApiResult<string>;
  },

  publishAgent: (params: {
    namespaceId?: string;
    name: string;
    version?: string;
    force?: boolean;
  }): ApiResult<string> => {
    const { name, ...query } = params;
    return client.post(`${BASE}/agents/${encodeURIComponent(name)}/publish`, undefined, { params: query }) as ApiResult<string>;
  },

  getRuntimeManifest: (params: {
    namespaceId?: string;
    name: string;
    version?: string;
    label?: string;
  }): ApiResult<AgentKitRuntimeManifest> => {
    const { name, ...query } = params;
    return client.get(`${CLIENT_BASE}/agents/${encodeURIComponent(name)}/manifest`, { params: query }) as ApiResult<AgentKitRuntimeManifest>;
  },
};
