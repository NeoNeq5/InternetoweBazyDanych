import { useState, type FormEvent } from "react"

import { DepartureTable } from "@/components/departure-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { normalizeQuery, stops } from "@/data/network"
import { formatDateTime } from "@/lib/format"
import { useNow } from "@/lib/use-now"
import { cn } from "cn"
import { useAccount } from "@/state/account"

export function StopsPage() {
  const now = useNow()
  const { favoriteStopIds, searches, toggleFavorite, search } = useAccount()
  const [query, setQuery] = useState("")
  const [selectedId, setSelectedId] = useState(
    favoriteStopIds[0] ?? stops[0]?.id ?? ""
  )
  const selected = stops.find((stop) => stop.id === selectedId) ?? stops[0]
  const needle = normalizeQuery(query.trim())
  const visible = stops.filter(
    (stop) => !needle || normalizeQuery(stop.name).includes(needle)
  )

  function remember(value: string) {
    const trimmed = value.trim()
    setQuery(trimmed)
    const match = stops.find((stop) =>
      normalizeQuery(stop.name).includes(normalizeQuery(trimmed))
    )
    if (match) {
      setSelectedId(match.id)
    }
    search(trimmed)
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    remember(query)
  }

  return (
    <div className="lg:grid lg:min-h-[calc(100svh-6.75rem)] lg:grid-cols-12">
      <section className="border-b lg:col-span-4 lg:border-r lg:border-b-0">
        <form
          className="flex flex-col gap-3 px-4 py-4 lg:px-5"
          onSubmit={onSubmit}
        >
          <div>
            <h1 className="text-lg font-medium">Przystanki</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Ulubione zostają przy koncie. Wyszukiwanie trafia do historii.
            </p>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="stop-query">Nazwa</Label>
            <Input
              id="stop-query"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Hala Stulecia"
            />
          </div>
          <Button type="submit" className="h-10">
            Szukaj
          </Button>
        </form>

        {visible.length === 0 ? (
          <p className="border-t px-4 py-4 text-sm text-muted-foreground lg:px-5">
            Brak przystanku dla „{query.trim()}”.
          </p>
        ) : (
          <ul className="border-t">
            {visible.map((stop) => {
              const favorite = favoriteStopIds.includes(stop.id)
              const current = selected?.id === stop.id
              return (
                <li
                  key={stop.id}
                  className={cn(
                    "flex items-center justify-between gap-3 border-b px-4 py-2.5 lg:px-5",
                    current && "bg-muted"
                  )}
                >
                  <button
                    type="button"
                    className="text-left text-sm font-medium hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                    onClick={() => setSelectedId(stop.id)}
                  >
                    {stop.name}
                  </button>
                  <button
                    type="button"
                    className="shrink-0 text-xs text-muted-foreground hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                    onClick={() => toggleFavorite(stop.id)}
                  >
                    {favorite ? "Usuń z ulubionych" : "Dodaj do ulubionych"}
                  </button>
                </li>
              )
            })}
          </ul>
        )}

        <div className="px-4 py-4 lg:px-5">
          <h2 className="text-sm font-medium">Ostatnie wyszukiwania</h2>
          {searches.length === 0 ? (
            <p className="mt-2 text-sm text-muted-foreground">Pusto.</p>
          ) : (
            <ul className="mt-2">
              {searches.map((entry) => (
                <li
                  key={entry.id}
                  className="flex items-baseline justify-between gap-3 py-1.5"
                >
                  <button
                    type="button"
                    className="text-sm hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                    onClick={() => remember(entry.query)}
                  >
                    {entry.query}
                  </button>
                  <span className="text-xs text-muted-foreground">
                    {formatDateTime(entry.searchedAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="lg:col-span-8">
        {selected ? (
          <>
            <div className="px-4 py-4 lg:px-6">
              <h2 className="text-lg font-medium">{selected.name}</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {favoriteStopIds.includes(selected.id)
                  ? "Ulubiony przystanek"
                  : "Poza ulubionymi"}
              </p>
            </div>
            <DepartureTable
              stopId={selected.id}
              stopName={selected.name}
              now={now}
            />
          </>
        ) : null}
      </section>
    </div>
  )
}
