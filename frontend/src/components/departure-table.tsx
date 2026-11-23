import { upcomingDepartures, vehicleLabel } from "@/data/network"
import { formatRemaining, formatTime } from "@/lib/format"
import { useRoutes } from "@/state/routes"

import { LineBadge } from "@/components/line-badge"

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
  const { routeById } = useRoutes()
  const rows = upcomingDepartures(stopId, now, limit)

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
            const route = routeById(row.routeId)
            return (
              <tr
                key={`${row.routeId}-${row.headsign}-${row.at.getTime()}`}
                className="border-b last:border-b-0 hover:bg-muted"
              >
                <td className="px-4 py-2.5 font-medium tabular-nums">
                  {formatTime(row.at)}
                </td>
                <td className="px-2 py-2.5">
                  <LineBadge routeId={row.routeId} />
                </td>
                <td className="px-2 py-2.5 text-muted-foreground">
                  {route ? vehicleLabel(route.vehicleType) : "—"}
                </td>
                <td className="px-2 py-2.5">{row.headsign}</td>
                <td className="px-4 py-2.5 text-right text-muted-foreground tabular-nums">
                  {formatRemaining(row.at.getTime() - now.getTime())}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
