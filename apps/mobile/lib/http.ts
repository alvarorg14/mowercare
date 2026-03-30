import { getApiBaseUrl } from './config';

export type ProblemBody = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
};

export class ApiProblemError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly title?: string;
  readonly detail?: string;
  readonly instance?: string;
  readonly type?: string;

  constructor(status: number, problem: ProblemBody) {
    super(problem.detail || problem.title || `HTTP ${status}`);
    this.name = 'ApiProblemError';
    this.status = status;
    this.code = problem.code;
    this.title = problem.title;
    this.detail = problem.detail;
    this.instance = problem.instance;
    this.type = problem.type;
  }
}

/** Login, refresh, logout — no Bearer header. */
export async function fetchWithoutAuth<T>(method: string, path: string, body?: unknown): Promise<T> {
  const url = `${getApiBaseUrl()}${path}`;
  const res = await fetch(url, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let parsed: unknown;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = undefined;
    }
  }

  if (!res.ok) {
    const problem = parsed as ProblemBody | undefined;
    if (problem && typeof problem === 'object' && ('code' in problem || 'title' in problem)) {
      throw new ApiProblemError(res.status, problem);
    }
    throw new ApiProblemError(res.status, {
      title: 'Request failed',
      detail: text || res.statusText,
      status: res.status,
    });
  }

  return parsed as T;
}
