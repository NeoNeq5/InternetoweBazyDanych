export type RouteShapeDto = {
  directionId: number
  points: [number, number][]
}

export async function fetchRouteShapes(
  routeId: string,
  signal?: AbortSignal
): Promise<RouteShapeDto[]> {
  const response = await fetch(
    `/api/routes/${encodeURIComponent(routeId)}/shapes`,
    { signal }
  )
  if (!response.ok) {
    throw new Error(`Nie udało się pobrać przebiegu linii (${response.status})`)
  }
  return response.json()
}
