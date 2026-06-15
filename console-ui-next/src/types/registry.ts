export interface AgentKitMetadata {
  namespace?: string;
  name: string;
  displayName?: string;
  version?: string;
  status?: string;
  visibility?: string;
  owner?: string;
  labels?: Record<string, string>;
  annotations?: Record<string, string>;
  resourceVersion?: string;
  generation?: number;
  digest?: string;
}

export interface AgentKitResourceRef {
  path: string;
  contentType?: string;
  digest?: string;
  encoding?: string;
  content?: string;
}

export interface AgentKitSkillSpec {
  description: string;
  license?: string;
  compatibility?: string;
  allowedTools?: string[];
  body?: string;
  resources?: AgentKitResourceRef[];
  skillSet?: string;
  groups?: string[];
  keywords?: string[];
  modelName?: string;
  modelDescription?: string;
  matchHint?: string;
  activation?: string;
  priority?: number;
}

export interface AgentKitSkill {
  apiVersion: 'agentkit.ai/v1alpha1';
  kind: 'Skill';
  metadata: AgentKitMetadata;
  spec: AgentKitSkillSpec;
}

export interface AgentKitSkillRef {
  name: string;
  version?: string;
  label?: string;
  activation?: string;
  refreshPolicy?: string;
  required?: boolean;
  order?: number;
}

export interface AgentKitToolRef {
  type?: 'builtin' | 'mcp' | 'function' | string;
  name?: string;
  server?: string;
  toolFilter?: string[];
  args?: Record<string, unknown>;
}

export interface AgentKitSubAgentRef {
  id: string;
  ref: AgentKitSkillRef;
  invocation?: Record<string, unknown>;
  context?: Record<string, unknown>;
  workspace?: Record<string, unknown>;
  output?: Record<string, unknown>;
}

export interface AgentKitAgentSpec {
  agentClass?: string;
  description?: string;
  modelRef?: string;
  instruction?: string;
  generationConfig?: Record<string, unknown>;
  skills?: AgentKitSkillRef[];
  tools?: AgentKitToolRef[];
  subAgents?: AgentKitSubAgentRef[];
  runtime?: Record<string, unknown>;
}

export interface AgentKitAgent {
  apiVersion: 'agentkit.ai/v1alpha1';
  kind: 'Agent';
  metadata: AgentKitMetadata;
  spec: AgentKitAgentSpec;
}

export interface AgentKitRuntimeManifest {
  agent: AgentKitAgent;
  skills: AgentKitSkill[];
  resourceVersion?: string;
  digest?: string;
}
