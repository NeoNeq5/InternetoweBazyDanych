import { useEffect, useMemo, useState, type FormEvent } from "react"

import { TransitMap } from "@/components/transit-map"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { NativeSelect } from "@/components/ui/input"
import {
  fetchDepartures,
  fetchRoutesForStop,
  fetchStopsForRouteDirection,
  type DepartureDto,
  type DirectionOption,
  type StopRouteDto,
} from "@/api/network"
import { fetchRouteShapes, type RouteShapeDto } from "@/api/shapes"
import type { StopDto } from "@/api/stops"
import { normalizeQuery } from "@/lib/normalize"
import { useAccount } from "@/state/account"
import { useRoutes } from "@/state/routes"
import { useStops } from "@/state/stops"
import { cn } from "cn"

export function StopsPage() {
  const { favoriteStopIds, toggleFavorite, search } = useAccount()
  const { routes } = useRoutes()
  const { stops, loading, error, stopById } = useStops()

  const [query, setQuery] = useState("")
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [selectedRouteId, setSelectedRouteId] = useState("")
  const [directionId, setDirectionId] = useState<number | null>(null)

  const [stopRoutes, setStopRoutes] = useState<StopRouteDto[]>([])
  const [routeStops, setRouteStops] = useState<StopDto[]>([])
  const [allShapes, setAllShapes] = useState<RouteShapeDto[]>([])
  const [departures, setDepartures] = useState<DepartureDto[]>([])
  const [panelError, setPanelError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const selectedStop = selectedStopId ? stopById(selectedStopId) : undefined
  const selectedRoute =
    routes.find((route) => route.id === selectedRouteId) ??
    stopRoutes.find((route) => route.id === selectedRouteId) ??
    null

  const directions: DirectionOption[] = useMemo(() => {
    const fromStop = stopRoutes.find((route) => route.id === selectedRouteId)
    if (fromStop) {
      return fromStop.directions
    }
    return allShapes.map((shape) => ({
      directionId: shape.directionId,
      headsign: `Kierunek ${shape.directionId}`,
    }))
  }, [allShapes, selectedRouteId, stopRoutes])

  const activeDirection =
    directionId != null
      ? directionId
      : directions[0]?.directionId != null
        ? directions[0].directionId
        : null

  const visibleShapes = useMemo(() => {
    if (activeDirection == null) {
      return []
    }
    return allShapes.filter((shape) => shape.directionId === activeDirection)
  }, [activeDirection, allShapes])

  const needle = normalizeQuery(query.trim())
  const matchedStops = useMemo(() => {
    if (!needle) {
      return []
    }
    return stops
      .filter((stop) => normalizeQuery(stop.name).includes(needle))
      .slice(0, 40)
  }, [needle, stops])

  const mapStops = useMemo(() => {
    if (selectedRouteId && activeDirection != null && routeStops.length > 0) {
      return routeStops
    }
    return stops
  }, [activeDirection, routeStops, selectedRouteId, stops])

  useEffect(() => {
    if (!selectedStopId) {
      setStopRoutes([])
      return
    }
    const controller = new AbortController()
    setBusy(true)
    setPanelError(null)
    fetchRoutesForStop(selectedStopId, controller.signal)
      .then((data) => {
        setStopRoutes(data)
        setBusy(false)
        if (
          selectedRouteId &&
          !data.some((route) => route.id === selectedRouteId)
        ) {
          // keep globally chosen route even if not in stop list yet
        } else if (!selectedRouteId && data.length === 1) {
          setSelectedRouteId(data[0].id)
          setDirectionId(data[0].directions[0]?.directionId ?? null)
        }
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setBusy(false)
        setPanelError(
          err instanceof Error ? err.message : "Błąd pobierania linii"
        )
      })
    return () => controller.abort()
  }, [selectedStopId])

  useEffect(() => {
    if (!selectedRouteId) {
      setAllShapes([])
      setRouteStops([])
      setDepartures([])
      return
    }
    const controller = new AbortController()
    setBusy(true)
    setPanelError(null)
    fetchRouteShapes(selectedRouteId, controller.signal)
      .then((shapes) => {
        setAllShapes(shapes)
        setBusy(false)
        setDirectionId((current) => {
          if (current != null && shapes.some((s) => s.directionId === current)) {
            return current
          }
          return shapes[0]?.directionId ?? null
        })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setBusy(false)
        setAllShapes([])
        setPanelError(
          err instanceof Error ? err.message : "Błąd pobierania przebiegu"
        )
      })
    return () => controller.abort()
  }, [selectedRouteId])

  useEffect(() => {
    if (!selectedRouteId || activeDirection == null) {
      setRouteStops([])
      return
    }
    const controller = new AbortController()
    fetchStopsForRouteDirection(
      selectedRouteId,
      activeDirection,
      controller.signal
    )
      .then(setRouteStops)
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setRouteStops([])
        setPanelError(
          err instanceof Error ? err.message : "Błąd przystanków linii"
        )
      })
    return () => controller.abort()
  }, [activeDirection, selectedRouteId])

  useEffect(() => {
    if (!selectedStopId || !selectedRouteId || activeDirection == null) {
      setDepartures([])
      return
    }
    const controller = new AbortController()
    fetchDepartures(
      selectedStopId,
      selectedRouteId,
      activeDirection,
      controller.signal
    )
      .then(setDepartures)
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setDepartures([])
        setPanelError(
          err instanceof Error ? err.message : "Błąd pobierania odjazdów"
        )
      })
    return () => controller.abort()
  }, [activeDirection, selectedRouteId, selectedStopId])

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = query.trim()
    search(trimmed)
    const match = stops.find((stop) =>
      normalizeQuery(stop.name).includes(normalizeQuery(trimmed))
    )
    if (match) {
      setSelectedStopId(match.id)
    }
  }

  function selectRoute(routeId: string) {
    setSelectedRouteId(routeId)
    const fromStop = stopRoutes.find((route) => route.id === routeId)
    setDirectionId(fromStop?.directions[0]?.directionId ?? null)
  }

  function onPickStop(stopId: string) {
    setSelectedStopId(stopId)
  }

  return (
    <div className="lg:grid lg:min-h-[calc(100svh-6.75rem)] lg:grid-cols-12">
      <section className="flex flex-col border-b lg:col-span-4 lg:border-r lg:border-b-0">
        <form
          className="flex flex-col gap-3 border-b px-4 py-4 lg:px-5"
          onSubmit={onSubmit}
        >
          <div>
            <h1 className="text-lg font-medium">Mapa sieci</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Wybierz przystanek albo linię. Po wyborze linii widać tylko
              przystanki wybranego kierunku.
            </p>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="stop-query">Przystanek</Label>
            <Input
              id="stop-query"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Plac Grunwaldzki"
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="route-pick">Linia</Label>
            <NativeSelect
              id="route-pick"
              value={selectedRouteId}
              onChange={(event) => selectRoute(event.target.value)}
            >
              <option value="">Wybierz linię</option>
              {routes.map((route) => (
                <option key={route.id} value={route.id}>
                  {route.shortName} · {route.longName}
                </option>
              ))}
            </NativeSelect>
          </div>

          <Button type="submit" className="h-10">
            Szukaj przystanku
          </Button>

          {loading || busy ? (
            <p className="text-sm text-muted-foreground">Ładowanie danych…</p>
          ) : null}
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          {panelError ? (
            <p className="text-sm text-destructive">{panelError}</p>
          ) : null}
        </form>

        {matchedStops.length > 0 ? (
          <ul className="max-h-40 overflow-y-auto border-b">
            {matchedStops.map((stop) => (
              <li key={stop.id}>
                <button
                  type="button"
                  className={cn(
                    "flex w-full items-baseline justify-between gap-3 px-4 py-2.5 text-left text-sm hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none lg:px-5",
                    selectedStopId === stop.id && "bg-muted"
                  )}
                  onClick={() => onPickStop(stop.id)}
                >
                  <span className="font-medium">{stop.name}</span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}

        <div className="space-y-5 overflow-y-auto px-4 py-4 lg:px-5">
          {favoriteStopIds.length > 0 ? (
            <div>
              <h2 className="text-sm font-medium">Ulubione</h2>
              <ul className="mt-2">
                {favoriteStopIds.map((id) => {
                  const stop = stopById(id)
                  if (!stop) {
                    return null
                  }
                  return (
                    <li key={id}>
                      <button
                        type="button"
                        className={cn(
                          "py-1 text-sm hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none",
                          selectedStopId === id && "font-medium"
                        )}
                        onClick={() => onPickStop(id)}
                      >
                        {stop.name}
                      </button>
                    </li>
                  )
                })}
              </ul>
            </div>
          ) : null}

          {selectedStop ? (
            <div>
              <h2 className="text-base font-medium">{selectedStop.name}</h2>
              <p className="mt-1 text-sm text-muted-foreground tabular-nums">
                {selectedStop.lat.toFixed(5)} N, {selectedStop.lon.toFixed(5)} E
              </p>
              <Button
                variant="outline"
                className="mt-3 h-9"
                onClick={() => toggleFavorite(selectedStop.id)}
              >
                {favoriteStopIds.includes(selectedStop.id)
                  ? "Usuń z ulubionych"
                  : "Dodaj do ulubionych"}
              </Button>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              Kliknij przystanek na mapie albo wyszukaj nazwę.
            </p>
          )}

          {selectedStop && stopRoutes.length > 0 ? (
            <div>
              <h3 className="text-sm font-medium">Linie na tym przystanku</h3>
              <ul className="mt-2 divide-y border-y">
                {stopRoutes.map((route) => (
                  <li key={route.id}>
                    <button
                      type="button"
                      className={cn(
                        "flex w-full items-baseline justify-between gap-3 py-2 text-left text-sm hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none",
                        selectedRouteId === route.id && "bg-muted font-medium"
                      )}
                      onClick={() => selectRoute(route.id)}
                    >
                      <span>
                        {route.shortName}{" "}
                        <span className="text-muted-foreground">
                          {route.vehicleType === "TRAM" ? "tramwaj" : "autobus"}
                        </span>
                      </span>
                      <span className="truncate text-xs text-muted-foreground">
                        {route.longName}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {selectedRouteId && directions.length > 0 ? (
            <div>
              <h3 className="text-sm font-medium">Kierunek</h3>
              <div className="mt-2 flex flex-col gap-1">
                {directions.map((direction) => (
                  <Button
                    key={direction.directionId}
                    variant={
                      activeDirection === direction.directionId
                        ? "default"
                        : "outline"
                    }
                    className="h-9 justify-start"
                    onClick={() => setDirectionId(direction.directionId)}
                  >
                    {direction.headsign}
                  </Button>
                ))}
              </div>
            </div>
          ) : null}

          {selectedStop && selectedRoute && activeDirection != null ? (
            <div>
              <h3 className="text-sm font-medium">
                Odjazdy · linia {selectedRoute.shortName}
              </h3>
              {departures.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">
                  Brak zaplanowanych odjazdów na dziś dla tego kierunku.
                </p>
              ) : (
                <ul className="mt-2 divide-y border-y">
                  {departures.map((item) => (
                    <li
                      key={`${item.tripId}-${item.departureTime}`}
                      className="flex items-baseline justify-between gap-3 py-2 text-sm"
                    >
                      <div>
                        <p className="font-medium tabular-nums">
                          {item.departureTime}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {item.headsign || "Bez kierunku"}
                        </p>
                      </div>
                      <p className="tabular-nums text-muted-foreground">
                        {item.minutesUntil <= 0
                          ? "teraz"
                          : `za ${item.minutesUntil} min`}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ) : null}
        </div>
      </section>

      <section className="relative lg:col-span-8">
        <div className="h-[28rem] lg:absolute lg:inset-0 lg:h-auto">
          <TransitMap
            stops={mapStops}
            highlightedStopId={selectedStopId}
            onSelectStop={onPickStop}
            shapes={visibleShapes}
            route={selectedRoute}
          />
        </div>
      </section>
    </div>
  )
}
