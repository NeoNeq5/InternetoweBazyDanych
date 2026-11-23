export type StopDto = {
  id: string
  name: string
  lat: number
  lon: number
}

export async function fetchStops(signal?: AbortSignal): Promise<StopDto[]> {
  const response = await fetch("/api/stops", { signal })
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać przystanków (${response.status})`)
  }
  return response.json()
}
