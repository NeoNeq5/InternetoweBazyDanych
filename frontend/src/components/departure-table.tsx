import { useEffect, useState } from "react"
import { cn } from "cn"

import { fetchDepartures, type DepartureDto } from "@/api/network"
import { formatRemaining, vehicleLabel } from "@/lib/format"

type FetchState = {
  key: string
  rows: DepartureDto[]
  error: string | null
}

export function DepartureTable({
  stopId,
  stopName,
  now,
  limit = 8,
}: {
  stopId: string
  stopName: string
  now: Date
  limit?: number
}) {
  const minuteBucket = Math.floor(now.getTime() / 60_000)
  const requestKey = `${stopId}:${limit}:${minuteBucket}`
  const [state, setState] = useState<FetchState>({
    key: "",
    rows: [],
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()
    fetchDepartures(stopId, { limit }, controller.signal)
      .then((rows) => {
        setState({ key: requestKey, rows, error: null })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setState({
          key: requestKey,
          rows: [],
          error:
            err instanceof Error ? err.message : "Nie udało się pobrać odjazdów",
        })
      })
    return () => controller.abort()
  }, [limit, requestKey, stopId])

  const loading = state.key !== requestKey && state.rows.length === 0
  const rows = state.key.startsWith(`${stopId}:${limit}:`) ? state.rows : []
  const error = state.key === requestKey ? state.error : null

  if (error) {
    return (
      <p className="px-4 py-4 text-sm text-destructive lg:px-6">{error}</p>
    )
  }

  if (loading) {
    return (
      <p className="px-4 py-4 text-sm text-muted-foreground lg:px-6">
        Pobieranie odjazdów…
      </p>
    )
  }

  if (rows.length === 0) {
    return (
      <p className="px-4 py-4 text-sm text-muted-foreground lg:px-6">
        Brak zaplanowanych odjazdów na dziś z tego przystanku.
      </p>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[40rem] border-collapse text-sm">
        <caption className="sr-only">Odjazdy z przystanku {stopName}</caption>
        <thead className="text-left text-xs text-muted-foreground">
          <tr className="border-b">
            <th className="px-4 py-2 font-medium">Godzina</th>
            <th className="px-2 py-2 font-medium">Linia</th>
            <th className="px-2 py-2 font-medium">Pojazd</th>
            <th className="px-2 py-2 font-medium">Kierunek</th>
            <th className="px-4 py-2 text-right font-medium">Za</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const isTram = row.vehicleType === "TRAM"
            return (
              <tr
                key={`${row.tripId}-${row.departureTime}`}
                className="border-b last:border-b-0 hover:bg-muted"
              >
                <td className="px-4 py-2.5 font-medium tabular-nums">
                  {row.departureTime}
                </td>
                <td className="px-2 py-2.5">
                  <span
                    className={cn(
                      "inline-grid min-w-8 place-items-center px-1.5 py-0.5 text-xs font-medium tabular-nums",
                      isTram
                        ? "bg-tram text-primary-foreground"
                        : "bg-bus text-bus-foreground"
                    )}
                  >
                    <span className="sr-only">
                      {vehicleLabel(row.vehicleType)}{" "}
                    </span>
                    {row.routeShortName}
                  </span>
                </td>
                <td className="px-2 py-2.5 text-muted-foreground">
                  {vehicleLabel(row.vehicleType)}
                </td>
                <td className="px-2 py-2.5">
                  {row.headsign || "Bez kierunku"}
                </td>
                <td className="px-4 py-2.5 text-right text-muted-foreground tabular-nums">
                  {row.minutesUntil <= 0
                    ? "teraz"
                    : formatRemaining(row.minutesUntil * 60_000)}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
