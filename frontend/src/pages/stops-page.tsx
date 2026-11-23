import { useEffect, useMemo, useState, type FormEvent } from "react"

import { TransitMap } from "@/components/transit-map"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { NativeSelect } from "@/components/ui/input"
import { fetchRouteShapes, type RouteShapeDto } from "@/api/shapes"
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
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [routeId, setRouteId] = useState("")
  const [shapes, setShapes] = useState<RouteShapeDto[]>([])
  const [shapeError, setShapeError] = useState<string | null>(null)
  const [shapeLoading, setShapeLoading] = useState(false)

  const selected = selectedId ? stopById(selectedId) : undefined
  const selectedRoute = routes.find((route) => route.id === routeId) ?? null
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
    if (needle) {
      const byId = new Map(matchedStops.map((stop) => [stop.id, stop]))
      if (selected) {
        byId.set(selected.id, selected)
      }
      for (const id of favoriteStopIds) {
        const stop = stopById(id)
        if (stop) {
          byId.set(stop.id, stop)
        }
      }
      return [...byId.values()]
    }
    return stops
  }, [favoriteStopIds, matchedStops, needle, selected, stopById, stops])

  useEffect(() => {
    if (!routeId) {
      setShapes([])
      setShapeError(null)
      return
    }

    const controller = new AbortController()
    setShapeLoading(true)
    setShapeError(null)

    fetchRouteShapes(routeId, controller.signal)
      .then((data) => {
        setShapes(data)
        setShapeLoading(false)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setShapes([])
        setShapeLoading(false)
        setShapeError(
          err instanceof Error ? err.message : "Błąd pobierania przebiegu"
        )
      })

    return () => controller.abort()
  }, [routeId])

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = query.trim()
    search(trimmed)
    const match = stops.find((stop) =>
      normalizeQuery(stop.name).includes(normalizeQuery(trimmed))
    )
    if (match) {
      setSelectedId(match.id)
    }
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
              Wyszukaj przystanek albo wybierz linię, żeby narysować przebieg.
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
              value={routeId}
              onChange={(event) => setRouteId(event.target.value)}
            >
              <option value="">Bez przebiegu</option>
              {routes.map((route) => (
                <option key={route.id} value={route.id}>
                  {route.shortName} · {route.longName}
                </option>
              ))}
            </NativeSelect>
          </div>

          <Button type="submit" className="h-10">
            Szukaj na mapie
          </Button>

          {loading ? (
            <p className="text-sm text-muted-foreground">Ładowanie przystanków…</p>
          ) : null}
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          {shapeLoading ? (
            <p className="text-sm text-muted-foreground">Ładowanie przebiegu…</p>
          ) : null}
          {shapeError ? (
            <p className="text-sm text-destructive">{shapeError}</p>
          ) : null}
        </form>

        {matchedStops.length > 0 ? (
          <ul className="max-h-48 overflow-y-auto border-b">
            {matchedStops.map((stop) => (
              <li key={stop.id}>
                <button
                  type="button"
                  className={cn(
                    "flex w-full items-baseline justify-between gap-3 px-4 py-2.5 text-left text-sm hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none lg:px-5",
                    selectedId === stop.id && "bg-muted"
                  )}
                  onClick={() => setSelectedId(stop.id)}
                >
                  <span className="font-medium">{stop.name}</span>
                  <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
                    {stop.lat.toFixed(3)}, {stop.lon.toFixed(3)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}

        <div className="px-4 py-4 lg:px-5">
          {selected ? (
            <>
              <h2 className="text-base font-medium">{selected.name}</h2>
              <p className="mt-1 text-sm text-muted-foreground tabular-nums">
                {selected.lat.toFixed(5)} N, {selected.lon.toFixed(5)} E
              </p>
              <Button
                variant="outline"
                className="mt-3 h-9"
                onClick={() => toggleFavorite(selected.id)}
              >
                {favoriteStopIds.includes(selected.id)
                  ? "Usuń z ulubionych"
                  : "Dodaj do ulubionych"}
              </Button>
            </>
          ) : (
            <p className="text-sm text-muted-foreground">
              Kliknij punkt na mapie albo wyszukaj nazwę przystanku.
            </p>
          )}

          {favoriteStopIds.length > 0 ? (
            <div className="mt-6">
              <h3 className="text-sm font-medium">Ulubione</h3>
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
                        className="py-1 text-sm hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                        onClick={() => setSelectedId(id)}
                      >
                        {stop.name}
                      </button>
                    </li>
                  )
                })}
              </ul>
            </div>
          ) : null}

          {selectedRoute ? (
            <p className="mt-6 text-sm text-muted-foreground">
              Linia {selectedRoute.shortName}: {selectedRoute.longName}. Na mapie
              widać {shapes.length}{" "}
              {shapes.length === 1 ? "kierunek" : "kierunki"}.
            </p>
          ) : null}
        </div>
      </section>

      <section className="relative lg:col-span-8">
        <div className="h-[28rem] lg:absolute lg:inset-0 lg:h-auto">
          <TransitMap
            stops={mapStops}
            highlightedStopId={selectedId}
            onSelectStop={setSelectedId}
            shapes={shapes}
            route={selectedRoute}
          />
        </div>
      </section>
    </div>
  )
}
