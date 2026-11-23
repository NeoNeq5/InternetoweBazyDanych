import { useEffect, useMemo, useState, type FormEvent } from "react"
import { cn } from "cn"

import {
  fetchDepartures,
  fetchRoutesForStop,
  fetchStopsForRouteDirection,
  fetchTripStops,
  type DepartureDto,
  type DirectionOption,
  type StopRouteDto,
  type TripStopDto,
} from "@/api/network"
import { fetchRouteShapes, type RouteShapeDto } from "@/api/shapes"
import type { StopDto } from "@/api/stops"
import { TransitMap } from "@/components/transit-map"
import { Button } from "@/components/ui/button"
import { Input, NativeSelect } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { formatRemaining } from "@/lib/format"
import { normalizeQuery } from "@/lib/normalize"
import { useAccount } from "@/state/account"
import { useRoutes } from "@/state/routes"
import { useStops } from "@/state/stops"

type KeyedState<T> = {
  key: string
  items: T[]
  error: string | null
}

function emptyKeyed<T>(): KeyedState<T> {
  return { key: "", items: [], error: null }
}

function formatMinutesUntil(minutesUntil: number) {
  if (minutesUntil < 0) {
    return `${formatRemaining(Math.abs(minutesUntil) * 60_000)} temu`
  }
  if (minutesUntil === 0) {
    return "teraz"
  }
  return `za ${formatRemaining(minutesUntil * 60_000)}`
}

export function StopsPage() {
  const { favoriteStopIds, toggleFavorite, search } = useAccount()
  const { routes } = useRoutes()
  const { stops, loading, error, stopById } = useStops()

  const [query, setQuery] = useState("")
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [selectedRouteId, setSelectedRouteId] = useState("")
  const [directionId, setDirectionId] = useState<number | null>(null)
  const [selectedTrip, setSelectedTrip] = useState<DepartureDto | null>(null)

  const [stopRoutesState, setStopRoutesState] =
    useState<KeyedState<StopRouteDto>>(emptyKeyed)
  const [shapesState, setShapesState] =
    useState<KeyedState<RouteShapeDto>>(emptyKeyed)
  const [routeStopsState, setRouteStopsState] =
    useState<KeyedState<StopDto>>(emptyKeyed)
  const [departuresState, setDeparturesState] =
    useState<KeyedState<DepartureDto>>(emptyKeyed)
  const [tripStopsState, setTripStopsState] =
    useState<KeyedState<TripStopDto>>(emptyKeyed)

  const selectedStop = selectedStopId ? stopById(selectedStopId) : undefined

  const stopRoutes = useMemo(
    () =>
      selectedStopId && stopRoutesState.key === selectedStopId
        ? stopRoutesState.items
        : [],
    [selectedStopId, stopRoutesState]
  )

  const effectiveRouteId = selectedTrip ? selectedTrip.routeId : selectedRouteId

  const allShapes = useMemo(
    () =>
      effectiveRouteId && shapesState.key === effectiveRouteId
        ? shapesState.items
        : [],
    [effectiveRouteId, shapesState]
  )

  const selectedRoute =
    routes.find((route) => route.id === selectedRouteId) ??
    stopRoutes.find((route) => route.id === selectedRouteId) ??
    null

  const effectiveRoute =
    routes.find((route) => route.id === effectiveRouteId) ??
    stopRoutes.find((route) => route.id === effectiveRouteId) ??
    (selectedTrip
      ? {
          id: selectedTrip.routeId,
          shortName: selectedTrip.routeShortName,
          longName: selectedTrip.headsign,
          vehicleType: selectedTrip.vehicleType,
        }
      : null)

  const directions: DirectionOption[] = useMemo(() => {
    const fromStop = stopRoutes.find((route) => route.id === selectedRouteId)
    if (fromStop) {
      return fromStop.directions
    }
    if (!selectedRouteId) {
      return []
    }
    return allShapes.map((shape) => ({
      directionId: shape.directionId,
      headsign: `Kierunek ${shape.directionId}`,
    }))
  }, [allShapes, selectedRouteId, stopRoutes])

  const activeDirection =
    directionId != null &&
    (directions.length === 0 ||
      directions.some((dir) => dir.directionId === directionId))
      ? directionId
      : directions[0]?.directionId != null
        ? directions[0].directionId
        : null

  const effectiveDirection = selectedTrip
    ? selectedTrip.directionId
    : activeDirection

  const routeStopsKey =
    selectedRouteId && activeDirection != null
      ? `${selectedRouteId}:${activeDirection}`
      : ""

  const routeStops = useMemo(
    () =>
      routeStopsKey && routeStopsState.key === routeStopsKey
        ? routeStopsState.items
        : [],
    [routeStopsKey, routeStopsState]
  )

  const departuresKey = useMemo(() => {
    if (!selectedStopId) {
      return ""
    }
    if (selectedRouteId && activeDirection != null) {
      return `${selectedStopId}|${selectedRouteId}|${activeDirection}`
    }
    if (selectedRouteId) {
      return `${selectedStopId}|${selectedRouteId}|all`
    }
    return `${selectedStopId}|all`
  }, [activeDirection, selectedRouteId, selectedStopId])

  const departures = useMemo(
    () =>
      departuresKey && departuresState.key === departuresKey
        ? departuresState.items
        : [],
    [departuresKey, departuresState]
  )

  const tripStops = useMemo(
    () =>
      selectedTrip && tripStopsState.key === selectedTrip.tripId
        ? tripStopsState.items
        : [],
    [selectedTrip, tripStopsState]
  )

  const tripStopsLoading =
    Boolean(selectedTrip) && tripStopsState.key !== selectedTrip?.tripId

  const busy =
    (Boolean(selectedStopId) && stopRoutesState.key !== selectedStopId) ||
    (Boolean(effectiveRouteId) && shapesState.key !== effectiveRouteId) ||
    (Boolean(departuresKey) && departuresState.key !== departuresKey) ||
    tripStopsLoading

  const panelError =
    (selectedStopId && stopRoutesState.key === selectedStopId
      ? stopRoutesState.error
      : null) ??
    (effectiveRouteId && shapesState.key === effectiveRouteId
      ? shapesState.error
      : null) ??
    (routeStopsKey && routeStopsState.key === routeStopsKey
      ? routeStopsState.error
      : null) ??
    (departuresKey && departuresState.key === departuresKey
      ? departuresState.error
      : null) ??
    (selectedTrip && tripStopsState.key === selectedTrip.tripId
      ? tripStopsState.error
      : null)

  const visibleShapes = useMemo(() => {
    if (effectiveDirection == null) {
      return []
    }
    return allShapes.filter(
      (shape) => shape.directionId === effectiveDirection
    )
  }, [allShapes, effectiveDirection])

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
    if (selectedTrip && tripStops.length > 0) {
      return tripStops.map((stop) => ({
        id: stop.stopId,
        name: stop.stopName,
        lat: stop.lat,
        lon: stop.lon,
      }))
    }
    if (selectedRouteId && activeDirection != null && routeStops.length > 0) {
      return routeStops
    }
    return stops
  }, [activeDirection, routeStops, selectedRouteId, selectedTrip, stops, tripStops])

  useEffect(() => {
    if (!selectedStopId) {
      return
    }
    const controller = new AbortController()
    fetchRoutesForStop(selectedStopId, controller.signal)
      .then((items) => {
        setStopRoutesState({ key: selectedStopId, items, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setStopRoutesState({
          key: selectedStopId,
          items: [],
          error:
            err instanceof Error ? err.message : "Błąd pobierania linii",
        })
      })
    return () => controller.abort()
  }, [selectedStopId])

  useEffect(() => {
    if (!effectiveRouteId) {
      return
    }
    const controller = new AbortController()
    fetchRouteShapes(effectiveRouteId, controller.signal)
      .then((items) => {
        setShapesState({ key: effectiveRouteId, items, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setShapesState({
          key: effectiveRouteId,
          items: [],
          error:
            err instanceof Error ? err.message : "Błąd pobierania przebiegu",
        })
      })
    return () => controller.abort()
  }, [effectiveRouteId])

  useEffect(() => {
    if (!selectedRouteId || activeDirection == null || !routeStopsKey) {
      return
    }
    const controller = new AbortController()
    fetchStopsForRouteDirection(
      selectedRouteId,
      activeDirection,
      controller.signal
    )
      .then((items) => {
        setRouteStopsState({ key: routeStopsKey, items, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setRouteStopsState({
          key: routeStopsKey,
          items: [],
          error:
            err instanceof Error ? err.message : "Błąd przystanków linii",
        })
      })
    return () => controller.abort()
  }, [activeDirection, routeStopsKey, selectedRouteId])

  useEffect(() => {
    if (!selectedStopId || !departuresKey) {
      return
    }
    const controller = new AbortController()
    fetchDepartures(
      selectedStopId,
      selectedRouteId
        ? {
            routeId: selectedRouteId,
            directionId: activeDirection,
            limit: 12,
          }
        : { limit: 20 },
      controller.signal
    )
      .then((items) => {
        setDeparturesState({ key: departuresKey, items, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setDeparturesState({
          key: departuresKey,
          items: [],
          error:
            err instanceof Error ? err.message : "Błąd pobierania odjazdów",
        })
      })
    return () => controller.abort()
  }, [activeDirection, departuresKey, selectedRouteId, selectedStopId])

  useEffect(() => {
    if (!selectedTrip) {
      return
    }
    const tripId = selectedTrip.tripId
    const controller = new AbortController()
    fetchTripStops(tripId, controller.signal)
      .then((items) => {
        setTripStopsState({ key: tripId, items, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setTripStopsState({
          key: tripId,
          items: [],
          error:
            err instanceof Error
              ? err.message
              : "Błąd pobierania przystanków przejazdu",
        })
      })
    return () => controller.abort()
  }, [selectedTrip])

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = query.trim()
    search(trimmed)
    const match = stops.find((stop) =>
      normalizeQuery(stop.name).includes(normalizeQuery(trimmed))
    )
    if (match) {
      setSelectedTrip(null)
      setSelectedStopId(match.id)
    }
  }

  function selectRoute(routeId: string) {
    setSelectedTrip(null)
    setSelectedRouteId(routeId)
    if (!routeId) {
      setDirectionId(null)
      return
    }
    const fromStop = stopRoutes.find((route) => route.id === routeId)
    setDirectionId(fromStop?.directions[0]?.directionId ?? null)
  }

  function toggleRoute(routeId: string) {
    if (selectedRouteId === routeId) {
      selectRoute("")
      return
    }
    selectRoute(routeId)
  }

  function onPickStop(stopId: string) {
    setSelectedTrip(null)
    setSelectedStopId((current) => (current === stopId ? null : stopId))
  }

  function onDeselectStop() {
    setSelectedTrip(null)
    setSelectedStopId(null)
  }

  function onSelectDirection(nextDirectionId: number) {
    setSelectedTrip(null)
    setDirectionId(nextDirectionId)
  }

  return (
    <div className="lg:grid lg:h-[calc(100svh-6.75rem)] lg:grid-cols-12 lg:overflow-hidden">
      <section className="flex flex-col border-b lg:col-span-4 lg:h-full lg:min-h-0 lg:overflow-y-auto lg:border-r lg:border-b-0">
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
            <div className="flex items-center justify-between gap-2">
              <Label htmlFor="route-pick">Linia</Label>
              {selectedRouteId ? (
                <button
                  type="button"
                  className="text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                  onClick={() => selectRoute("")}
                >
                  Wszystkie linie
                </button>
              ) : null}
            </div>
            <NativeSelect
              id="route-pick"
              value={selectedRouteId}
              onChange={(event) => selectRoute(event.target.value)}
            >
              <option value="">Wszystkie linie</option>
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
          <ul className="max-h-40 shrink-0 overflow-y-auto border-b">
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

        <div className="space-y-5 px-4 py-4 lg:px-5">
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
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="text-base font-medium">{selectedStop.name}</h2>
                  <p className="mt-1 text-sm text-muted-foreground tabular-nums">
                    {selectedStop.lat.toFixed(5)} N,{" "}
                    {selectedStop.lon.toFixed(5)} E
                  </p>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="shrink-0"
                  onClick={onDeselectStop}
                >
                  Odznacz
                </Button>
              </div>
              <Button
                type="button"
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

          {!selectedTrip && selectedRouteId && directions.length > 0 ? (
            <div>
              <h3 className="text-sm font-medium">Kierunek</h3>
              <div className="mt-2 flex flex-col gap-1">
                {directions.map((direction) => (
                  <Button
                    key={direction.directionId}
                    type="button"
                    variant={
                      activeDirection === direction.directionId
                        ? "default"
                        : "outline"
                    }
                    className="h-9 justify-start"
                    onClick={() => onSelectDirection(direction.directionId)}
                  >
                    {direction.headsign}
                  </Button>
                ))}
              </div>
            </div>
          ) : null}

          {selectedStop && selectedTrip ? (
            <div>
              <div className="flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-center gap-2.5">
                  <span
                    className={cn(
                      "inline-grid min-w-8 shrink-0 place-items-center px-1.5 py-0.5 text-xs font-medium tabular-nums",
                      selectedTrip.vehicleType === "TRAM"
                        ? "bg-tram text-primary-foreground"
                        : "bg-bus text-bus-foreground"
                    )}
                  >
                    {selectedTrip.routeShortName}
                  </span>
                  <div className="min-w-0">
                    <h3 className="truncate text-sm font-medium">
                      {selectedTrip.headsign || "Przebieg przejazdu"}
                    </h3>
                    <p className="text-xs text-muted-foreground tabular-nums">
                      Odjazd {selectedTrip.departureTime} (
                      {formatMinutesUntil(selectedTrip.minutesUntil)})
                    </p>
                  </div>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="shrink-0"
                  onClick={() => setSelectedTrip(null)}
                >
                  Wróć
                </Button>
              </div>

              {tripStopsLoading ? (
                <p className="mt-3 text-sm text-muted-foreground">
                  Pobieranie przystanków przejazdu…
                </p>
              ) : tripStops.length === 0 ? (
                <p className="mt-3 text-sm text-muted-foreground">
                  Brak danych o przystankach dla tego przejazdu.
                </p>
              ) : (
                <ol className="mt-3 divide-y border-y">
                  {tripStops.map((stop, index) => {
                    const isCurrent = stop.stopId === selectedStopId
                    const isPast = stop.minutesUntil < 0
                    return (
                      <li
                        key={`${stop.stopSequence}-${stop.stopId}`}
                        className={cn(
                          "flex items-center justify-between gap-3 px-2 py-2 text-sm",
                          isCurrent && "bg-muted",
                          isPast && !isCurrent && "text-muted-foreground"
                        )}
                      >
                        <div className="flex min-w-0 items-center gap-2.5">
                          <span className="w-5 shrink-0 text-right text-xs text-muted-foreground tabular-nums">
                            {index + 1}.
                          </span>
                          <div className="min-w-0">
                            <p
                              className={cn(
                                "truncate",
                                isCurrent ? "font-semibold" : "font-medium"
                              )}
                            >
                              {stop.stopName}
                            </p>
                            <p className="text-xs text-muted-foreground tabular-nums">
                              {stop.arrivalTime}
                              {isCurrent ? " · wybrany przystanek" : ""}
                            </p>
                          </div>
                        </div>
                        <p
                          className={cn(
                            "shrink-0 tabular-nums",
                            isCurrent
                              ? "font-semibold text-primary"
                              : isPast
                                ? "text-xs text-muted-foreground"
                                : "font-medium"
                          )}
                        >
                          {formatMinutesUntil(stop.minutesUntil)}
                        </p>
                      </li>
                    )
                  })}
                </ol>
              )}
            </div>
          ) : selectedStop ? (
            <div>
              <div className="flex items-baseline justify-between gap-2">
                <h3 className="text-sm font-medium">
                  {selectedRoute
                    ? `Odjazdy · linia ${selectedRoute.shortName}`
                    : "Najbliższe odjazdy · wszystkie linie"}
                </h3>
                {selectedRoute ? (
                  <button
                    type="button"
                    className="text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                    onClick={() => selectRoute("")}
                  >
                    Wszystkie linie
                  </button>
                ) : null}
              </div>

              {departures.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">
                  {busy
                    ? "Pobieranie odjazdów…"
                    : selectedRoute
                      ? "Brak zaplanowanych odjazdów na dziś dla tego kierunku."
                      : "Brak zaplanowanych odjazdów na dziś z tego przystanku."}
                </p>
              ) : (
                <>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Kliknij odjazd, aby zobaczyć kolejne przystanki i czasy
                    przyjazdu.
                  </p>
                  <ul className="mt-2 divide-y border-y">
                    {departures.map((item) => {
                      const isTram = item.vehicleType === "TRAM"
                      return (
                        <li key={`${item.tripId}-${item.departureTime}`}>
                          <button
                            type="button"
                            className="flex w-full items-center justify-between gap-3 px-1.5 py-2 text-left text-sm transition-colors hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                            onClick={() => setSelectedTrip(item)}
                          >
                            <div className="flex min-w-0 items-center gap-2.5">
                              {!selectedRoute ? (
                                <span
                                  className={cn(
                                    "inline-grid min-w-8 shrink-0 place-items-center px-1.5 py-0.5 text-xs font-medium tabular-nums",
                                    isTram
                                      ? "bg-tram text-primary-foreground"
                                      : "bg-bus text-bus-foreground"
                                  )}
                                >
                                  {item.routeShortName}
                                </span>
                              ) : null}
                              <div className="min-w-0">
                                <p className="truncate font-medium">
                                  {item.headsign || "Bez kierunku"}
                                </p>
                                <p className="text-xs text-muted-foreground tabular-nums">
                                  {item.departureTime}
                                </p>
                              </div>
                            </div>
                            <p className="shrink-0 font-medium tabular-nums">
                              {formatMinutesUntil(item.minutesUntil)}
                            </p>
                          </button>
                        </li>
                      )
                    })}
                  </ul>
                </>
              )}
            </div>
          ) : null}

          {!selectedTrip && selectedStop && stopRoutes.length > 0 ? (
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
                      onClick={() => toggleRoute(route.id)}
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
        </div>
      </section>

      <section className="relative lg:col-span-8 lg:h-full">
        <div className="h-[28rem] lg:absolute lg:inset-0 lg:h-auto">
          <TransitMap
            stops={mapStops}
            highlightedStopId={selectedStopId}
            onSelectStop={onPickStop}
            onDeselectStop={onDeselectStop}
            shapes={visibleShapes}
            route={effectiveRoute}
          />
        </div>
      </section>
    </div>
  )
}
