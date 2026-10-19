/** 后端统一响应体 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页返回结构（P1 起使用） */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}
