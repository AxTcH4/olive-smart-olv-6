import type { ApiError } from "./api-error";

export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
  timestamp: string;
  error: ApiError | null;
}