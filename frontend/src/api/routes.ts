export type VehicleType = "TRAM" | "BUS"

export type RouteDto = {
  id: string
  shortName: string
  longName: string
  vehicleType: VehicleType
}

export async function fetchRoutes(signal?: AbortSignal): Promise<RouteDto[]> {
  const response = await fetch("/api/routes", { signal })
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać tras (${response.status})`)
  }
  return response.json()
}
