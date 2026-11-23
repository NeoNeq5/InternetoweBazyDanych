import type { RouteDto } from "@/api/routes"
import type { StopDto } from "@/api/stops"

export type DirectionOption = {
  directionId: number
  headsign: string
}

export type StopRouteDto = RouteDto & {
  directions: DirectionOption[]
}

export type DepartureDto = {
  tripId: string
  headsign: string
  departureTime: string
  minutesUntil: number
}

export async function fetchRoutesForStop(
  stopId: string,
  signal?: AbortSignal
): Promise<StopRouteDto[]> {
  const response = await fetch(
    `/api/stops/${encodeURIComponent(stopId)}/routes`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać linii przystanku (${response.status})`)
  }
  return response.json()
}

export async function fetchStopsForRouteDirection(
  routeId: string,
  directionId: number,
  signal?: AbortSignal
): Promise<StopDto[]> {
  const response = await fetch(
    `/api/routes/${encodeURIComponent(routeId)}/stops?directionId=${directionId}`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać przystanków linii (${response.status})`)
  }
  return response.json()
}

export async function fetchDepartures(
  stopId: string,
  routeId: string,
  directionId: number,
  signal?: AbortSignal
): Promise<DepartureDto[]> {
  const params = new URLSearchParams({
    routeId,
    directionId: String(directionId),
    limit: "12",
  })
  const response = await fetch(
    `/api/stops/${encodeURIComponent(stopId)}/departures?${params}`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać odjazdów (${response.status})`)
  }
  return response.json()
}
