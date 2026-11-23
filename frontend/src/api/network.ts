import type { RouteDto, VehicleType } from "@/api/routes"
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
  routeId: string
  routeShortName: string
  vehicleType: VehicleType
  directionId: number
  headsign: string
  departureTime: string
  minutesUntil: number
}

export type TripStopDto = {
  stopSequence: number
  stopId: string
  stopName: string
  lat: number
  lon: number
  arrivalTime: string
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
  options?: {
    routeId?: string
    directionId?: number | null
    limit?: number
  },
  signal?: AbortSignal
): Promise<DepartureDto[]> {
  const params = new URLSearchParams()
  if (options?.routeId) {
    params.set("routeId", options.routeId)
    if (options.directionId != null) {
      params.set("directionId", String(options.directionId))
    }
  }
  params.set("limit", String(options?.limit ?? (options?.routeId ? 12 : 20)))

  const response = await fetch(
    `/api/stops/${encodeURIComponent(stopId)}/departures?${params}`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać odjazdów (${response.status})`)
  }
  return response.json()
}

export async function fetchTripStops(
  tripId: string,
  signal?: AbortSignal
): Promise<TripStopDto[]> {
  const response = await fetch(
    `/api/trips/${encodeURIComponent(tripId)}/stops`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(
      `Nie udało się pobrać przystanków przejazdu (${response.status})`
    )
  }
  return response.json()
}
