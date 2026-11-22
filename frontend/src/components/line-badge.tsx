import { routeById, vehicleLabel } from "@/data/network"
import { cn } from "cn"

export function LineBadge({ routeId }: { routeId: string }) {
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
