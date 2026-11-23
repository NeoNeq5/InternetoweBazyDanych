export type VehicleType = "TRAM" | "BUS"

export type Stop = {
  id: string
  name: string
  lat: number
  lon: number
}

type Service = {
  stopId: string
  routeId: string
  headsign: string
  headwayMinutes: number
  offsetMinutes: number
}

export type Departure = {
  stopId: string
  routeId: string
  headsign: string
  at: Date
}

export const stops: Stop[] = [
  {
    id: "plac-grunwaldzki",
    name: "Plac Grunwaldzki",
    lat: 51.1097,
    lon: 17.0606,
  },
  { id: "dworzec-glowny", name: "Dworzec Główny", lat: 51.0989, lon: 17.0365 },
  {
    id: "galeria-dominikanska",
    name: "Galeria Dominikańska",
    lat: 51.1084,
    lon: 17.0396,
  },
  { id: "hala-stulecia", name: "Hala Stulecia", lat: 51.1069, lon: 17.0773 },
  { id: "kromera", name: "Kromera", lat: 51.1286, lon: 17.0454 },
  { id: "renoma", name: "Renoma", lat: 51.1034, lon: 17.0308 },
  { id: "jan-pawel", name: "pl. Jana Pawła II", lat: 51.108, lon: 17.032 },
  { id: "biskupin", name: "Biskupin", lat: 51.1015, lon: 17.0955 },
  {
    id: "most-grunwaldzki",
    name: "Most Grunwaldzki",
    lat: 51.1092,
    lon: 17.0534,
  },
  { id: "fat", name: "FAT", lat: 51.0935, lon: 16.9788 },
]

const services: Service[] = [
  service("plac-grunwaldzki", "4", "Biskupin", 8, 1),
  service("plac-grunwaldzki", "4", "Grabiszynek", 8, 5),
  service("plac-grunwaldzki", "8", "Karłowice", 10, 2),
  service("plac-grunwaldzki", "8", "Tarnogaj", 10, 7),
  service("plac-grunwaldzki", "10", "Leśnica", 12, 3),
  service("plac-grunwaldzki", "33", "Sępolno", 10, 0),
  service("plac-grunwaldzki", "A", "Gaj", 12, 4),
  service("plac-grunwaldzki", "145", "Zakrzów", 20, 6),
  service("dworzec-glowny", "4", "Biskupin", 8, 3),
  service("dworzec-glowny", "8", "Tarnogaj", 10, 4),
  service("dworzec-glowny", "17", "Klecina", 10, 1),
  service("dworzec-glowny", "K", "Kozanów", 15, 8),
  service("dworzec-glowny", "145", "Nowy Dwór", 20, 11),
  service("galeria-dominikanska", "8", "Karłowice", 10, 0),
  service("galeria-dominikanska", "17", "Sępolno", 10, 5),
  service("galeria-dominikanska", "33", "Pilczyce", 10, 2),
  service("galeria-dominikanska", "A", "Osobowice", 12, 6),
  service("hala-stulecia", "4", "Biskupin", 8, 6),
  service("hala-stulecia", "10", "Biskupin", 12, 9),
  service("hala-stulecia", "33", "Sępolno", 10, 4),
  service("kromera", "8", "Karłowice", 10, 8),
  service("kromera", "10", "Leśnica", 12, 1),
  service("kromera", "K", "Księże Małe", 15, 3),
  service("kromera", "145", "Zakrzów", 20, 14),
  service("renoma", "4", "Grabiszynek", 8, 2),
  service("renoma", "17", "Klecina", 10, 6),
  service("renoma", "A", "Gaj", 12, 9),
  service("jan-pawel", "17", "Sępolno", 10, 0),
  service("jan-pawel", "17", "Klecina", 10, 5),
  service("jan-pawel", "33", "Pilczyce", 10, 7),
  service("jan-pawel", "K", "Kozanów", 15, 2),
  service("biskupin", "4", "Grabiszynek", 8, 0),
  service("biskupin", "10", "Leśnica", 12, 4),
  service("biskupin", "33", "Pilczyce", 10, 8),
  service("most-grunwaldzki", "4", "Biskupin", 8, 4),
  service("most-grunwaldzki", "8", "Tarnogaj", 10, 1),
  service("most-grunwaldzki", "10", "Biskupin", 12, 6),
  service("fat", "4", "Biskupin", 10, 2),
  service("fat", "17", "Sępolno", 12, 7),
  service("fat", "A", "Osobowice", 15, 5),
]

function service(
  stopId: string,
  routeId: string,
  headsign: string,
  headwayMinutes: number,
  offsetMinutes: number
): Service {
  return { stopId, routeId, headsign, headwayMinutes, offsetMinutes }
}

export function stopById(id: string) {
  return stops.find((stop) => stop.id === id)
}

export function normalizeQuery(value: string) {
  return value.toLocaleLowerCase("pl").normalize("NFD").replace(/\p{M}/gu, "")
}

export function upcomingDepartures(stopId: string, now: Date, limit = 8) {
  const minuteOfDay = now.getHours() * 60 + now.getMinutes()
  const second = now.getSeconds()
  const departures: Departure[] = []

  for (const item of services) {
    if (item.stopId !== stopId) {
      continue
    }

    const sinceOffset = minuteOfDay - item.offsetMinutes
    const mod =
      ((sinceOffset % item.headwayMinutes) + item.headwayMinutes) %
      item.headwayMinutes
    let waitMinutes =
      mod === 0 && second === 0
        ? 0
        : (item.headwayMinutes - mod) % item.headwayMinutes
    if (waitMinutes === 0 && second > 0) {
      waitMinutes = item.headwayMinutes
    }

    for (let index = 0; index < 2; index += 1) {
      const at = new Date(
        now.getTime() +
          (waitMinutes + index * item.headwayMinutes) * 60_000 -
          second * 1000 -
          now.getMilliseconds()
      )
      departures.push({
        stopId,
        routeId: item.routeId,
        headsign: item.headsign,
        at,
      })
    }
  }

  departures.sort((left, right) => left.at.getTime() - right.at.getTime())
  return departures.slice(0, limit)
}

export function vehicleLabel(vehicleType: VehicleType) {
  return vehicleType === "TRAM" ? "Tramwaj" : "Autobus"
}
