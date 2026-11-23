import { useEffect } from "react"
import {
  CircleMarker,
  MapContainer,
  Polyline,
  Popup,
  TileLayer,
  useMap,
} from "react-leaflet"
import type { LatLngExpression } from "leaflet"

import type { RouteShapeDto } from "@/api/shapes"
import type { StopDto } from "@/api/stops"
import type { RouteDto } from "@/api/routes"

import "leaflet/dist/leaflet.css"

const WROCLAW_CENTER: LatLngExpression = [51.1079, 17.0385]

function FitView({
  stops,
  shapes,
}: {
  stops: StopDto[]
  shapes: RouteShapeDto[]
}) {
  const map = useMap()

  useEffect(() => {
    const points: LatLngExpression[] = []
    for (const shape of shapes) {
      for (const [lat, lon] of shape.points) {
        points.push([lat, lon])
      }
    }
    for (const stop of stops) {
      points.push([stop.lat, stop.lon])
    }
    if (points.length === 0) {
      map.setView(WROCLAW_CENTER, 12)
      return
    }
    if (points.length === 1) {
      map.setView(points[0], 15)
      return
    }
    map.fitBounds(points as [number, number][], { padding: [36, 36], maxZoom: 15 })
  }, [map, shapes, stops])

  return null
}

export function TransitMap({
  stops,
  highlightedStopId,
  onSelectStop,
  shapes,
  route,
}: {
  stops: StopDto[]
  highlightedStopId: string | null
  onSelectStop: (stopId: string) => void
  shapes: RouteShapeDto[]
  route: RouteDto | null
}) {
  const lineColor =
    route?.vehicleType === "BUS"
      ? "oklch(0.62 0.12 75)"
      : "oklch(0.42 0.12 255)"

  const fitStops =
    highlightedStopId != null
      ? stops.filter((stop) => stop.id === highlightedStopId)
      : []

  return (
    <MapContainer
      center={WROCLAW_CENTER}
      zoom={12}
      className="h-full min-h-[28rem] w-full"
      scrollWheelZoom
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FitView stops={fitStops} shapes={shapes} />
      {shapes.map((shape) => (
        <Polyline
          key={shape.directionId}
          positions={shape.points}
          pathOptions={{
            color: lineColor,
            weight: 5,
            opacity: 0.9,
          }}
        />
      ))}
      {stops.map((stop) => {
        const active = stop.id === highlightedStopId
        return (
          <CircleMarker
            key={stop.id}
            center={[stop.lat, stop.lon]}
            radius={active ? 8 : 4}
            pathOptions={{
              color: active ? "oklch(0.22 0.03 255)" : "oklch(0.45 0.02 255)",
              fillColor: active ? "oklch(0.42 0.12 255)" : "oklch(0.97 0.01 95)",
              fillOpacity: 1,
              weight: active ? 2 : 1,
            }}
            eventHandlers={{
              click: () => onSelectStop(stop.id),
            }}
          >
            <Popup>
              <p className="m-0 text-sm font-medium">{stop.name}</p>
              <p className="m-0 text-xs text-muted-foreground">
                {stop.lat.toFixed(5)}, {stop.lon.toFixed(5)}
              </p>
            </Popup>
          </CircleMarker>
        )
      })}
    </MapContainer>
  )
}
