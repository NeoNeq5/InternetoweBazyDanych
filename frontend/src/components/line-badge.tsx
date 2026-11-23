import { cn } from "cn"

import { vehicleLabel } from "@/data/network"
import { useRoutes } from "@/state/routes"

export function LineBadge({ routeId }: { routeId: string }) {
  const { routeById } = useRoutes()
  const route = routeById(routeId)
  if (!route) {
    return <span className="tabular-nums">{routeId}</span>
  }

  const tram = route.vehicleType === "TRAM"

  return (
    <span
      className={cn(
        "inline-grid min-w-8 place-items-center px-1.5 py-0.5 text-xs font-medium tabular-nums",
        tram ? "bg-tram text-primary-foreground" : "bg-bus text-bus-foreground"
      )}
    >
      <span className="sr-only">{vehicleLabel(route.vehicleType)} </span>
      {route.shortName}
    </span>
  )
}
