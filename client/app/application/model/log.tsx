export interface LogPayload {
  fileName: string;
  linesReturned: number;
  nextOffset: number
  data: string[];
}